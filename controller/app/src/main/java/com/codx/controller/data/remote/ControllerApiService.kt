package com.codx.controller.data.remote

import com.codx.controller.data.remote.models.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ControllerApiService {

    @POST("api/auth/google-login")
    suspend fun loginWithGoogle(
        @Body request: GoogleLoginRequestDto
    ): Response<UserAuthResponseDto>

    @DELETE("api/auth/delete-account")
    suspend fun deleteAccount(
        @Header("Authorization") authorization: String
    ): Response<DeleteAccountResponseDto>

    @GET("api/auth/me")
    suspend fun getMe(
        @Header("Authorization") authorization: String
    ): Response<Map<String, Any>>

    @POST("api/auth/register-controller")
    suspend fun registerController(
        @Body request: RegisterControllerDto
    ): Response<DeviceAuthDto>

    @POST("api/auth/pair")
    suspend fun pairWithRecorder(
        @Body request: PairDeviceDto
    ): Response<PairResponseDto>

    @GET("api/controller/recorder-status/{recorder_id}")
    suspend fun getRecorderStatus(
        @Path("recorder_id") recorderId: String
    ): Response<RecorderStatusDto>

    @GET("api/schedules/{recorder_id}")
    suspend fun getSchedules(
        @Path("recorder_id") recorderId: String
    ): Response<List<ScheduleDto>>

    @POST("api/schedules/{recorder_id}")
    suspend fun createSchedule(
        @Path("recorder_id") recorderId: String,
        @Body request: CreateScheduleDto
    ): Response<ScheduleDto>

    @PUT("api/schedules/{recorder_id}/{schedule_id}")
    suspend fun updateSchedule(
        @Path("recorder_id") recorderId: String,
        @Path("schedule_id") scheduleId: String,
        @Body request: UpdateScheduleDto
    ): Response<ScheduleDto>

    @DELETE("api/schedules/{recorder_id}/{schedule_id}")
    suspend fun deleteSchedule(
        @Path("recorder_id") recorderId: String,
        @Path("schedule_id") scheduleId: String
    ): Response<Unit>

    @GET("api/recordings/{recorder_id}")
    suspend fun listRecordings(
        @Path("recorder_id") recorderId: String
    ): Response<List<RecordingItemDto>>

    @Streaming
    @GET("api/recordings/{recording_id}/download")
    suspend fun downloadRecording(
        @Path("recording_id") recordingId: String
    ): Response<ResponseBody>
}
