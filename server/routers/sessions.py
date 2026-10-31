from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from database import get_db
import models
import schemas

router = APIRouter(prefix="/api", tags=["Sessions & Sync Events"])

@router.post("/sessions", response_model=schemas.SessionResponse, status_code=status.HTTP_201_CREATED)
def create_or_update_session(session_in: schemas.SessionCreate, db: Session = Depends(get_db)):
    recorder = db.query(models.Device).filter(models.Device.id == session_in.recorder_id).first()
    if not recorder:
        raise HTTPException(status_code=404, detail="Recorder device not found")

    existing = db.query(models.RecordingSession).filter(models.RecordingSession.id == session_in.id).first()
    if existing:
        existing.status = session_in.status
        db.commit()
        db.refresh(existing)
        return existing

    new_session = models.RecordingSession(
        id=session_in.id,
        recorder_id=session_in.recorder_id,
        schedule_id=session_in.schedule_id,
        started_at=session_in.started_at,
        status=session_in.status
    )
    db.add(new_session)
    db.commit()
    db.refresh(new_session)
    return new_session

@router.put("/sessions/{session_id}/end", response_model=schemas.SessionResponse)
def end_session(session_id: str, session_end: schemas.SessionEnd, db: Session = Depends(get_db)):
    session = db.query(models.RecordingSession).filter(models.RecordingSession.id == session_id).first()
    if not session:
        raise HTTPException(status_code=404, detail="Session not found")

    session.ended_at = session_end.ended_at
    session.status = session_end.status
    session.error_message = session_end.error_message

    db.commit()
    db.refresh(session)
    return session

@router.get("/sessions/{recorder_id}", response_model=List[schemas.SessionResponse])
def get_sessions(recorder_id: str, db: Session = Depends(get_db)):
    sessions = db.query(models.RecordingSession).filter(
        models.RecordingSession.recorder_id == recorder_id
    ).order_by(models.RecordingSession.started_at.desc()).all()
    return sessions

@router.post("/sync/events")
def post_sync_event(event_in: schemas.SyncEventCreate, db: Session = Depends(get_db)):
    event = models.SyncEvent(
        device_id=event_in.device_id,
        event_type=event_in.event_type,
        details=event_in.details
    )
    db.add(event)
    db.commit()
    return {"status": "ok", "event_id": event.id}
