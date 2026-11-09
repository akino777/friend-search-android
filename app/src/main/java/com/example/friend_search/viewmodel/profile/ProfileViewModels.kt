package com.example.friend_search.viewmodel.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.friend_search.model.AppError
import com.example.friend_search.model.Post
import com.example.friend_search.model.PostPage
import com.example.friend_search.model.PublicProfile
import com.example.friend_search.usecase.post.DefaultListMyPostsUseCase
import com.example.friend_search.usecase.post.ListMyPostsUseCase
import com.example.friend_search.usecase.profile.BootstrapProfileUseCase
import com.example.friend_search.usecase.profile.DefaultBootstrapProfileUseCase
import com.example.friend_search.usecase.profile.DefaultUpdateProfileUseCase
import com.example.friend_search.usecase.profile.UpdateProfileUseCase
import com.example.friend_search.viewmodel.shared.DisplayNameRule
import com.example.friend_search.viewmodel.shared.ListPhase
import com.example.friend_search.viewmodel.shared.PostPager
import com.example.friend_search.viewmodel.shared.userMessage
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** 初回の表示名設定 */
class ProfileSetupViewModel(
    private val bootstrap: BootstrapProfileUseCase = DefaultBootstrapProfileUseCase(),
) : ViewModel() {
    var displayName by mutableStateOf("")
    var isSubmitting by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)

    val canSubmit: Boolean get() = DisplayNameRule.normalized(displayName) != null && !isSubmitting

    suspend fun submit(): PublicProfile? {
        if (isSubmitting) return null
        val name = DisplayNameRule.normalized(displayName) ?: run {
            errorMessage = DisplayNameRule.INVALID_MESSAGE
            return null
        }
        isSubmitting = true
        errorMessage = null
        try {
            return bootstrap.execute(name)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorMessage = AppError.from(e).userMessage
            return null
        } finally {
            isSubmitting = false
        }
    }
}

/** マイページ（自分の募集・参加中の募集・表示名変更） */
class MyPageViewModel(
    profile: PublicProfile,
    private val listMyPosts: ListMyPostsUseCase = DefaultListMyPostsUseCase(),
    private val updateProfile: UpdateProfileUseCase = DefaultUpdateProfileUseCase(),
    private val onProfileUpdated: (PublicProfile) -> Unit = {},
) : ViewModel() {
    enum class Segment(val label: String) {
        MY_POSTS("自分の募集"),
        PARTICIPATIONS("参加中"),
    }

    var profile by mutableStateOf(profile)
        private set
    var segment by mutableStateOf(Segment.MY_POSTS)
    var phase by mutableStateOf<ListPhase>(ListPhase.Idle)
        private set
    private val pagers = mutableStateMapOf<Segment, PostPager>()
    var editingName by mutableStateOf("")
    var isSavingName by mutableStateOf(false)
        private set
    var nameError by mutableStateOf<String?>(null)

    /** 取り直しのたびに進める。それより前に始めた取得の結果は古いので捨てる */
    private var generation = 0

    val visiblePosts: List<Post> get() = pagers[segment]?.posts ?: emptyList()
    val emptyMessage: String get() = if (segment == Segment.MY_POSTS) "まだ募集を投稿していません" else "参加中の募集はありません"
    val isLoadingMore: Boolean get() = pagers[segment]?.isLoadingMore ?: false
    val loadMoreError: AppError? get() = pagers[segment]?.loadMoreError

    /** 表示のたびに呼び、自分の募集と参加中の募集を先頭から取り直す */
    suspend fun reload() {
        generation += 1
        val current = generation
        if (visiblePosts.isEmpty()) phase = ListPhase.Loading
        try {
            val (posts, joined) = coroutineScope {
                val postsPage = async { fetch(Segment.MY_POSTS, null) }
                val joinedPage = async { fetch(Segment.PARTICIPATIONS, null) }
                postsPage.await() to joinedPage.await()
            }
            if (current != generation) return
            pagers[Segment.MY_POSTS] = (pagers[Segment.MY_POSTS] ?: PostPager()).reset(posts)
            pagers[Segment.PARTICIPATIONS] = (pagers[Segment.PARTICIPATIONS] ?: PostPager()).reset(joined)
            phase = if (visiblePosts.isEmpty()) ListPhase.Empty else ListPhase.Loaded
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (current != generation) return
            phase = ListPhase.Failed(AppError.from(e))
        }
    }

    fun segmentChanged() {
        if (phase is ListPhase.Failed) return
        phase = if (visiblePosts.isEmpty()) ListPhase.Empty else ListPhase.Loaded
    }

    /** 一覧の末尾が表示されたら次ページを読み込む */
    suspend fun loadMoreIfNeeded(current: Post) {
        if (current.id != visiblePosts.lastOrNull()?.id || loadMoreError != null) return
        loadMore()
    }

    /** 追加読み込みの失敗は読み込み済みの一覧を壊さず、カーソルを残して再試行できるようにする */
    suspend fun loadMore() {
        val target = segment
        val pager = pagers[target] ?: return
        val cursor = pager.nextCursor ?: return
        if (pager.isLoadingMore) return
        val current = generation
        update(target) { it.copy(isLoadingMore = true, loadMoreError = null) }
        try {
            val page = fetch(target, cursor)
            if (current != generation) return
            update(target) { it.append(page) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (current != generation) return
            update(target) { it.copy(loadMoreError = AppError.from(e)) }
        } finally {
            update(target) { it.copy(isLoadingMore = false) }
        }
    }

    private fun update(segment: Segment, change: (PostPager) -> PostPager) {
        pagers[segment]?.let { pagers[segment] = change(it) }
    }

    private suspend fun fetch(segment: Segment, cursor: String?): PostPage = PostPager.fetch(cursor) {
        when (segment) {
            Segment.MY_POSTS -> listMyPosts.myPosts(it)
            Segment.PARTICIPATIONS -> listMyPosts.participations(it)
        }
    }

    val canSaveName: Boolean get() = DisplayNameRule.normalized(editingName) != null && !isSavingName

    fun beginEditingName() {
        editingName = profile.displayName
        nameError = null
    }

    suspend fun saveName(): Boolean {
        if (isSavingName) return false
        val name = DisplayNameRule.normalized(editingName) ?: run {
            nameError = DisplayNameRule.INVALID_MESSAGE
            return false
        }
        isSavingName = true
        nameError = null
        try {
            profile = updateProfile.execute(name)
            onProfileUpdated(profile)
            return true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            nameError = AppError.from(e).userMessage
            return false
        } finally {
            isSavingName = false
        }
    }
}
