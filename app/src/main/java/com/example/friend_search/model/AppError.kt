package com.example.friend_search.model

import com.amplifyframework.auth.AuthException
import com.amplifyframework.auth.cognito.exceptions.service.FailedAttemptsLimitExceededException
import com.amplifyframework.auth.cognito.exceptions.service.LimitExceededException
import com.amplifyframework.auth.cognito.exceptions.service.TooManyRequestsException
import com.amplifyframework.auth.cognito.exceptions.service.UserNotFoundException
import com.amplifyframework.auth.exceptions.NotAuthorizedException
import com.amplifyframework.auth.exceptions.SessionExpiredException
import com.amplifyframework.auth.exceptions.SignedOutException
import com.example.friend_search.apimodel.HttpErrorApiModel
import java.io.IOException
import kotlinx.serialization.SerializationException

/** アプリ内で扱う通信・認証のエラー。表示する文言は ViewModel が決める。 */
sealed class AppError : Exception() {
    // 業務 API（サーバーが返したメッセージがあれば保持する）
    data class Validation(val serverMessage: String?) : AppError()
    data object UnknownGame : AppError()
    data object Unauthorized : AppError()
    data class Forbidden(val serverMessage: String?) : AppError()
    data object ProfileRequired : AppError()
    data object ProfileNotFound : AppError()
    data object NotFound : AppError()
    data object PostFull : AppError()
    data object PostClosed : AppError()
    data object IdempotencyConflict : AppError()
    data object ConcurrentUpdate : AppError()
    data object RateLimited : AppError()
    data object SearchUnavailable : AppError()
    data object TemporarilyUnavailable : AppError()
    data class Server(val status: Int, val serverMessage: String?) : AppError()

    // 通信・応答
    data object Network : AppError()
    data object InvalidResponse : AppError()
    data object Unexpected : AppError()

    // 認証
    data class Auth(val failure: AuthFailure) : AppError()

    // MARK: - API / SDK のエラー → AppError

    companion object {
        fun from(error: Throwable): AppError = when (error) {
            is AppError -> error
            is HttpErrorApiModel -> from(error)
            is AuthException -> from(error)
            is ModelConversionError, is SerializationException -> InvalidResponse
            is IOException -> Network
            else -> Unexpected
        }

        /** エラーコードで分類する。API Gateway が生成する 401 / 429 等は共通形式の本文がないため HTTP ステータスで分類する */
        fun from(apiModel: HttpErrorApiModel): AppError {
            val message = apiModel.body?.error?.message
            return when (apiModel.body?.error?.code) {
                "UNKNOWN_GAME" -> UnknownGame
                "PROFILE_NOT_FOUND" -> ProfileNotFound
                "PROFILE_REQUIRED" -> ProfileRequired
                "POST_FULL" -> PostFull
                "POST_CLOSED" -> PostClosed
                "IDEMPOTENCY_CONFLICT" -> IdempotencyConflict
                "CONCURRENT_UPDATE" -> ConcurrentUpdate
                "SEARCH_UNAVAILABLE" -> SearchUnavailable
                else -> when (apiModel.statusCode) {
                    400 -> Validation(message)
                    401 -> Unauthorized
                    403 -> Forbidden(message)
                    404 -> NotFound
                    409 -> ConcurrentUpdate
                    429 -> RateLimited
                    503 -> TemporarilyUnavailable
                    else -> Server(apiModel.statusCode, message)
                }
            }
        }

        /** セッション切れはログインし直し、通信障害は再試行できるエラーにする */
        fun from(apiModel: AuthException): AppError = when {
            apiModel is SignedOutException || apiModel is SessionExpiredException -> Unauthorized
            apiModel.causes().any { it is IOException } -> Network
            apiModel is LimitExceededException || apiModel is TooManyRequestsException ||
                apiModel is FailedAttemptsLimitExceededException -> Auth(AuthFailure.LimitExceeded)
            apiModel is UserNotFoundException -> Auth(AuthFailure.UserNotFound)
            apiModel is NotAuthorizedException -> Auth(AuthFailure.InvalidCredentials)
            else -> Auth(AuthFailure.Unexpected(apiModel.message ?: apiModel.javaClass.simpleName))
        }

        private fun Throwable.causes(): Sequence<Throwable> = generateSequence(cause) { it.cause }
    }
}

/** Cognito の認証エラー */
sealed interface AuthFailure {
    data object InvalidCredentials : AuthFailure
    data object LimitExceeded : AuthFailure
    data object UserNotFound : AuthFailure
    data class Unexpected(val detail: String) : AuthFailure
}
