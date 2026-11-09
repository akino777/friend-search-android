package com.example.friend_search.api

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
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * friend-search 業務 API（API Gateway HTTP API）。
 * 失敗は HttpErrorApiModel / IOException / SerializationException / CancellationException で返し、Retrofit の型を外へ出さない。
 */
interface FriendSearchApi {
    suspend fun fetchGames(): GameCatalogApiModel
    suspend fun fetchMe(): ProfileResponseApiModel
    suspend fun bootstrap(body: DisplayNameRequestApiModel): ProfileResponseApiModel
    suspend fun updateMe(body: DisplayNameRequestApiModel): ProfileResponseApiModel

    /** gameId が null なら全ゲームの新着 */
    suspend fun listPosts(gameId: String?, cursor: String?): PostPageResponseApiModel
    suspend fun createPost(body: CreatePostRequestApiModel): PostResponseApiModel
    suspend fun getPost(id: String): PostResponseApiModel
    suspend fun joinPost(id: String): PostResponseApiModel
    suspend fun leavePost(id: String): PostResponseApiModel
    suspend fun closePost(id: String): PostResponseApiModel
    suspend fun searchPosts(body: PostSearchRequestApiModel): PostSearchResponseApiModel
    suspend fun myPosts(cursor: String?): PostPageResponseApiModel
    suspend fun myParticipations(cursor: String?): PostPageResponseApiModel
}

/** 各リクエストに Amplify から取得した Access Token を `Authorization: Bearer` で付ける（トークンの更新は Amplify が行う） */
class DefaultFriendSearchApi(
    baseUrl: String = Endpoint.API_BASE_URL,
    private val auth: CognitoAuthApi = DefaultCognitoAuthApi(),
) : FriendSearchApi {
    companion object {
        /** 契約にない項目は読み飛ばす */
        private val json = Json { ignoreUnknownKeys = true }

        /** 接続の再利用のため、インスタンス間で共有する */
        private val client: OkHttpClient by lazy {
            OkHttpClient.Builder().readTimeout(20, TimeUnit.SECONDS).build()
        }
    }

    private val service: FriendSearchService = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(FriendSearchService::class.java)

    override suspend fun fetchGames() = send { fetchGames(it) }
    override suspend fun fetchMe() = send { fetchMe(it) }
    override suspend fun bootstrap(body: DisplayNameRequestApiModel) = send { bootstrap(it, body) }
    override suspend fun updateMe(body: DisplayNameRequestApiModel) = send { updateMe(it, body) }
    override suspend fun listPosts(gameId: String?, cursor: String?) = send { listPosts(it, gameId, cursor) }
    override suspend fun createPost(body: CreatePostRequestApiModel) = send { createPost(it, body) }
    override suspend fun getPost(id: String) = send { getPost(it, id) }
    override suspend fun joinPost(id: String) = send { joinPost(it, id) }
    override suspend fun leavePost(id: String) = send { leavePost(it, id) }
    override suspend fun closePost(id: String) = send { closePost(it, id) }
    override suspend fun searchPosts(body: PostSearchRequestApiModel) = send { searchPosts(it, body) }
    override suspend fun myPosts(cursor: String?) = send { myPosts(it, cursor) }
    override suspend fun myParticipations(cursor: String?) = send { myParticipations(it, cursor) }

    // MARK: 共通処理

    /**
     * Access Token を付けて送る。エラー応答は本文を共通形式の APIModel として読み、HttpErrorApiModel にする
     * （Retrofit は失敗した応答の本文を読み込み済みで渡すため、ここで通信は発生しない）
     */
    private suspend fun <T> send(request: suspend FriendSearchService.(authorization: String) -> T): T {
        val authorization = "Bearer ${auth.accessToken()}"
        return try {
            service.request(authorization)
        } catch (e: HttpException) {
            val body = e.response()?.errorBody()?.string()?.let {
                runCatching { json.decodeFromString<ErrorResponseApiModel>(it) }.getOrNull()
            }
            throw HttpErrorApiModel(e.code(), body)
        }
    }
}

/** Retrofit の API 宣言。null のクエリ値は送らない（Retrofit の @Query の仕様） */
internal interface FriendSearchService {
    @GET("games")
    suspend fun fetchGames(@Header("Authorization") authorization: String): GameCatalogApiModel

    @GET("me")
    suspend fun fetchMe(@Header("Authorization") authorization: String): ProfileResponseApiModel

    @POST("me/bootstrap")
    suspend fun bootstrap(@Header("Authorization") authorization: String, @Body body: DisplayNameRequestApiModel): ProfileResponseApiModel

    @PATCH("me")
    suspend fun updateMe(@Header("Authorization") authorization: String, @Body body: DisplayNameRequestApiModel): ProfileResponseApiModel

    @GET("posts")
    suspend fun listPosts(
        @Header("Authorization") authorization: String,
        @Query("gameId") gameId: String?,
        @Query("cursor") cursor: String?,
    ): PostPageResponseApiModel

    @POST("posts")
    suspend fun createPost(@Header("Authorization") authorization: String, @Body body: CreatePostRequestApiModel): PostResponseApiModel

    @GET("posts/{id}")
    suspend fun getPost(@Header("Authorization") authorization: String, @Path("id") id: String): PostResponseApiModel

    @POST("posts/{id}/join")
    suspend fun joinPost(@Header("Authorization") authorization: String, @Path("id") id: String): PostResponseApiModel

    @DELETE("posts/{id}/join")
    suspend fun leavePost(@Header("Authorization") authorization: String, @Path("id") id: String): PostResponseApiModel

    @POST("posts/{id}/close")
    suspend fun closePost(@Header("Authorization") authorization: String, @Path("id") id: String): PostResponseApiModel

    @POST("posts/search")
    suspend fun searchPosts(@Header("Authorization") authorization: String, @Body body: PostSearchRequestApiModel): PostSearchResponseApiModel

    @GET("me/posts")
    suspend fun myPosts(@Header("Authorization") authorization: String, @Query("cursor") cursor: String?): PostPageResponseApiModel

    @GET("me/participations")
    suspend fun myParticipations(@Header("Authorization") authorization: String, @Query("cursor") cursor: String?): PostPageResponseApiModel
}
