package com.example.friend_search.repository

import com.amplifyframework.auth.exceptions.SessionExpiredException
import com.example.friend_search.Fixture
import com.example.friend_search.apimodel.ErrorResponseApiModel
import com.example.friend_search.apimodel.GameApiModel
import com.example.friend_search.apimodel.GameCatalogApiModel
import com.example.friend_search.apimodel.PostPageResponseApiModel
import com.example.friend_search.apimodel.PostSearchRequestApiModel
import com.example.friend_search.apimodel.PostSearchResponseApiModel
import com.example.friend_search.expectThrows
import com.example.friend_search.model.AppError
import com.example.friend_search.model.AvatarIdentity
import com.example.friend_search.model.ProfileLookup
import com.example.friend_search.model.VCTool
import java.net.SocketTimeoutException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Repository 層: API の結果を Model と AppError へ置き換える。依存は Data のモックのみ。

class PostRepositoryTests {
    private val api = MockFriendSearchApi()
    private val repository get() = DefaultPostRepository(api)

    @Test
    fun 一覧をカーソル付きで取得しModelに変換する() = runTest {
        var received: Pair<String?, String?>? = null
        api.listPostsHandler = { gameId, cursor ->
            received = gameId to cursor
            PostPageResponseApiModel(listOf(ApiFixture.post("p1")), "c1")
        }
        val page = repository.listPosts("g", "c0")
        assertEquals("g" to "c0", received)
        assertEquals(listOf("p1"), page.items.map { it.id })
        assertEquals("c1", page.nextCursor)
    }

    @Test
    fun AI検索はゲーム未指定ならgameIdなしで送りModelに変換する() = runTest {
        var received: PostSearchRequestApiModel? = null
        api.searchPostsHandler = { body ->
            received = body
            PostSearchResponseApiModel(listOf(ApiFixture.post("p1")))
        }
        val posts = repository.search(null, "まったり", VCTool.DISCORD)
        val request = checkNotNull(received)
        assertNull(request.gameId)
        assertEquals("まったり", request.query)
        assertEquals("discord", request.vcTool)
        assertEquals(listOf("p1"), posts.map { it.id })
    }

    @Test
    fun 業務エラーをAppErrorに置き換える() = runTest {
        api.joinPostHandler = { throw ApiFixture.error(409, "POST_FULL") }
        expectThrows(AppError.PostFull) { repository.join("p1") }
    }

    @Test
    fun 契約外の応答は不正な応答として扱う() = runTest {
        api.getPostHandler = { ApiFixture.post("p1").copy(vcTool = "skype") }
        expectThrows(AppError.InvalidResponse) { repository.post("p1") }
    }
}

class ProfileRepositoryTests {
    private val api = MockFriendSearchApi()

    @Test
    fun 作成済みならプロフィールを返す() = runTest {
        api.fetchMeHandler = { ApiFixture.author }
        assertEquals(ProfileLookup.Found(Fixture.author), DefaultProfileRepository(api).myProfile())
    }

    @Test
    fun 未作成の404はアイコン情報付きの未作成にする() = runTest {
        api.fetchMeHandler = { throw ApiFixture.error(404, "PROFILE_NOT_FOUND", ErrorResponseApiModel.PendingAvatar("u1", 2)) }
        assertEquals(ProfileLookup.NotCreated(AvatarIdentity("u1", 2)), DefaultProfileRepository(api).myProfile())
    }

    @Test
    fun 未作成以外のエラーはAppErrorにする() = runTest {
        api.fetchMeHandler = { throw SocketTimeoutException() }
        expectThrows(AppError.Network) { DefaultProfileRepository(api).myProfile() }
    }
}

class GameRepositoryTests {
    private val api = MockFriendSearchApi()

    @Test
    fun マスタを取得してModelに変換する() = runTest {
        api.fetchGamesHandler = { GameCatalogApiModel(listOf(GameApiModel("g", "新作", emptyList(), true))) }
        assertEquals(listOf("g"), DefaultGameRepository(api).games().map { it.id })
    }
}

class AuthRepositoryTests {
    private val api = MockCognitoAuthApi()

    @Test
    fun ログイン切れはログインし直すエラーにする() = runTest {
        api.isSignedInHandler = { throw SessionExpiredException() }
        expectThrows(AppError.Unauthorized) { DefaultAuthRepository(api).isSignedIn() }
    }
}
