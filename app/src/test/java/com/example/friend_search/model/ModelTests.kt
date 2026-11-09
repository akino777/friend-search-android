package com.example.friend_search.model

import com.amplifyframework.auth.exceptions.NotAuthorizedException
import com.amplifyframework.auth.exceptions.SessionExpiredException
import com.amplifyframework.auth.exceptions.UnknownException
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.step.AuthNextSignInStep
import com.amplifyframework.auth.result.step.AuthSignInStep
import com.example.friend_search.apimodel.ErrorResponseApiModel
import com.example.friend_search.apimodel.GameApiModel
import com.example.friend_search.apimodel.HttpErrorApiModel
import com.example.friend_search.apimodel.PostPageResponseApiModel
import com.example.friend_search.apimodel.PostResponseApiModel
import com.example.friend_search.apimodel.PostSearchRequestApiModel
import com.example.friend_search.apimodel.ProfileResponseApiModel
import com.example.friend_search.expectThrows
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.Instant
import java.util.UUID
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Model 層: APIModel / 外部のエラー → Model の変換

class PostConversionTests {
    private val json = Json { ignoreUnknownKeys = true }
    private val author = ProfileResponseApiModel("id_001", "ユーザーA", 1)

    private fun response(vcTool: String = "discord", status: String = "open", createdAt: String = "2026-11-01T10:00:00.123Z") =
        PostResponseApiModel(
            id = "post_001", gameId = "monster-hunter-wilds", body = "乙っても気にしないで素材集めしよう！", vcTool = vcTool,
            capacity = 3, participantCount = 1, status = status, isJoinedByMe = false, isOwnedByMe = false,
            createdAt = createdAt, author = author,
        )

    @Test
    fun JSONをデコードしてModelに変換する() {
        val text = """
            {"items":[{"id":"post_001","gameId":"monster-hunter-wilds","body":"本文","vcTool":"in_game","capacity":3,"participantCount":3,"status":"closed","isJoinedByMe":false,"isOwnedByMe":true,"createdAt":"2026-11-01T10:00:00Z","author":{"publicUserId":"id_001","displayName":"ユーザーA","avatarVersion":1},"inviteUrl":"https://discord.gg/abc"}],"nextCursor":"c1"}
        """.trimIndent()
        val page = PostPage.from(json.decodeFromString<PostPageResponseApiModel>(text))
        val post = page.items.first()
        assertEquals(VCTool.IN_GAME, post.vcTool)
        assertEquals(PostStatus.CLOSED, post.status)
        assertTrue(post.isOwnedByMe)
        assertEquals(PublicProfile("id_001", "ユーザーA", 1), post.author)
        assertEquals("https://discord.gg/abc", post.inviteUrl)
        assertEquals("c1", page.nextCursor)
    }

    @Test
    fun 契約にない値は変換エラーにする() {
        expectThrows(ModelConversionError.UnknownVCTool("skype")) { Post.from(response(vcTool = "skype")) }
        expectThrows(ModelConversionError.UnknownPostStatus("draft")) { Post.from(response(status = "draft")) }
        expectThrows(ModelConversionError.InvalidDate("yesterday")) { Post.from(response(createdAt = "yesterday")) }
    }

    @Test
    fun 日時は小数秒の有無どちらも読める() {
        assertEquals(Instant.ofEpochMilli(1_793_527_200_123), Post.from(response()).createdAt)
        assertEquals(Instant.ofEpochSecond(1_793_527_200), Post.from(response(createdAt = "2026-11-01T10:00:00Z")).createdAt)
    }

    @Test
    fun 募集作成の内容をAPIの値にする() {
        val requestId = UUID.randomUUID()
        val request = NewPost("g", "b", VCTool.PLAYSTATION_PARTY, 2).toApiModel(requestId)
        val encoded = Json.parseToJsonElement(Json.encodeToString(request)).jsonObject
        assertEquals("playstation_party", encoded.getValue("vcTool").jsonPrimitive.content)
        assertEquals(requestId.toString().lowercase(), encoded.getValue("clientRequestId").jsonPrimitive.content)
        assertFalse("inviteUrl" in encoded)
        assertEquals(
            setOf("none", "discord", "line", "in_game", "playstation_party", "xbox_party", "other"),
            VCTool.entries.map(VCTool::rawValue).toSet(),
        )
    }

    @Test
    fun 全ゲームのAI検索はgameIdを送らない() {
        val encoded = Json.parseToJsonElement(Json.encodeToString(PostSearchRequestApiModel(gameId = null, query = "まったり"))).jsonObject
        assertEquals(setOf("query"), encoded.keys)
    }
}

class GameAndProfileConversionTests {
    @Test
    fun ゲームを変換する() {
        val apiModel = GameApiModel("g", "ゲーム", listOf("略称"), active = false)
        assertEquals(Game("g", "ゲーム", listOf("略称"), isActive = false), Game.from(apiModel))
    }

    @Test
    fun プロフィール未作成の404だけを未作成として扱う() {
        val pending = ErrorResponseApiModel.PendingAvatar("u1", 2)
        val notFound = HttpErrorApiModel(404, ErrorResponseApiModel(ErrorResponseApiModel.Body("PROFILE_NOT_FOUND", "m", pending)))
        assertEquals(ProfileLookup.NotCreated(AvatarIdentity("u1", 2)), ProfileLookup.fromNotFoundError(notFound))
        assertNull(ProfileLookup.fromNotFoundError(HttpErrorApiModel(404, null)))
        assertNull(ProfileLookup.fromNotFoundError(SocketTimeoutException()))
    }
}

class AuthResultConversionTests {
    @Test
    fun サインインの次の手順() {
        fun result(step: AuthSignInStep) = AuthSignInResult(step == AuthSignInStep.DONE, AuthNextSignInStep(step, emptyMap(), null, null, null, null))
        assertEquals(SignInOutcome.SIGNED_IN, SignInOutcome.from(result(AuthSignInStep.DONE)))
        assertEquals(SignInOutcome.UNSUPPORTED_STEP, SignInOutcome.from(result(AuthSignInStep.CONFIRM_SIGN_IN_WITH_TOTP_CODE)))
    }
}

class AppErrorConversionTests {
    private fun http(status: Int, code: String? = null) =
        HttpErrorApiModel(status, code?.let { ErrorResponseApiModel(ErrorResponseApiModel.Body(it, "m")) })

    @Test
    fun エラーコードで分類する() {
        assertEquals(AppError.PostFull, AppError.from(http(409, "POST_FULL")))
        assertEquals(AppError.PostClosed, AppError.from(http(409, "POST_CLOSED")))
        assertEquals(AppError.SearchUnavailable, AppError.from(http(503, "SEARCH_UNAVAILABLE")))
        assertEquals(AppError.Forbidden("m"), AppError.from(http(403, "FORBIDDEN")))
        assertEquals(AppError.Server(500, "m"), AppError.from(http(500, "INTERNAL")))
    }

    @Test
    fun 共通形式の本文がなければHTTPステータスで分類する() {
        assertEquals(AppError.Unauthorized, AppError.from(http(401)))
        assertEquals(AppError.RateLimited, AppError.from(http(429)))
        assertEquals(AppError.TemporarilyUnavailable, AppError.from(http(503)))
        assertEquals(AppError.Server(502, null), AppError.from(http(502)))
    }

    @Test
    fun 通信障害と契約違反() {
        assertEquals(AppError.Network, AppError.from(IOException("offline")))
        assertEquals(AppError.InvalidResponse, AppError.from(ModelConversionError.UnknownVCTool("x")))
        assertEquals(AppError.InvalidResponse, AppError.from(SerializationException("broken")))
        assertEquals(AppError.Unexpected, AppError.from(IllegalStateException()))
    }

    @Test
    fun 認証エラーを分類する() {
        assertEquals(AppError.Unauthorized, AppError.from(SessionExpiredException()))
        assertEquals(AppError.Auth(AuthFailure.InvalidCredentials), AppError.from(NotAuthorizedException()))
        assertEquals(AppError.Network, AppError.from(UnknownException(cause = SocketTimeoutException())))
    }
}
