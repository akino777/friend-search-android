package com.example.friend_search.viewmodel

import com.example.friend_search.Fixture
import com.example.friend_search.Gate
import com.example.friend_search.MainDispatcherRule
import com.example.friend_search.model.AppError
import com.example.friend_search.model.AvatarIdentity
import com.example.friend_search.model.Game
import com.example.friend_search.model.NewPost
import com.example.friend_search.model.Post
import com.example.friend_search.model.ProfileLookup
import com.example.friend_search.model.VCTool
import com.example.friend_search.viewmodel.game.GameCatalogStore
import com.example.friend_search.viewmodel.game.GamePickerViewModel
import com.example.friend_search.viewmodel.post.CreatePostViewModel
import com.example.friend_search.viewmodel.post.PostDetailViewModel
import com.example.friend_search.viewmodel.post.PostListViewModel
import com.example.friend_search.viewmodel.profile.MyPageViewModel
import com.example.friend_search.viewmodel.root.RootViewModel
import com.example.friend_search.viewmodel.shared.ListPhase
import com.example.friend_search.viewmodel.shared.PostInputRule
import com.example.friend_search.viewmodel.shared.userMessage
import java.time.Instant
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

// ViewModel 層: 画面状態・多重実行の抑止・古い結果の破棄。依存は UseCase のモックのみ。
// 「通信中」「後から応答が届く」状況は Gate で作り、時間待ちには頼らない。

class CreatePostViewModelTests {
    private val useCase = MockCreatePostUseCase()
    private val game = Game("monster-hunter-wilds", "モンハンワイルズ", emptyList(), true)
    private val body = "今夜、初心者同士でまったり狩りたい。ミスしてもOKな人！"

    private fun make() = CreatePostViewModel(useCase).also {
        it.selectedGame = game
        it.body = body
    }

    @Test
    fun 入力を整えた募集内容で送信する() = runTest {
        useCase.handler = { _, id -> Fixture.post(id.toString()) }
        val viewModel = make()
        viewModel.body = "  $body\n"
        viewModel.vcTool = VCTool.DISCORD
        viewModel.inviteUrl = " https://discord.gg/abc "
        viewModel.updateCapacityText("3")
        assertNotNull(viewModel.submit())
        assertEquals(NewPost(game.id, body, VCTool.DISCORD, 3, "https://discord.gg/abc"), useCase.received.first().first)
    }

    @Test
    fun 結果不明の再送は同じclientRequestIdを使い内容を変えたら新しいIDにする() = runTest {
        useCase.handler = { _, _ -> throw AppError.Network }
        val viewModel = make()
        viewModel.submit()
        viewModel.submit()
        viewModel.updateCapacityText("4")
        viewModel.submit()
        val ids = useCase.received.map { it.second }
        assertEquals(3, ids.size)
        assertEquals(ids[0], ids[1])
        assertNotEquals(ids[1], ids[2])
    }

    @Test
    fun 投稿中の多重タップは送信しない() = runTest {
        val gate = Gate()
        useCase.handler = { _, id ->
            gate.pass()
            Fixture.post(id.toString())
        }
        val viewModel = make()
        val first = launch { viewModel.submit() }
        gate.waitForArrival()
        assertNull(viewModel.submit())
        gate.open()
        first.join()
        assertEquals(1, useCase.received.size)
    }

    @Test
    fun 投稿に成功したら入力を初期状態に戻す() = runTest {
        useCase.handler = { _, id -> Fixture.post(id.toString()) }
        val viewModel = make()
        viewModel.vcTool = VCTool.LINE
        viewModel.inviteUrl = "https://line.me/abc"
        viewModel.updateCapacityText("5")
        assertNotNull(viewModel.submit())
        assertEquals("", viewModel.body)
        assertEquals(VCTool.NONE, viewModel.vcTool)
        assertEquals("", viewModel.inviteUrl)
        assertEquals(PostInputRule.DEFAULT_CAPACITY.toString(), viewModel.capacityText)
    }

    @Test
    fun ゲーム未選択は送信しない() = runTest {
        val viewModel = make()
        viewModel.selectedGame = null
        assertFalse(viewModel.canSubmit)
        assertNull(viewModel.submit())
        assertEquals(PostInputRule.GAME_MISSING_MESSAGE, viewModel.errorMessage)
        assertTrue(useCase.received.isEmpty())
    }

    @Test
    fun 本文の文字数が範囲外なら送信しない() = runTest {
        for (text in listOf(" \n ", "あ".repeat(1001))) {
            val viewModel = make()
            viewModel.body = text
            assertFalse(viewModel.isBodyCountValid)
            assertNull(viewModel.submit())
            assertEquals(PostInputRule.BODY_MESSAGE, viewModel.errorMessage)
        }
    }

    @Test
    fun 本文は前後の空白を除いて1文字から送信できる() = runTest {
        useCase.handler = { _, id -> Fixture.post(id.toString()) }
        val viewModel = make()
        viewModel.body = " あ "
        assertTrue(viewModel.canSubmit)
        assertNotNull(viewModel.submit())
        assertEquals("あ", useCase.received.first().first.body)
    }

    @Test
    fun 本文の文字数はサロゲートペアを1文字として数える() {
        val viewModel = make()
        viewModel.body = "🎮".repeat(1000)
        assertEquals(1000, viewModel.bodyCount)
        assertTrue(viewModel.isBodyCountValid)
    }

    @Test
    fun ツールの公式ホストのHTTPS以外の招待リンクは送信しない() = runTest {
        for (link in listOf("http://discord.gg/abc", "https://evil.example.com/abc", "https://user@discord.gg/abc", "https://discord.gg:8443/abc", "")) {
            val viewModel = make()
            viewModel.vcTool = VCTool.DISCORD
            viewModel.inviteUrl = link
            assertNull(viewModel.submit())
            assertEquals(PostInputRule.INVITE_LINK_MESSAGE, viewModel.errorMessage)
        }
        assertTrue(useCase.received.isEmpty())
    }

    @Test
    fun 招待リンクを扱わないツールではリンクを送らない() = runTest {
        useCase.handler = { _, id -> Fixture.post(id.toString()) }
        val viewModel = make()
        viewModel.vcTool = VCTool.IN_GAME
        viewModel.inviteUrl = "https://discord.gg/abc"
        assertNotNull(viewModel.submit())
        assertNull(useCase.received.first().first.inviteUrl)
    }

    @Test
    fun 募集人数の入力欄は半角数字2桁だけ残す() {
        val viewModel = make()
        viewModel.updateCapacityText("1a2b3")
        assertEquals("12", viewModel.capacityText)
        assertNull(viewModel.capacity)
        viewModel.updateCapacityText("１")
        assertTrue(viewModel.capacityText.isEmpty())
    }
}

class PostDetailViewModelTests {
    private val getPost = MockGetPostUseCase()
    private val join = MockPostActionUseCase()

    private fun make(initialPost: Post? = Fixture.post("p"), onUpdate: (Post) -> Unit = {}) = PostDetailViewModel(
        "p", initialPost, getPost, join, MockPostActionUseCase(), MockPostActionUseCase(), onUpdate,
    )

    @Test
    fun 満員の409は説明を出して最新状態を取り直す() = runTest {
        join.handler = { throw AppError.PostFull }
        getPost.handler = { Fixture.post("p", count = 3) }
        val viewModel = make()
        viewModel.join()
        assertEquals(AppError.PostFull.userMessage, viewModel.actionMessage)
        assertEquals(3, viewModel.post?.participantCount)
        assertEquals(1, getPost.calls)
    }

    @Test
    fun 参加成功で人数と参加状態を反映し呼び出し元へ伝える() = runTest {
        join.handler = { Fixture.post("p", count = 1, joined = true) }
        var updated: Post? = null
        val viewModel = make(onUpdate = { updated = it })
        viewModel.join()
        assertEquals(true, viewModel.post?.isJoinedByMe)
        assertEquals(1, viewModel.post?.participantCount)
        assertNull(viewModel.actionMessage)
        assertEquals(true, updated?.isJoinedByMe)
    }

    @Test
    fun 存在しない募集は画面を閉じる() = runTest {
        getPost.handler = { throw AppError.NotFound }
        val viewModel = make(initialPost = null)
        viewModel.load()
        assertTrue(viewModel.shouldDismiss)
    }

    @Test
    fun 参加後に古い詳細の取得結果で上書きしない() = runTest {
        val gate = Gate()
        getPost.handler = {
            gate.pass()
            Fixture.post("p")
        }
        join.handler = { Fixture.post("p", count = 1, joined = true) }
        val viewModel = make()
        val loading = launch { viewModel.load() }
        gate.waitForArrival()
        viewModel.join()
        gate.open()
        loading.join()
        assertEquals(true, viewModel.post?.isJoinedByMe)
    }

    @Test
    fun 重なった取得では後から始めた結果を残す() = runTest {
        val gate = Gate()
        var calls = 0
        getPost.handler = {
            calls += 1
            if (calls == 1) {
                gate.pass()
                Fixture.post("p", count = 0)
            } else {
                Fixture.post("p", count = 2)
            }
        }
        val viewModel = make()
        val older = launch { viewModel.load() }
        gate.waitForArrival()
        viewModel.load()
        gate.open()
        older.join()
        assertEquals(2, viewModel.post?.participantCount)
    }
}

class RootViewModelTests {
    private val restoreSession = MockRestoreSessionUseCase()
    private val getProfile = MockGetProfileUseCase()

    private fun make() = RootViewModel(restoreSession, getProfile, MockSignOutUseCase())

    @Test
    fun 未作成なら表示名設定へ() = runTest {
        restoreSession.handler = { true }
        getProfile.handler = { ProfileLookup.NotCreated() }
        val viewModel = make()
        viewModel.start()
        assertEquals(RootViewModel.State.NeedsProfile(null), viewModel.state)
    }

    @Test
    fun 未作成でもサーバーが返したアイコン情報を表示名設定へ渡す() = runTest {
        val avatar = AvatarIdentity("u1", 1)
        restoreSession.handler = { true }
        getProfile.handler = { ProfileLookup.NotCreated(avatar) }
        val viewModel = make()
        viewModel.start()
        assertEquals(RootViewModel.State.NeedsProfile(avatar), viewModel.state)
    }

    @Test
    fun プロフィール取得の通信障害は未作成と区別する() = runTest {
        restoreSession.handler = { true }
        getProfile.handler = { throw AppError.Network }
        val viewModel = make()
        viewModel.start()
        assertEquals(RootViewModel.State.ProfileLoadFailed(AppError.Network), viewModel.state)
    }

    @Test
    fun 未ログインはログイン画面() = runTest {
        restoreSession.handler = { false }
        val viewModel = make()
        viewModel.start()
        assertEquals(RootViewModel.State.SignedOut(null), viewModel.state)
    }

    @Test
    fun 起動時の通信障害はログイン画面へ戻さず再試行で復帰できる() = runTest {
        restoreSession.handler = { throw AppError.Network }
        getProfile.handler = { ProfileLookup.NotCreated() }
        val viewModel = make()
        viewModel.start()
        assertEquals(RootViewModel.State.ProfileLoadFailed(AppError.Network), viewModel.state)
        viewModel.loadProfile()
        assertEquals(RootViewModel.State.NeedsProfile(null), viewModel.state)
    }

    @Test
    fun 起動時の通信障害以外のエラーはログイン画面() = runTest {
        restoreSession.handler = { throw AppError.Unauthorized }
        val viewModel = make()
        viewModel.start()
        assertEquals(RootViewModel.State.SignedOut(null), viewModel.state)
    }
}

class MyPageViewModelTests {
    private val listMyPosts = MockListMyPostsUseCase()

    private fun make() = MyPageViewModel(Fixture.author, listMyPosts, MockUpdateProfileUseCase())

    private val MyPageViewModel.ids get() = visiblePosts.map(Post::id)

    @Test
    fun 自分の募集を重複なく次ページまで表示する() = runTest {
        listMyPosts.myPostsHandler = { if (it == null) Fixture.page(listOf("a", "b"), next = "c1") else Fixture.page(listOf("b", "c")) }
        val viewModel = make()
        viewModel.reload()
        viewModel.loadMoreIfNeeded(viewModel.visiblePosts[0])
        assertEquals(listOf("a", "b"), viewModel.ids)
        viewModel.loadMoreIfNeeded(viewModel.visiblePosts[1])
        assertEquals(listOf("a", "b", "c"), viewModel.ids)
    }

    @Test
    fun 参加中も独立したカーソルで次ページを読む() = runTest {
        listMyPosts.myPostsHandler = { Fixture.page(listOf("mine")) }
        listMyPosts.participationsHandler = { if (it == null) Fixture.page(listOf("j1"), next = "c1") else Fixture.page(listOf("j2")) }
        val viewModel = make()
        viewModel.reload()
        viewModel.segment = MyPageViewModel.Segment.PARTICIPATIONS
        viewModel.loadMoreIfNeeded(viewModel.visiblePosts[0])
        assertEquals(listOf("j1", "j2"), viewModel.ids)
        viewModel.segment = MyPageViewModel.Segment.MY_POSTS
        assertEquals(listOf("mine"), viewModel.ids)
    }

    @Test
    fun 空ページでも続きがあれば先へ進む() = runTest {
        listMyPosts.myPostsHandler = { cursor ->
            when (cursor) {
                null -> Fixture.page(emptyList(), next = "c1")
                "c1" -> Fixture.page(listOf("a"), next = "c2")
                "c2" -> Fixture.page(emptyList(), next = "c3")
                else -> Fixture.page(listOf("b"))
            }
        }
        val viewModel = make()
        viewModel.reload()
        assertEquals(ListPhase.Loaded, viewModel.phase)
        viewModel.loadMore()
        assertEquals(listOf("a", "b"), viewModel.ids)
    }

    @Test
    fun 追加読み込みの失敗は一覧を保持して再試行できる() = runTest {
        var fails = true
        listMyPosts.myPostsHandler = { cursor ->
            when {
                cursor == null -> Fixture.page(listOf("a"), next = "c1")
                fails -> throw AppError.Network
                else -> Fixture.page(listOf("b"))
            }
        }
        val viewModel = make()
        viewModel.reload()
        viewModel.loadMoreIfNeeded(viewModel.visiblePosts[0])
        assertEquals(AppError.Network, viewModel.loadMoreError)
        assertEquals(listOf("a"), viewModel.ids)
        fails = false
        viewModel.loadMore()
        assertNull(viewModel.loadMoreError)
        assertEquals(listOf("a", "b"), viewModel.ids)
    }

    @Test
    fun 表示のたびに先頭から取り直す() = runTest {
        val requested = mutableListOf<String?>()
        listMyPosts.myPostsHandler = { cursor ->
            requested += cursor
            if (cursor == null) Fixture.page(listOf("a"), next = "c1") else Fixture.page(listOf("b"))
        }
        val viewModel = make()
        viewModel.reload()
        viewModel.loadMore()
        viewModel.reload()
        assertEquals(listOf("a"), viewModel.ids)
        assertEquals(listOf(null, "c1", null), requested)
    }
}

class PostListViewModelTests {
    private val listUseCase = MockListPostsUseCase()
    private val searchUseCase = MockSearchPostsUseCase()

    private fun make(now: () -> Instant = Instant::now) = PostListViewModel(listUseCase, searchUseCase, now)

    private fun request(gameId: String?, query: String, vcTool: VCTool?) = MockSearchPostsUseCase.Request(gameId, query, vcTool)

    // MARK: 新着

    @Test
    fun 最初は全ゲームの新着を表示し末尾で続きを読み込む() = runTest {
        listUseCase.handler = { if (it == null) Fixture.page(listOf("p1"), next = "c1") else Fixture.page(listOf("p2")) }
        val viewModel = make()
        assertNull(viewModel.selectedGameId)
        viewModel.reload()
        viewModel.loadMoreIfNeeded(viewModel.posts[0])
        assertEquals(listOf("p1", "p2"), viewModel.posts.map(Post::id))
        assertEquals(listOf(null, null), listUseCase.requestedGameIds)
    }

    @Test
    fun ゲームで絞り込み解除すると全ゲームに戻る() = runTest {
        listUseCase.handler = { Fixture.page(listOf("p")) }
        val viewModel = make()
        viewModel.reload()
        viewModel.select("apex-legends")
        assertEquals("apex-legends", viewModel.selectedGameId)
        viewModel.select(null)
        assertNull(viewModel.selectedGameId)
        assertEquals(listOf(null, "apex-legends", null), listUseCase.requestedGameIds)
    }

    @Test
    fun 更新後に古い次ページの結果を混ぜない() = runTest {
        val gate = Gate()
        var refreshed = false
        listUseCase.handler = { cursor ->
            if (cursor != null) {
                gate.pass()
                Fixture.page(listOf("old"), next = "c-old")
            } else if (refreshed) {
                Fixture.page(listOf("fresh"))
            } else {
                Fixture.page(listOf("first"), next = "c1")
            }
        }
        val viewModel = make()
        viewModel.reload()
        val more = launch { viewModel.loadMoreIfNeeded(viewModel.posts[0]) }
        gate.waitForArrival()
        refreshed = true
        viewModel.reload()
        gate.open()
        more.join()
        assertEquals(listOf("fresh"), viewModel.posts.map(Post::id))
    }

    // MARK: AI 検索

    @Test
    fun ゲーム未選択なら全ゲームを検索して新着の代わりに結果を出す() = runTest {
        listUseCase.handler = { Fixture.page(listOf("p")) }
        searchUseCase.handler = { listOf(Fixture.post("s1"), Fixture.post("s2")) }
        val viewModel = make()
        viewModel.reload()
        assertFalse(viewModel.isSearchActive)
        viewModel.search("まったり", VCTool.DISCORD)
        assertTrue(viewModel.isSearchActive)
        assertEquals(PostListViewModel.SearchPhase.Results, viewModel.searchPhase)
        assertEquals(listOf("s1", "s2"), viewModel.searchResults.map(Post::id))
        assertEquals(listOf(request(null, "まったり", VCTool.DISCORD)), searchUseCase.requests)
    }

    @Test
    fun 絞り込み中のゲームを検索し検索文を整えて送り短すぎる検索文は送らない() = runTest {
        listUseCase.handler = { Fixture.page(emptyList()) }
        searchUseCase.handler = { emptyList() }
        val viewModel = make()
        viewModel.select("apex-legends")
        viewModel.search("  まったり  ", null)
        viewModel.search(" あ ", null)
        assertEquals(listOf(request("apex-legends", "まったり", null)), searchUseCase.requests)
        assertEquals(PostListViewModel.SearchPhase.Empty, viewModel.searchPhase)
    }

    @Test
    fun 検索結果の表示中にゲームを変えると同じ検索文とVCで選んだゲームを検索し直す() = runTest {
        listUseCase.handler = { Fixture.page(listOf("p")) }
        searchUseCase.handler = { listOf(Fixture.post("s")) }
        val viewModel = make()
        viewModel.search("まったり", VCTool.DISCORD)
        viewModel.select("apex-legends")
        viewModel.select(null)
        assertEquals(
            listOf(
                request(null, "まったり", VCTool.DISCORD),
                request("apex-legends", "まったり", VCTool.DISCORD),
                request(null, "まったり", VCTool.DISCORD),
            ),
            searchUseCase.requests,
        )
        // 解除したときに選んだゲームの新着が出るよう、新着も取り直しておく
        assertEquals(listOf("apex-legends", null), listUseCase.requestedGameIds)
        assertTrue(viewModel.isSearchActive)
    }

    @Test
    fun 検索していなければゲームを変えても検索しない() = runTest {
        listUseCase.handler = { Fixture.page(listOf("p")) }
        val viewModel = make()
        viewModel.select("apex-legends")
        assertEquals(0, searchUseCase.calls)
        assertFalse(viewModel.isSearchActive)
    }

    @Test
    fun 検索を解除すると新着に戻り通信中だった検索の結果を出さない() = runTest {
        val gate = Gate()
        searchUseCase.handler = {
            gate.pass()
            listOf(Fixture.post("s"))
        }
        val viewModel = make()
        val searching = launch { viewModel.search("まったり", null) }
        gate.waitForArrival()
        assertEquals(PostListViewModel.SearchPhase.Searching, viewModel.searchPhase)
        viewModel.clearSearch()
        gate.open()
        searching.join()
        assertFalse(viewModel.isSearchActive)
        assertTrue(viewModel.searchResults.isEmpty())
    }

    @Test
    fun 引っ張って更新は検索結果の表示中なら同じ条件で検索し直し新着は取り直さない() = runTest {
        listUseCase.handler = { Fixture.page(listOf("p")) }
        searchUseCase.handler = { listOf(Fixture.post("s")) }
        var now = Instant.EPOCH
        val viewModel = make(now = { now })
        viewModel.search("まったり", VCTool.DISCORD)
        now = now.plus(PostListViewModel.DUPLICATE_SEARCH_INTERVAL)
        viewModel.refresh()
        val same = request(null, "まったり", VCTool.DISCORD)
        assertEquals(listOf(same, same), searchUseCase.requests)
        assertTrue(listUseCase.requestedGameIds.isEmpty())

        viewModel.clearSearch()
        viewModel.refresh()
        assertEquals(listOf<String?>(null), listUseCase.requestedGameIds)
        assertEquals(2, searchUseCase.calls)
    }

    @Test
    fun 後から届いた古い検索結果で上書きしない() = runTest {
        val gate = Gate()
        searchUseCase.handler = { query ->
            if (query == "最初の検索") {
                gate.pass()
                listOf(Fixture.post("old"))
            } else {
                listOf(Fixture.post("new"))
            }
        }
        val viewModel = make()
        val first = launch { viewModel.search("最初の検索", null) }
        gate.waitForArrival()
        viewModel.search("二回目の検索", null)
        gate.open()
        first.join()
        assertEquals(listOf("new"), viewModel.searchResults.map(Post::id))
        assertEquals(PostListViewModel.SearchPhase.Results, viewModel.searchPhase)
    }

    @Test
    fun 同じ条件の検索は通信中に送り直さず別条件なら送る() = runTest {
        val gate = Gate()
        searchUseCase.handler = { query ->
            if (query == "のんびり遊びたい") gate.pass()
            emptyList()
        }
        val viewModel = make()
        val first = launch { viewModel.search("のんびり遊びたい", null) }
        gate.waitForArrival()
        viewModel.search("のんびり遊びたい", null)
        assertEquals(1, searchUseCase.calls)
        viewModel.search("ガチで周回したい", null)
        assertEquals(2, searchUseCase.calls)
        gate.open()
        first.join()
    }

    @Test
    fun 同じ条件の短時間の再検索は抑止し時間が経てば送る() = runTest {
        searchUseCase.handler = { emptyList() }
        var now = Instant.EPOCH
        val viewModel = make(now = { now })
        viewModel.search("まったり", null)
        viewModel.search("まったり", null)
        assertEquals(1, searchUseCase.calls)
        now = now.plus(PostListViewModel.DUPLICATE_SEARCH_INTERVAL)
        viewModel.search("まったり", null)
        assertEquals(2, searchUseCase.calls)
    }

    @Test
    fun 検索中に取り消されたら検索中のまま止まらず検索を解除した状態に戻る() = runTest {
        val gate = Gate()
        searchUseCase.handler = {
            gate.pass()
            coroutineContext.ensureActive()
            emptyList()
        }
        val viewModel = make()
        val searching = launch { viewModel.search("まったり", null) }
        gate.waitForArrival()
        assertEquals(PostListViewModel.SearchPhase.Searching, viewModel.searchPhase)
        searching.cancel()
        gate.open()
        searching.join()
        assertFalse(viewModel.isSearchActive)

        searchUseCase.handler = { listOf(Fixture.post("a")) }
        viewModel.search("まったり", null)
        assertEquals(PostListViewModel.SearchPhase.Results, viewModel.searchPhase)
    }

    @Test
    fun AI検索が使えなければ理由を出し引っ張って更新で検索し直せる() = runTest {
        searchUseCase.handler = { throw AppError.SearchUnavailable }
        val viewModel = make()
        viewModel.search("まったり", null)
        assertEquals(PostListViewModel.SearchPhase.Unavailable, viewModel.searchPhase)
        assertEquals(AppError.SearchUnavailable.userMessage, viewModel.searchErrorMessage)

        searchUseCase.handler = { listOf(Fixture.post("a")) }
        viewModel.refresh()
        assertEquals(PostListViewModel.SearchPhase.Results, viewModel.searchPhase)
        assertNull(viewModel.searchErrorMessage)
    }

    @Test
    fun 詳細で更新された募集を検索結果に反映する() = runTest {
        searchUseCase.handler = { listOf(Fixture.post("a"), Fixture.post("b")) }
        val viewModel = make()
        viewModel.search("まったり", null)
        viewModel.applyToSearchResults(Fixture.post("b", count = 1, joined = true))
        assertEquals(listOf(false, true), viewModel.searchResults.map(Post::isJoinedByMe))
    }
}

class GameCatalogStoreTests {
    /** GameCatalogStore は取得を viewModelScope（Main）で行う */
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val getGames = MockGetGamesUseCase()
    private val games = listOf(Game("monster-hunter-wilds", "モンスターハンターワイルズ", listOf("モンハン"), true))

    @Test
    fun 取得に失敗したらエラーを出し次の表示で取り直す() = runTest {
        getGames.executeHandler = { throw AppError.Network }
        val store = GameCatalogStore(getGames)
        store.loadIfNeeded()
        assertNull(store.games)
        assertEquals(AppError.Network, store.error)
        getGames.executeHandler = { games }
        store.loadIfNeeded()
        assertEquals(games, store.games)
        assertNull(store.error)
    }

    @Test
    fun 取得中に呼ばれたら新しく取得せず完了を待つ() = runTest {
        val gate = Gate()
        var calls = 0
        getGames.executeHandler = {
            calls += 1
            gate.pass()
            games
        }
        val store = GameCatalogStore(getGames)
        val first = launch { store.refresh() }
        gate.waitForArrival()
        assertTrue(store.isLoading)
        val second = launch { store.refresh() }
        gate.open()
        second.join()
        assertEquals(games, store.games)
        first.join()
        assertEquals(1, calls)
        assertFalse(store.isLoading)
    }

    @Test
    fun 入力に合う候補が0件なら見つからないことを出す() = runTest {
        getGames.executeHandler = { games }
        val viewModel = GamePickerViewModel(GameCatalogStore(getGames))
        viewModel.load()
        viewModel.query = "存在しないゲーム"
        assertTrue(viewModel.showsNoMatch)
        viewModel.query = "モンハン"
        assertFalse(viewModel.showsNoMatch)
    }
}
