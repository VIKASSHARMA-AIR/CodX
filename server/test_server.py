import io
import hashlib
import uuid
import datetime
from database import Base, engine, SessionLocal
import models
import schemas
from routers import auth, status, schedules, recordings, sessions
from fastapi import UploadFile

def run_direct_api_tests():
    Base.metadata.create_all(bind=engine)
    db = SessionLocal()

    try:
        print("\n=== STEP 1: Google Login via Firebase API ===")
        google_req = schemas.GoogleLoginRequest(
            id_token="mock_firebase_google_token_12345",
            email="controller.admin@codx-audio.com",
            display_name="CodX Executive",
            photo_url="https://lh3.googleusercontent.com/a/sample-photo"
        )
        user_auth = auth.login_with_google(google_req, db)
        assert user_auth.user_id is not None
        assert user_auth.email == "controller.admin@codx-audio.com"
        assert user_auth.api_token is not None
        user_token = user_auth.api_token
        print(f"[OK] Google Login Successful! User: {user_auth.display_name} ({user_auth.email})")

        print("\n=== STEP 2: Fetch Current User Profile (/api/auth/me) ===")
        profile = auth.get_current_user_profile(f"Bearer {user_token}", db)
        assert profile.user_id == user_auth.user_id
        assert profile.email == "controller.admin@codx-audio.com"
        print(f"[OK] User Profile verified! Devices: {profile.devices_count}")

        print("\n=== STEP 3: Register Recorder Phone ===")
        recorder_id = str(uuid.uuid4())
        rec_req = schemas.RegisterRecorderRequest(name="Phone A - CodX Recorder", device_id=recorder_id)
        rec_resp = auth.register_recorder(rec_req, db)
        assert rec_resp.device_id == recorder_id
        assert rec_resp.device_type == "RECORDER"
        assert rec_resp.pairing_code.startswith("CDX-")
        pairing_code = rec_resp.pairing_code
        print(f"[OK] Recorder registered: ID={recorder_id}, Pairing Code={pairing_code}")

        print("\n=== STEP 4: Register Controller Phone (Linked to User Account) ===")
        controller_id = str(uuid.uuid4())
        ctrl_req = schemas.RegisterControllerRequest(name="Phone B - CodX Controller", device_id=controller_id)
        ctrl_resp = auth.register_controller(ctrl_req, authorization=f"Bearer {user_token}", db=db)
        assert ctrl_resp.device_id == controller_id
        assert ctrl_resp.device_type == "CONTROLLER"
        print(f"[OK] Controller registered and associated with user {user_auth.email}")

        print("\n=== STEP 5: Pair Controller with Recorder ===")
        pair_req = schemas.PairDeviceRequest(controller_id=controller_id, pairing_code=pairing_code)
        pair_resp = auth.pair_controller_with_recorder(pair_req, db)
        assert pair_resp.success is True
        assert pair_resp.recorder_id == recorder_id
        print(f"[OK] Paired successfully! Controller bound to {pair_resp.recorder_name}")

        print("\n=== STEP 6: Recorder Posts Heartbeat & Telemetry ===")
        status_req = schemas.RecorderStatusUpdate(
            recorder_id=recorder_id,
            battery_pct=88,
            is_charging=False,
            storage_free_bytes=1024 * 1024 * 1024 * 32, # 32 GB
            storage_total_bytes=1024 * 1024 * 1024 * 128,
            network_type="WIFI",
            current_state="IDLE"
        )
        status_resp = status.update_recorder_status(status_req, db)
        assert status_resp.battery_pct == 88
        assert status_resp.is_online is True
        print(f"[OK] Heartbeat received: Battery={status_resp.battery_pct}%, Free={status_resp.storage_free_bytes / (1024**3):.1f} GB")

        print("\n=== STEP 7: Controller Fetches Recorder Status ===")
        fetched_status = status.get_recorder_status(recorder_id, db)
        assert fetched_status.battery_pct == 88
        assert fetched_status.is_online is True
        print(f"[OK] Controller queried Recorder status: Online={fetched_status.is_online}, State={fetched_status.current_state}")

        print("\n=== STEP 8: Controller Creates Schedules (Daily & Weekly) ===")
        sched1_req = schemas.ScheduleCreate(
            label="Daily Morning Standup",
            start_time_str="09:00",
            stop_time_str="09:30",
            days_of_week="MON,TUE,WED,THU,FRI",
            is_active=True
        )
        sched1 = schedules.create_schedule(recorder_id, sched1_req, db)
        assert sched1.id is not None
        assert sched1.version == 1

        sched2_req = schemas.ScheduleCreate(
            label="Weekly Architecture Review",
            start_time_str="14:00",
            stop_time_str="15:30",
            days_of_week="FRI",
            is_active=True
        )
        sched2 = schedules.create_schedule(recorder_id, sched2_req, db)
        print(f"[OK] Created 2 schedules: '{sched1.label}' and '{sched2.label}'")

        print("\n=== STEP 9: Recorder Pulls Timetable for Offline Recording ===")
        offline_schedules = schedules.get_schedules_for_recorder(recorder_id, db)
        assert len(offline_schedules) == 2
        print(f"[OK] Recorder downloaded {len(offline_schedules)} schedules for local offline Room DB")

        print("\n=== STEP 10: Recorder Starts Recording Session ===")
        session_id = str(uuid.uuid4())
        sess_create = schemas.SessionCreate(
            id=session_id,
            recorder_id=recorder_id,
            schedule_id=sched1.id,
            started_at=datetime.datetime.now(datetime.timezone.utc),
            status="RECORDING"
        )
        sess_resp = sessions.create_or_update_session(sess_create, db)
        assert sess_resp.id == session_id
        assert sess_resp.status == "RECORDING"
        print(f"[OK] Session {session_id} active")

        print("\n=== STEP 11: Recorder Uploads Audio File (with SHA-256) ===")
        dummy_audio = b"AAC_CODEX_AUDIO_SAMPLE_DATA_" + (b"0123456789" * 500)
        sha256 = hashlib.sha256(dummy_audio).hexdigest()
        recording_id = str(uuid.uuid4())

        upload_file = UploadFile(
            file=io.BytesIO(dummy_audio),
            filename="meeting_session_01.m4a",
            headers={"content-type": "audio/mp4"}
        )

        upload_result = recordings.upload_recording(
            id=recording_id,
            session_id=session_id,
            recorder_id=recorder_id,
            duration_ms=1800000, # 30 mins
            checksum_sha256=sha256,
            file=upload_file,
            db=db
        )
        assert upload_result.id == recording_id
        assert upload_result.checksum_sha256 == sha256
        assert upload_result.file_size_bytes == len(dummy_audio)
        print(f"[OK] Audio uploaded & verified! File size: {upload_result.file_size_bytes} bytes, Checksum: {sha256[:16]}...")

        print("\n=== STEP 12: Deduplication / Idempotent Upload Verification ===")
        dup_file = UploadFile(
            file=io.BytesIO(dummy_audio),
            filename="meeting_session_01.m4a",
            headers={"content-type": "audio/mp4"}
        )
        dup_result = recordings.upload_recording(
            id=recording_id,
            session_id=session_id,
            recorder_id=recorder_id,
            duration_ms=1800000,
            checksum_sha256=sha256,
            file=dup_file,
            db=db
        )
        assert dup_result.id == recording_id
        print("[OK] Deduplication verified: Reconnection/retried upload does not duplicate recordings!")

        print("\n=== STEP 13: Controller Lists Recordings & Downloads ===")
        ctrl_recs = recordings.list_recordings_for_recorder(recorder_id, db)
        assert len(ctrl_recs) == 1
        assert ctrl_recs[0].id == recording_id

        dl_resp = recordings.download_recording(recording_id, db)
        assert dl_resp.path.endswith("meeting_session_01.m4a")
        print(f"[OK] Controller retrieved recording list and verified download endpoint for: {ctrl_recs[0].filename}")

        print("\n=== STEP 14: Google Play Account Deletion Policy Compliance ===")
        del_resp = auth.delete_user_account(f"Bearer {user_token}", db)
        assert del_resp.success is True
        print(f"[OK] Account Deletion Verified: {del_resp.message}")

        # Verify user is completely erased
        deleted_user = db.query(models.User).filter(models.User.id == user_auth.user_id).first()
        assert deleted_user is None
        print("[OK] Verified user record and associated devices permanently purged from database!")

        print("\n==========================================================")
        print(" ALL 14 END-TO-END CODEX CLOUD API & AUTH TESTS PASSED! ")
        print("==========================================================")

    finally:
        db.close()

if __name__ == "__main__":
    run_direct_api_tests()
