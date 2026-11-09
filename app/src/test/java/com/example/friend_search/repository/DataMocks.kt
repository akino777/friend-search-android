package com.example.friend_search.repository

import com.amplifyframework.auth.result.AuthSignInResult
import com.example.friend_search.Unimplemented
import com.example.friend_search.api.CognitoAuthApi
import com.example.friend_search.api.FriendSearchApi
import com.example.friend_search.apimodel.CreatePostRequestApiModel
import com.example.friend_search.apimodel.DisplayNameRequestApiModel
import com.example.friend_search.apimodel.ErrorResponseApiModel
import com.example.friend_search.apimodel.GameCatalogApiModel
import com.example.friend_search.apimodel.HttpErrorApiModel
import com.example.friend_search.apimodel.PostPageResponseApiModel
import com.example.friend_search.apimodel.PostResponseApiModel
import com.example.friend_search.apimodel.PostSearchRequestApiModel
import com.example.friend_search.apimodel.PostSearchResponseApiModel
import com.example.friend_search.apimodel.ProfileResponseApiModel

// Repository 層のテストで差し込む Data（API）のモック。未設定の処理は Unimplemented を投げる。

class MockFriendSearchApi : FriendSearchApi {
    var fetchGamesHandler: suspend () -> GameCatalogApiModel = { throw Unimplemented() }
    var fetchMeHandler: suspend () -> ProfileResponseApiModel = { throw Unimplemented() }
    var bootstrapHandler: suspend (DisplayNameRequestApiModel) -> ProfileResponseApiModel = { throw Unimplemented() }
    var updateMeHandler: suspend (DisplayNameRequestApiModel) -> ProfileResponseApiModel = { throw Unimplemented() }
    var listPostsHandler: suspend (String?, String?) -> PostPageResponseApiModel = { _, _ -> throw Unimplemented() }
    var createPostHandler: suspend (CreatePostRequestApiModel) -> PostResponseApiModel = { throw Unimplemented() }
    var getPostHandler: suspend (String) -> PostResponseApiModel = { throw Unimplemented() }
    var joinPostHandler: suspend (String) -> PostResponseApiModel = { throw Unimplemented() }
    var leavePostHandler: suspend (String) -> PostResponseApiModel = { throw Unimplemented() }
    var closePostHandler: suspend (String) -> PostResponseApiModel = { throw Unimplemented() }
    var searchPostsHandler: suspend (PostSearchRequestApiModel) -> PostSearchResponseApiModel = { throw Unimplemented() }
    var myPostsHandler: suspend (String?) -> PostPageResponseApiModel = { throw Unimplemented() }
    var myParticipationsHandler: suspend (String?) -> PostPageResponseApiModel = { throw Unimplemented() }

    override suspend fun fetchGames() = fetchGamesHandler()
    override suspend fun fetchMe() = fetchMeHandler()
    override suspend fun bootstrap(body: DisplayNameRequestApiModel) = bootstrapHandler(body)
    override suspend fun updateMe(body: DisplayNameRequestApiModel) = updateMeHandler(body)
    override suspend fun listPosts(gameId: String?, cursor: String?) = listPostsHandler(gameId, cursor)
    override suspend fun createPost(body: CreatePostRequestApiModel) = createPostHandler(body)
    override suspend fun getPost(id: String) = getPostHandler(id)
    override suspend fun joinPost(id: String) = joinPostHandler(id)
    override suspend fun leavePost(id: String) = leavePostHandler(id)
    override suspend fun closePost(id: String) = closePostHandler(id)
    override suspend fun searchPosts(body: PostSearchRequestApiModel) = searchPostsHandler(body)
    override suspend fun myPosts(cursor: String?) = myPostsHandler(cursor)
    override suspend fun myParticipations(cursor: String?) = myParticipationsHandler(cursor)
}

class MockCognitoAuthApi : CognitoAuthApi {
    var isSignedInHandler: suspend () -> Boolean = { throw Unimplemented() }
    var signInHandler: suspend (String, String) -> AuthSignInResult = { _, _ -> throw Unimplemented() }
    var signOutHandler: suspend () -> Unit = { throw Unimplemented() }

    override suspend fun isSignedIn() = isSignedInHandler()
    override suspend fun accessToken(): String = throw Unimplemented()
    override suspend fun signIn(email: String, password: String) = signInHandler(email, password)
    override suspend fun signOut() = signOutHandler()
}

/** API の応答データ */
object ApiFixture {
    val author = ProfileResponseApiModel("u", "ユーザー", 1)

    fun post(id: String, count: Int = 0) = PostResponseApiModel(
        id = id, gameId = "g", body = "テスト用の募集本文です。よろしくお願いします。", vcTool = "discord",
        capacity = 3, participantCount = count, status = "open", isJoinedByMe = false, isOwnedByMe = false,
        createdAt = "2026-11-01T10:00:00Z", author = author,
    )

    fun error(status: Int, code: String, avatar: ErrorResponseApiModel.PendingAvatar? = null) =
        HttpErrorApiModel(status, ErrorResponseApiModel(ErrorResponseApiModel.Body(code, "m", avatar)))
}
