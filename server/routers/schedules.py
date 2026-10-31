import datetime
import uuid
from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from database import get_db
import models
import schemas

router = APIRouter(prefix="/api/schedules", tags=["Schedules & Timetable"])

@router.get("/{recorder_id}", response_model=List[schemas.ScheduleResponse])
def get_schedules_for_recorder(recorder_id: str, db: Session = Depends(get_db)):
    recorder = db.query(models.Device).filter(models.Device.id == recorder_id).first()
    if not recorder:
        raise HTTPException(status_code=404, detail="Recorder device not found")

    schedules = db.query(models.Schedule).filter(
        models.Schedule.recorder_id == recorder_id
    ).order_by(models.Schedule.start_time_str.asc()).all()
    return schedules

@router.post("/{recorder_id}", response_model=schemas.ScheduleResponse, status_code=status.HTTP_201_CREATED)
def create_schedule(recorder_id: str, schedule_in: schemas.ScheduleCreate, db: Session = Depends(get_db)):
    recorder = db.query(models.Device).filter(models.Device.id == recorder_id).first()
    if not recorder:
        raise HTTPException(status_code=404, detail="Recorder device not found")

    new_schedule = models.Schedule(
        id=str(uuid.uuid4()),
        recorder_id=recorder_id,
        label=schedule_in.label,
        start_time_str=schedule_in.start_time_str,
        stop_time_str=schedule_in.stop_time_str,
        days_of_week=schedule_in.days_of_week,
        is_active=schedule_in.is_active,
        version=1,
        updated_at=datetime.datetime.now(datetime.timezone.utc)
    )
    db.add(new_schedule)

    # Log sync event
    sync_event = models.SyncEvent(
        device_id=recorder_id,
        event_type="SCHEDULE_CREATED",
        details=f"Schedule '{schedule_in.label}' ({schedule_in.start_time_str}-{schedule_in.stop_time_str}) created"
    )
    db.add(sync_event)

    db.commit()
    db.refresh(new_schedule)
    return new_schedule

@router.put("/{recorder_id}/{schedule_id}", response_model=schemas.ScheduleResponse)
def update_schedule(recorder_id: str, schedule_id: str, schedule_in: schemas.ScheduleUpdate, db: Session = Depends(get_db)):
    schedule = db.query(models.Schedule).filter(
        models.Schedule.id == schedule_id,
        models.Schedule.recorder_id == recorder_id
    ).first()

    if not schedule:
        raise HTTPException(status_code=404, detail="Schedule not found")

    if schedule_in.label is not None:
        schedule.label = schedule_in.label
    if schedule_in.start_time_str is not None:
        schedule.start_time_str = schedule_in.start_time_str
    if schedule_in.stop_time_str is not None:
        schedule.stop_time_str = schedule_in.stop_time_str
    if schedule_in.days_of_week is not None:
        schedule.days_of_week = schedule_in.days_of_week
    if schedule_in.is_active is not None:
        schedule.is_active = schedule_in.is_active

    schedule.version += 1
    schedule.updated_at = datetime.datetime.now(datetime.timezone.utc)

    # Log sync event
    sync_event = models.SyncEvent(
        device_id=recorder_id,
        event_type="SCHEDULE_UPDATED",
        details=f"Schedule {schedule_id} updated to version {schedule.version}"
    )
    db.add(sync_event)

    db.commit()
    db.refresh(schedule)
    return schedule

@router.delete("/{recorder_id}/{schedule_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_schedule(recorder_id: str, schedule_id: str, db: Session = Depends(get_db)):
    schedule = db.query(models.Schedule).filter(
        models.Schedule.id == schedule_id,
        models.Schedule.recorder_id == recorder_id
    ).first()

    if not schedule:
        raise HTTPException(status_code=404, detail="Schedule not found")

    db.delete(schedule)

    sync_event = models.SyncEvent(
        device_id=recorder_id,
        event_type="SCHEDULE_DELETED",
        details=f"Schedule {schedule_id} deleted"
    )
    db.add(sync_event)

    db.commit()
    return None
