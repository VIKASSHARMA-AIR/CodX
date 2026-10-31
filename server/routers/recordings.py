import os
import hashlib
import datetime
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, UploadFile, File, Form, Header, Request, status
from fastapi.responses import FileResponse, StreamingResponse
from sqlalchemy.orm import Session
from database import get_db
import models
import schemas

router = APIRouter(prefix="/api/recordings", tags=["Recordings & Audio"])

UPLOAD_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "uploads")
os.makedirs(UPLOAD_DIR, exist_ok=True)

@router.post("/upload", response_model=schemas.RecordingResponse)
def upload_recording(
    id: str = Form(...), # Phone A generated UUID
    session_id: str = Form(...),
    recorder_id: str = Form(...),
    duration_ms: int = Form(...),
    checksum_sha256: str = Form(...),
    file: UploadFile = File(...),
    db: Session = Depends(get_db)
):
    # Verify recorder exists
    recorder = db.query(models.Device).filter(models.Device.id == recorder_id).first()
    if not recorder:
        raise HTTPException(status_code=404, detail="Recorder device not found")

    # Ensure session exists or create fallback session
    session = db.query(models.RecordingSession).filter(models.RecordingSession.id == session_id).first()
    if not session:
        session = models.RecordingSession(
            id=session_id,
            recorder_id=recorder_id,
            started_at=datetime.datetime.now(datetime.timezone.utc) - datetime.timedelta(milliseconds=duration_ms),
            ended_at=datetime.datetime.now(datetime.timezone.utc),
            status="COMPLETED"
        )
        db.add(session)
        db.commit()

    # Deduplication check: if recording with this ID already uploaded
    existing = db.query(models.Recording).filter(models.Recording.id == id).first()
    if existing:
        return schemas.RecordingResponse(
            id=existing.id,
            session_id=existing.session_id,
            recorder_id=existing.recorder_id,
            filename=existing.filename,
            duration_ms=existing.duration_ms,
            file_size_bytes=existing.file_size_bytes,
            checksum_sha256=existing.checksum_sha256,
            uploaded_at=existing.uploaded_at,
            download_url=f"/api/recordings/{existing.id}/download",
            stream_url=f"/api/recordings/{existing.id}/stream"
        )

    # Read content synchronously from underlying SpooledTemporaryFile / BytesIO
    content = file.file.read()
    actual_sha256 = hashlib.sha256(content).hexdigest()

    if checksum_sha256.lower() != actual_sha256.lower():
        raise HTTPException(
            status_code=400,
            detail=f"Checksum mismatch! Expected {checksum_sha256}, got {actual_sha256}"
        )

    # Save to disk
    safe_filename = f"{id}_{file.filename}"
    file_path = os.path.join(UPLOAD_DIR, safe_filename)
    with open(file_path, "wb") as f:
        f.write(content)

    new_recording = models.Recording(
        id=id,
        session_id=session_id,
        recorder_id=recorder_id,
        filename=file.filename or "recording.m4a",
        duration_ms=duration_ms,
        file_size_bytes=len(content),
        checksum_sha256=actual_sha256,
        storage_path=file_path,
        uploaded_at=datetime.datetime.now(datetime.timezone.utc)
    )
    db.add(new_recording)

    # Log sync event
    sync_event = models.SyncEvent(
        device_id=recorder_id,
        event_type="AUDIO_UPLOADED",
        details=f"Recording {id} ({len(content)} bytes, {duration_ms} ms) successfully uploaded"
    )
    db.add(sync_event)

    db.commit()
    db.refresh(new_recording)

    return schemas.RecordingResponse(
        id=new_recording.id,
        session_id=new_recording.session_id,
        recorder_id=new_recording.recorder_id,
        filename=new_recording.filename,
        duration_ms=new_recording.duration_ms,
        file_size_bytes=new_recording.file_size_bytes,
        checksum_sha256=new_recording.checksum_sha256,
        uploaded_at=new_recording.uploaded_at,
        download_url=f"/api/recordings/{new_recording.id}/download",
        stream_url=f"/api/recordings/{new_recording.id}/stream"
    )

@router.get("/{recorder_id}", response_model=List[schemas.RecordingResponse])
def list_recordings_for_recorder(recorder_id: str, db: Session = Depends(get_db)):
    recordings = db.query(models.Recording).filter(
        models.Recording.recorder_id == recorder_id
    ).order_by(models.Recording.uploaded_at.desc()).all()

    return [
        schemas.RecordingResponse(
            id=r.id,
            session_id=r.session_id,
            recorder_id=r.recorder_id,
            filename=r.filename,
            duration_ms=r.duration_ms,
            file_size_bytes=r.file_size_bytes,
            checksum_sha256=r.checksum_sha256,
            uploaded_at=r.uploaded_at,
            download_url=f"/api/recordings/{r.id}/download",
            stream_url=f"/api/recordings/{r.id}/stream"
        )
        for r in recordings
    ]

@router.get("/{recording_id}/download")
def download_recording(recording_id: str, db: Session = Depends(get_db)):
    recording = db.query(models.Recording).filter(models.Recording.id == recording_id).first()
    if not recording or not os.path.exists(recording.storage_path):
        raise HTTPException(status_code=404, detail="Recording audio file not found")

    return FileResponse(
        path=recording.storage_path,
        filename=recording.filename,
        media_type="audio/mp4"
    )

@router.get("/{recording_id}/stream")
def stream_recording(recording_id: str, request: Request, db: Session = Depends(get_db)):
    recording = db.query(models.Recording).filter(models.Recording.id == recording_id).first()
    if not recording or not os.path.exists(recording.storage_path):
        raise HTTPException(status_code=404, detail="Recording audio file not found")

    file_path = recording.storage_path
    file_size = os.path.getsize(file_path)

    range_header = request.headers.get("range")
    if range_header:
        byte1, byte2 = 0, None
        match = range_header.replace("bytes=", "").split("-")
        if match[0]:
            byte1 = int(match[0])
        if len(match) > 1 and match[1]:
            byte2 = int(match[1])

        length = file_size - byte1 if byte2 is None else byte2 - byte1 + 1
        
        def iter_range():
            with open(file_path, "rb") as f:
                f.seek(byte1)
                remaining = length
                chunk_size = 1024 * 64
                while remaining > 0:
                    read_bytes = f.read(min(chunk_size, remaining))
                    if not read_bytes:
                        break
                    remaining -= len(read_bytes)
                    yield read_bytes

        headers = {
            "Content-Range": f"bytes {byte1}-{byte2 or file_size - 1}/{file_size}",
            "Accept-Ranges": "bytes",
            "Content-Length": str(length),
            "Content-Type": "audio/mp4",
        }
        return StreamingResponse(iter_range(), status_code=206, headers=headers)

    return FileResponse(
        path=file_path,
        media_type="audio/mp4",
        headers={"Accept-Ranges": "bytes"}
    )
