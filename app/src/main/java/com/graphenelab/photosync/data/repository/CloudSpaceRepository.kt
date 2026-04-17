package com.graphenelab.photosync.data.repository

import android.util.Log
import com.graphenelab.photosync.data.network.payment.SubscriptionPlan
import com.graphenelab.photosync.data.network.user.CloudSpaceService
import com.graphenelab.photosync.domain.model.CloudSpaceCredentials
import com.graphenelab.photosync.domain.repositroy.DeleteAccountResult
import com.graphenelab.photosync.domain.repositroy.ICloudSpaceRepository
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudSpaceRepository @Inject constructor(
    private val cloudSpaceService: CloudSpaceService
) : ICloudSpaceRepository {
    companion object{
        private const val TAG = "CloudSpaceRepository"
    }
    override suspend fun getCloudSpaceCredentials(): Result<CloudSpaceCredentials> {
        Log.i(TAG, "getCloudSpaceCredentials starting...")
        return try {
            val response = cloudSpaceService.getCloudSpaceCredentials()
            Log.d(TAG, "getCloudSpaceCredentials success: ${response.qrEncrypted}")
            Result.success(
                CloudSpaceCredentials(
                    qrEncrypted = response.qrEncrypted,
                    pin = response.pin
                )
            )
        } catch (e: HttpException) {
            Log.w(TAG, "getCloudSpaceCredentials http error: ${e.code()}")
            Result.failure(
                when (e.code()) {
                    401 -> UnauthorizedException("User not authenticated")
                    404 -> NotFoundException("Cloud space credentials not found")
                    500 -> ServerErrorException("Server error occurred")
                    else -> NetworkException("HTTP error: ${e.code()}")
                }
            )
        } catch (e: IOException) {
            Log.w(TAG, "getCloudSpaceCredentials network error: ${e.message}")
            Result.failure(NetworkException("Network error: ${e.message}"))
        }
        // catch all other type of exceptions
        catch (e: Exception) {
            Log.e(TAG, "getCloudSpaceCredentials unexpected error: ${e.message}", e)
            Result.failure(NetworkException("Unexpected error: ${e.message}"))
        }
    }

    override suspend fun getCurrentSubscriptionPlan(): Result<SubscriptionPlan> {
        return try {
            val plan = cloudSpaceService.getCurrentSubscriptionPlan()
            Result.success(plan)
        } catch (e: HttpException) {
            Result.failure(
                when (e.code()) {
                    401 -> UnauthorizedException("User not authenticated")
                    404 -> NotFoundException("Subscription plan not found")
                    else -> NetworkException("HTTP error: ${e.code()}")
                }
            )
        } catch (e: IOException) {
            Result.failure(NetworkException("Network error: ${e.message}"))
        }
    }

    override suspend fun deleteCurrentUserAccount(): Result<DeleteAccountResult> {
        return try {
            val response = cloudSpaceService.deleteCurrentUserAccount()
            when {
                response.code() == 204 || response.isSuccessful -> {
                    Result.success(DeleteAccountResult.Deleted)
                }

                response.code() == 401 || response.code() == 403 -> {
                    Result.failure(SessionExpiredException("Session expired"))
                }

                response.code() == 409 && isUserNotFoundConflict(response.errorBody()?.string()) -> {
                    Result.success(DeleteAccountResult.UserNotFound)
                }

                else -> {
                    Result.failure(
                        RetryableOperationException("Unable to delete account. Please try again.")
                    )
                }
            }
        } catch (e: IOException) {
            Result.failure(RetryableOperationException("Unable to delete account. Please try again."))
        } catch (e: Exception) {
            Result.failure(RetryableOperationException("Unable to delete account. Please try again."))
        }
    }

    private fun isUserNotFoundConflict(errorBody: String?): Boolean {
        if (errorBody.isNullOrBlank()) return false
        return errorBody.contains("USER_NOT_FOUND", ignoreCase = true)
    }
}

// Custom exception classes
class NetworkException(message: String) : Exception(message)
class UnauthorizedException(message: String) : Exception(message)
class NotFoundException(message: String) : Exception(message)
class ServerErrorException(message: String) : Exception(message)
class SessionExpiredException(message: String) : Exception(message)
class RetryableOperationException(message: String) : Exception(message)
