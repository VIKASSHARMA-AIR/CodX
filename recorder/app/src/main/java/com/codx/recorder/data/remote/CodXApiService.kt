package com.codx.recorder.data.remote

import com.codx.recorder.data.remote.models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface CodXApiService {

    @POST("api/auth/register-recorder")
    suspend fun registerRecorder(
        @Body request: RegisterRecorderDto
    ): Response<DeviceAuthDto>

    @POST("api/recorder/status")
    suspend fun updateStatus(
        @Body request: RecorderStatusDto
    ): Response<Map<String, Any>>

    @GET("api/schedules/{recorder_id}")
    suspend fun getSchedules(
        @Path("recorder_id") recorderId: String
    ): Response<List<ScheduleDto>>

    @POST("api/sessions")
    suspend fun createSession(
        @Body request: SessionCreateDto
    ): Response<SessionCreateDto>

    @PUT("api/sessions/{session_id}/end")
    suspend fun endSession(
        @Path("session_id") sessionId: String,
        @Body request: SessionEndDto
    ): Response<Map<String, Any>>

    @Multipart
    @POST("api/recordings/upload")
    suspend fun uploadRecording(
        @Part("id") id: RequestBody,
        @Part("session_id") sessionId: RequestBody,
        @Part("recorder_id") recorderId: RequestBody,
        @Part("duration_ms") durationMs: RequestBody,
        @Part("checksum_sha256") checksumSha256: RequestBody,
        @Part file: MultipartBody.Part
    ): Response<RecordingResponseDto>
}
