import datetime
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from database import get_db
import models
import schemas

router = APIRouter(prefix="/api", tags=["Status & Monitoring"])

@router.post("/recorder/status", response_model=schemas.DeviceStatusResponse)
def update_recorder_status(status_in: schemas.RecorderStatusUpdate, db: Session = Depends(get_db)):
    recorder = db.query(models.Device).filter(models.Device.id == status_in.recorder_id).first()
    if not recorder:
        raise HTTPException(status_code=404, detail="Recorder device not found")

    recorder.last_seen = datetime.datetime.now(datetime.timezone.utc)

    device_status = db.query(models.DeviceStatus).filter(
        models.DeviceStatus.recorder_id == status_in.recorder_id
    ).first()

    if not device_status:
        device_status = models.DeviceStatus(recorder_id=status_in.recorder_id)
        db.add(device_status)

    device_status.battery_pct = status_in.battery_pct
    device_status.is_charging = status_in.is_charging
    device_status.storage_free_bytes = status_in.storage_free_bytes
    device_status.storage_total_bytes = status_in.storage_total_bytes
    device_status.network_type = status_in.network_type
    device_status.current_state = status_in.current_state
    device_status.updated_at = datetime.datetime.now(datetime.timezone.utc)

    db.commit()
    db.refresh(device_status)

    return schemas.DeviceStatusResponse(
        recorder_id=device_status.recorder_id,
        battery_pct=device_status.battery_pct,
        is_charging=device_status.is_charging,
        storage_free_bytes=device_status.storage_free_bytes,
        storage_total_bytes=device_status.storage_total_bytes,
        network_type=device_status.network_type,
        current_state=device_status.current_state,
        is_online=True,
        last_seen=recorder.last_seen
    )

@router.get("/controller/recorder-status/{recorder_id}", response_model=schemas.DeviceStatusResponse)
def get_recorder_status(recorder_id: str, db: Session = Depends(get_db)):
    recorder = db.query(models.Device).filter(models.Device.id == recorder_id).first()
    if not recorder:
        raise HTTPException(status_code=404, detail="Recorder device not found")

    device_status = db.query(models.DeviceStatus).filter(
        models.DeviceStatus.recorder_id == recorder_id
    ).first()

    if not device_status:
        # Default empty status
        return schemas.DeviceStatusResponse(
            recorder_id=recorder_id,
            battery_pct=100,
            is_charging=False,
            storage_free_bytes=0,
            storage_total_bytes=0,
            network_type="UNKNOWN",
            current_state="OFFLINE",
            is_online=False,
            last_seen=recorder.last_seen
        )

    last_seen = recorder.last_seen.replace(tzinfo=datetime.timezone.utc) if recorder.last_seen.tzinfo is None else recorder.last_seen
    diff_seconds = (datetime.datetime.now(datetime.timezone.utc) - last_seen).total_seconds()
    is_online = diff_seconds <= 60

    return schemas.DeviceStatusResponse(
        recorder_id=device_status.recorder_id,
        battery_pct=device_status.battery_pct,
        is_charging=device_status.is_charging,
        storage_free_bytes=device_status.storage_free_bytes,
        storage_total_bytes=device_status.storage_total_bytes,
        network_type=device_status.network_type,
        current_state=device_status.current_state if is_online else "OFFLINE",
        is_online=is_online,
        last_seen=recorder.last_seen
    )

@router.get("/time")
def get_server_authoritative_time():
    """
    Returns the cloud server's authoritative UTC timestamp and timezone.
    Used by Recorder and Controller apps to calculate device clock drift.
    """
    now = datetime.datetime.now(datetime.timezone.utc)
    return {
        "server_utc_iso": now.isoformat(),
        "server_timestamp_ms": int(now.timestamp() * 1000),
        "timezone": "UTC"
    }
