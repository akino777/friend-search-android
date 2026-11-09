package com.example.friend_search.viewmodel.post

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.friend_search.model.AppError
import com.example.friend_search.model.Post
import com.example.friend_search.usecase.post.ClosePostUseCase
import com.example.friend_search.usecase.post.DefaultClosePostUseCase
import com.example.friend_search.usecase.post.DefaultGetPostUseCase
import com.example.friend_search.usecase.post.DefaultJoinPostUseCase
import com.example.friend_search.usecase.post.DefaultLeavePostUseCase
import com.example.friend_search.usecase.post.GetPostUseCase
import com.example.friend_search.usecase.post.JoinPostUseCase
import com.example.friend_search.usecase.post.LeavePostUseCase
import com.example.friend_search.viewmodel.shared.userMessage
import kotlin.coroutines.cancellation.CancellationException

/** 募集詳細（参加・取消・投稿者による終了） */
class PostDetailViewModel(
    val postId: String,
    initialPost: Post?,
    private val getPost: GetPostUseCase = DefaultGetPostUseCase(),
    private val joinPost: JoinPostUseCase = DefaultJoinPostUseCase(),
    private val leavePost: LeavePostUseCase = DefaultLeavePostUseCase(),
    private val closePost: ClosePostUseCase = DefaultClosePostUseCase(),
    /** 最新の募集を呼び出し元へ伝える（AI 検索の結果に反映する） */
    private val onUpdate: (Post) -> Unit = {},
) : ViewModel() {
    var post by mutableStateOf(initialPost)
        private set
    var loadError by mutableStateOf<AppError?>(null)
        private set
    var isProcessing by mutableStateOf(false)
        private set
    var actionMessage by mutableStateOf<String?>(null)

    /** 募集が削除された・存在しない場合に画面を閉じる */
    var shouldDismiss by mutableStateOf(false)
        private set

    /** 取得や操作を始めるたびに進める。それより前に始めた取得の結果は古いので捨てる */
    private var generation = 0

    suspend fun load() {
        generation += 1
        val current = generation
        try {
            val latest = getPost.execute(postId)
            if (current != generation) return
            show(latest)
            loadError = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (current != generation) return
            when (val appError = AppError.from(e)) {
                AppError.NotFound -> shouldDismiss = true
                else -> loadError = appError
            }
        }
    }

    suspend fun join() = perform { joinPost.execute(postId) }
    suspend fun leave() = perform { leavePost.execute(postId) }
    suspend fun close() = perform { closePost.execute(postId) }

    /** 成功する前に確定した表示をしない。失敗したら理由を出し、必要なら最新状態を取り直す */
    private suspend fun perform(operation: suspend () -> Post) {
        if (isProcessing) return
        isProcessing = true
        actionMessage = null
        try {
            val updated = operation()
            // 操作の前に始めた取得の結果で、操作後の状態を上書きしない
            generation += 1
            show(updated)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val appError = AppError.from(e)
            if (appError == AppError.NotFound) {
                shouldDismiss = true
                return
            }
            actionMessage = appError.userMessage
            if (needsReload(appError)) load()
        } finally {
            isProcessing = false
        }
    }

    private fun show(latest: Post) {
        post = latest
        onUpdate(latest)
    }

    /** 失敗後に最新状態を取り直すべきか（競合・状態変化・一時的な障害） */
    private fun needsReload(error: AppError): Boolean = when (error) {
        AppError.PostFull, AppError.PostClosed, AppError.ConcurrentUpdate, is AppError.Forbidden,
        AppError.Network, AppError.TemporarilyUnavailable -> true
        else -> false
    }
}
