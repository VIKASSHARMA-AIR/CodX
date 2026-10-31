import datetime
import uuid
from sqlalchemy import Column, String, Integer, BigInteger, Boolean, DateTime, ForeignKey, Text
from sqlalchemy.orm import relationship
from database import Base

class User(Base):
    __tablename__ = "users"

    id = Column(String(64), primary_key=True, default=lambda: str(uuid.uuid4()))
    firebase_uid = Column(String(128), unique=True, index=True, nullable=False)
    email = Column(String(255), unique=True, index=True, nullable=False)
    display_name = Column(String(255), nullable=True)
    photo_url = Column(String(500), nullable=True)
    api_token = Column(String(128), unique=True, index=True, nullable=False)
    created_at = Column(DateTime, default=lambda: datetime.datetime.now(datetime.timezone.utc))
    last_login = Column(DateTime, default=lambda: datetime.datetime.now(datetime.timezone.utc))

    # Relationships
    devices = relationship("Device", back_populates="user", cascade="all, delete-orphan")

class Device(Base):
    __tablename__ = "devices"

    id = Column(String(64), primary_key=True, index=True) # UUID or client hardware ID
    user_id = Column(String(64), ForeignKey("users.id"), nullable=True, index=True)
    device_type = Column(String(20), nullable=False) # 'RECORDER' or 'CONTROLLER'
    name = Column(String(100), nullable=False)
    pairing_code = Column(String(16), unique=True, index=True, nullable=True) # e.g. "CDX-9481"
    paired_with = Column(String(64), nullable=True) # Recorder ID paired to this Controller or vice-versa
    api_token = Column(String(128), unique=True, index=True, nullable=False)
    created_at = Column(DateTime, default=lambda: datetime.datetime.now(datetime.timezone.utc))
    last_seen = Column(DateTime, default=lambda: datetime.datetime.now(datetime.timezone.utc))

    # Relationships
    user = relationship("User", back_populates="devices")
    schedules = relationship("Schedule", back_populates="recorder", cascade="all, delete-orphan")
    sessions = relationship("RecordingSession", back_populates="recorder", cascade="all, delete-orphan")
    recordings = relationship("Recording", back_populates="recorder", cascade="all, delete-orphan")
    status = relationship("DeviceStatus", back_populates="recorder", uselist=False, cascade="all, delete-orphan")

class Schedule(Base):
    __tablename__ = "schedules"

    id = Column(String(64), primary_key=True, default=lambda: str(uuid.uuid4()))
    recorder_id = Column(String(64), ForeignKey("devices.id"), nullable=False, index=True)
    label = Column(String(100), nullable=False, default="Scheduled Recording")
    start_time_str = Column(String(10), nullable=False) # e.g., "14:30" (24-hour HH:mm)
    stop_time_str = Column(String(10), nullable=False)  # e.g., "15:30" (24-hour HH:mm)
    days_of_week = Column(String(30), nullable=False, default="DAILY") # "DAILY", "MON,TUE,WED", or "ONCE:2026-11-01"
    is_active = Column(Boolean, default=True)
    version = Column(Integer, default=1)
    updated_at = Column(DateTime, default=lambda: datetime.datetime.now(datetime.timezone.utc), onupdate=lambda: datetime.datetime.now(datetime.timezone.utc))

    recorder = relationship("Device", back_populates="schedules")

class RecordingSession(Base):
    __tablename__ = "recording_sessions"

    id = Column(String(64), primary_key=True, default=lambda: str(uuid.uuid4()))
    recorder_id = Column(String(64), ForeignKey("devices.id"), nullable=False, index=True)
    schedule_id = Column(String(64), nullable=True)
    started_at = Column(DateTime, nullable=False)
    ended_at = Column(DateTime, nullable=True)
    status = Column(String(30), nullable=False, default="RECORDING") # RECORDING, COMPLETED, INTERRUPTED, FAILED
    error_message = Column(Text, nullable=True)

    recorder = relationship("Device", back_populates="sessions")
    recordings = relationship("Recording", back_populates="session", cascade="all, delete-orphan")

class Recording(Base):
    __tablename__ = "recordings"

    id = Column(String(64), primary_key=True) # UUID generated on Phone A to guarantee deduplication
    session_id = Column(String(64), ForeignKey("recording_sessions.id"), nullable=False, index=True)
    recorder_id = Column(String(64), ForeignKey("devices.id"), nullable=False, index=True)
    filename = Column(String(255), nullable=False)
    duration_ms = Column(BigInteger, default=0)
    file_size_bytes = Column(BigInteger, default=0)
    checksum_sha256 = Column(String(64), nullable=False)
    storage_path = Column(String(500), nullable=False)
    uploaded_at = Column(DateTime, default=lambda: datetime.datetime.now(datetime.timezone.utc))

    session = relationship("RecordingSession", back_populates="recordings")
    recorder = relationship("Device", back_populates="recordings")

class DeviceStatus(Base):
    __tablename__ = "device_status"

    recorder_id = Column(String(64), ForeignKey("devices.id"), primary_key=True)
    battery_pct = Column(Integer, default=100)
    is_charging = Column(Boolean, default=False)
    storage_free_bytes = Column(BigInteger, default=0)
    storage_total_bytes = Column(BigInteger, default=0)
    network_type = Column(String(30), default="WIFI")
    current_state = Column(String(30), default="IDLE") # IDLE, RECORDING, SYNCING, ERROR
    updated_at = Column(DateTime, default=lambda: datetime.datetime.now(datetime.timezone.utc), onupdate=lambda: datetime.datetime.now(datetime.timezone.utc))

    recorder = relationship("Device", back_populates="status")

class SyncEvent(Base):
    __tablename__ = "sync_events"

    id = Column(String(64), primary_key=True, default=lambda: str(uuid.uuid4()))
    device_id = Column(String(64), nullable=False, index=True)
    event_type = Column(String(50), nullable=False) # e.g. SCHEDULE_PULL, AUDIO_UPLOAD, HEARTBEAT
    details = Column(Text, nullable=True)
    timestamp = Column(DateTime, default=lambda: datetime.datetime.now(datetime.timezone.utc))
