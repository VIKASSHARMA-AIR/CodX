import datetime
from typing import Optional, List
from pydantic import BaseModel, Field

# Auth schemas
class GoogleLoginRequest(BaseModel):
    id_token: str
    email: Optional[str] = None
    display_name: Optional[str] = None
    photo_url: Optional[str] = None

class UserAuthResponse(BaseModel):
    user_id: str
    firebase_uid: str
    email: str
    display_name: Optional[str] = None
    photo_url: Optional[str] = None
    api_token: str

class UserProfileResponse(BaseModel):
    user_id: str
    email: str
    display_name: Optional[str] = None
    photo_url: Optional[str] = None
    devices_count: int

class DeleteAccountResponse(BaseModel):
    success: bool
    message: str

class RegisterRecorderRequest(BaseModel):
    name: str = "CodX Recorder Phone"
    device_id: Optional[str] = None

class RegisterControllerRequest(BaseModel):
    name: str = "CodX Controller Phone"
    device_id: Optional[str] = None

class DeviceAuthResponse(BaseModel):
    device_id: str
    device_type: str
    name: str
    pairing_code: Optional[str] = None
    paired_with: Optional[str] = None
    api_token: str

class PairDeviceRequest(BaseModel):
    controller_id: str
    pairing_code: str

class PairDeviceResponse(BaseModel):
    success: bool
    message: str
    recorder_id: str
    recorder_name: str

# Status schemas
class RecorderStatusUpdate(BaseModel):
    recorder_id: str
    battery_pct: int
    is_charging: bool
    storage_free_bytes: int
    storage_total_bytes: int
    network_type: str
    current_state: str

class DeviceStatusResponse(BaseModel):
    recorder_id: str
    battery_pct: int
    is_charging: bool
    storage_free_bytes: int
    storage_total_bytes: int
    network_type: str
    current_state: str
    is_online: bool
    last_seen: datetime.datetime

# Schedule schemas
class ScheduleCreate(BaseModel):
    label: str = "Scheduled Recording"
    start_time_str: str # "14:30"
    stop_time_str: str  # "15:00"
    days_of_week: str = "DAILY" # "DAILY", "MON,TUE,WED", "FRI", or "ONCE:2026-11-01"
    is_active: bool = True

class ScheduleUpdate(BaseModel):
    label: Optional[str] = None
    start_time_str: Optional[str] = None
    stop_time_str: Optional[str] = None
    days_of_week: Optional[str] = None
    is_active: Optional[bool] = None

class ScheduleResponse(BaseModel):
    id: str
    recorder_id: str
    label: str
    start_time_str: str
    stop_time_str: str
    days_of_week: str
    is_active: bool
    version: int
    updated_at: datetime.datetime

    class Config:
        from_attributes = True

# Session schemas
class SessionCreate(BaseModel):
    id: str
    recorder_id: str
    schedule_id: Optional[str] = None
    started_at: datetime.datetime
    status: str = "RECORDING"

class SessionEnd(BaseModel):
    ended_at: datetime.datetime
    status: str
    error_message: Optional[str] = None

class SessionResponse(BaseModel):
    id: str
    recorder_id: str
    schedule_id: Optional[str] = None
    started_at: datetime.datetime
    ended_at: Optional[datetime.datetime] = None
    status: str
    error_message: Optional[str] = None

    class Config:
        from_attributes = True

# Recording schemas
class RecordingMeta(BaseModel):
    id: str # UUID from Phone A
    session_id: str
    recorder_id: str
    duration_ms: int
    file_size_bytes: int
    checksum_sha256: str
    filename: str

class RecordingResponse(BaseModel):
    id: str
    session_id: str
    recorder_id: str
    filename: str
    duration_ms: int
    file_size_bytes: int
    checksum_sha256: str
    uploaded_at: datetime.datetime
    download_url: str
    stream_url: str

    class Config:
        from_attributes = True

# Sync Event schema
class SyncEventCreate(BaseModel):
    device_id: str
    event_type: str
    details: Optional[str] = None
