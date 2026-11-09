package com.example.friend_search.viewmodel.post

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.friend_search.model.AppError
import com.example.friend_search.model.Post
import com.example.friend_search.model.PostPage
import com.example.friend_search.model.VCTool
import com.example.friend_search.usecase.post.DefaultListPostsUseCase
import com.example.friend_search.usecase.post.DefaultSearchPostsUseCase
import com.example.friend_search.usecase.post.ListPostsUseCase
import com.example.friend_search.usecase.post.SearchPostsUseCase
import com.example.friend_search.viewmodel.shared.ListPhase
import com.example.friend_search.viewmodel.shared.PostInputRule
import com.example.friend_search.viewmodel.shared.PostPager
import com.example.friend_search.viewmodel.shared.userMessage
import java.time.Duration
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * 募集一覧（新着順・カーソルページング）と AI 検索。未選択なら全ゲーム、ゲームを選ぶとそのゲームに絞り込む。
 * AI 検索中・検索結果の表示中は、新着の代わりに検索結果を出す（検索ボタン押下で実行し、キーストローク単位では呼ばない）
 */
class PostListViewModel(
    private val listPosts: ListPostsUseCase = DefaultListPostsUseCase(),
    private val searchPosts: SearchPostsUseCase = DefaultSearchPostsUseCase(),
    private val now: () -> Instant = Instant::now,
) : ViewModel() {
    /** AI 検索の状態。Idle のときは新着を出す */
    sealed interface SearchPhase {
        data object Idle : SearchPhase
        data object Searching : SearchPhase
        data object Results : SearchPhase
        data object Empty : SearchPhase

        /** AI 検索が使えない */
        data object Unavailable : SearchPhase
        data class Failed(val error: AppError) : SearchPhase
    }

    /** 検索条件。同じ条件の重複実行を見分けるのに使う */
    private data class SearchRequest(val gameId: String?, val query: String, val vcTool: VCTool?)

    companion object {
        /** 同じ条件の再検索を抑止する時間（Bedrock の呼び出しを減らす） */
        val DUPLICATE_SEARCH_INTERVAL: Duration = Duration.ofSeconds(5)
    }

    /** 絞り込み中のゲーム。null なら全ゲーム */
    var selectedGameId by mutableStateOf<String?>(null)
        private set
    var phase by mutableStateOf<ListPhase>(ListPhase.Idle)
        private set
    var pager by mutableStateOf(PostPager())
        private set
    var searchPhase by mutableStateOf<SearchPhase>(SearchPhase.Idle)
        private set
    var searchResults by mutableStateOf<List<Post>>(emptyList())
        private set

    /** 取り直しのたびに進める。それより前に始めた取得の結果は古いので捨てる */
    private var loadGeneration = 0

    /** 検索の開始・解除のたびに進める。それより前に始めた検索の結果は古いので捨てる */
    private var searchGeneration = 0
    private var lastSearch: SearchRequest? = null
    private var lastSearchCompletedAt: Instant? = null

    val posts: List<Post> get() = pager.posts
    val isLoadingMore: Boolean get() = pager.isLoadingMore

    /** 検索中・検索結果の表示中か（新着の代わりに検索結果を出す） */
    val isSearchActive: Boolean get() = searchPhase != SearchPhase.Idle

    val searchErrorMessage: String?
        get() = when (val phase = searchPhase) {
            SearchPhase.Unavailable -> AppError.SearchUnavailable.userMessage
            is SearchPhase.Failed -> phase.error.userMessage
            else -> null
        }

    /** 絞り込むゲームを変える。null で解除（全ゲーム）。検索結果の表示中なら、同じ検索文で選んだゲームを検索し直す */
    suspend fun select(gameId: String?) {
        if (gameId == selectedGameId) return
        selectedGameId = gameId
        pager = PostPager()
        if (isSearchActive) {
            coroutineScope {
                launch { reload() }
                launch { searchAgain() }
            }
        } else {
            reload()
        }
    }

    /** 表示のたびに呼び、絞り込み中の条件で募集を先頭から取り直す（AI 検索は費用がかかるので取り直さない） */
    suspend fun reload() {
        val gameId = selectedGameId
        if (posts.isEmpty()) phase = ListPhase.Loading
        loadGeneration += 1
        val generation = loadGeneration
        try {
            val page = fetch(gameId, null)
            if (generation != loadGeneration) return
            pager = pager.reset(page)
            phase = if (posts.isEmpty()) ListPhase.Empty else ListPhase.Loaded
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (generation != loadGeneration) return
            phase = ListPhase.Failed(AppError.from(e))
        }
    }

    /** 引っ張って更新。検索結果の表示中は同じ条件で検索し直し、それ以外は新着を取り直す */
    suspend fun refresh() {
        if (isSearchActive) searchAgain() else reload()
    }

    /** 一覧の末尾が表示されたら次ページを読み込む。失敗しても読み込み済みの一覧は壊さない */
    suspend fun loadMoreIfNeeded(current: Post) {
        if (current.id != posts.lastOrNull()?.id || pager.isLoadingMore) return
        val cursor = pager.nextCursor ?: return
        val gameId = selectedGameId
        pager = pager.copy(isLoadingMore = true)
        val generation = loadGeneration
        try {
            val page = fetch(gameId, cursor)
            if (generation != loadGeneration) return
            pager = pager.append(page)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // 次に末尾が表示されたときに再試行する
        } finally {
            pager = pager.copy(isLoadingMore = false)
        }
    }

    /** 絞り込み中のゲーム（未選択なら全ゲーム）を AI 検索する。検索文が規則を満たさなければ何もしない */
    suspend fun search(query: String, vcTool: VCTool?) {
        val normalizedQuery = PostInputRule.normalizedQuery(query) ?: return
        runSearch(SearchRequest(selectedGameId, normalizedQuery, vcTool))
    }

    /** 検索を解除して新着に戻る。通信中の検索の結果は捨てる */
    fun clearSearch() {
        searchGeneration += 1
        lastSearch = null
        lastSearchCompletedAt = null
        searchResults = emptyList()
        searchPhase = SearchPhase.Idle
    }

    /** 詳細画面で更新された募集を検索結果に反映する（検索は費用がかかるので取り直さない） */
    fun applyToSearchResults(post: Post) {
        val index = searchResults.indexOfFirst { it.id == post.id }
        if (index >= 0) searchResults = searchResults.toMutableList().also { it[index] = post }
    }

    /** 直前の検索と同じ検索文・VC で、絞り込み中のゲームを検索し直す */
    private suspend fun searchAgain() {
        val last = lastSearch ?: return
        runSearch(SearchRequest(selectedGameId, last.query, last.vcTool))
    }

    private suspend fun runSearch(request: SearchRequest) {
        if (request == lastSearch && isDuplicateSearch) return

        searchGeneration += 1
        val current = searchGeneration
        lastSearch = request
        lastSearchCompletedAt = null
        searchPhase = SearchPhase.Searching
        try {
            val posts = searchPosts.execute(request.gameId, request.query, request.vcTool)
            if (current != searchGeneration) return
            searchResults = posts
            searchPhase = if (posts.isEmpty()) SearchPhase.Empty else SearchPhase.Results
            lastSearchCompletedAt = now()
        } catch (e: CancellationException) {
            // 取り消された。「検索中」のまま止まらないよう、検索を解除した状態に戻す
            if (current == searchGeneration) clearSearch()
            throw e
        } catch (e: Exception) {
            if (current != searchGeneration) return
            searchResults = emptyList()
            searchPhase = when (val appError = AppError.from(e)) {
                AppError.SearchUnavailable, AppError.TemporarilyUnavailable -> SearchPhase.Unavailable
                else -> SearchPhase.Failed(appError)
            }
        }
    }

    /** 同じ条件が通信中、または直前に完了したばかりか（取り消してもサーバー側の処理と回数制限は消費され得る） */
    private val isDuplicateSearch: Boolean
        get() = when (searchPhase) {
            SearchPhase.Searching -> true
            SearchPhase.Results, SearchPhase.Empty -> {
                val completedAt = lastSearchCompletedAt
                completedAt != null && Duration.between(completedAt, now()) < DUPLICATE_SEARCH_INTERVAL
            }
            else -> false
        }

    private suspend fun fetch(gameId: String?, cursor: String?): PostPage = PostPager.fetch(cursor) { listPosts.execute(gameId, it) }
}
