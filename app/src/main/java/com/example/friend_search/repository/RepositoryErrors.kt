package com.example.friend_search.repository

import com.example.friend_search.model.AppError
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/** API / SDK のエラーを AppError に置き換える（変換規則は Model の `AppError.from`） */
suspend fun <T> withAppErrors(operation: suspend () -> T): T = try {
    operation()
} catch (e: CancellationException) {
    // 取り消し（画面離脱など）はエラーとして表示しない
    throw e
} catch (e: Exception) {
    currentCoroutineContext().ensureActive()
    throw AppError.from(e)
}
