package com.example.friend_search.model

import com.example.friend_search.apimodel.CreatePostRequestApiModel
import com.example.friend_search.apimodel.PostPageResponseApiModel
import com.example.friend_search.apimodel.PostResponseApiModel
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.UUID

/** VC ツール（単一選択）。rawValue は API 契約の値 */
enum class VCTool(val rawValue: String) {
    NONE("none"),
    DISCORD("discord"),
    LINE("line"),
    IN_GAME("in_game"),
    PLAYSTATION_PARTY("playstation_party"),
    XBOX_PARTY("xbox_party"),
    OTHER("other");

    companion object {
        fun fromRawValue(value: String): VCTool? = entries.firstOrNull { it.rawValue == value }
    }
}

/** サーバー上の募集状態 */
enum class PostStatus(val rawValue: String) {
    OPEN("open"),
    CLOSED("closed");

    companion object {
        fun fromRawValue(value: String): PostStatus? = entries.firstOrNull { it.rawValue == value }
    }
}

data class Post(
    val id: String,
    val gameId: String,
    val author: PublicProfile,
    val body: String,
    val vcTool: VCTool,
    /** 投稿者を除く募集枠 */
    val capacity: Int,
    val participantCount: Int,
    val status: PostStatus,
    val isJoinedByMe: Boolean,
    val isOwnedByMe: Boolean,
    val createdAt: Instant,
    val inviteUrl: String? = null,
) {
    // MARK: - APIModel ⇄ Model

    companion object {
        /** 不明な値を別の値に置き換えず、契約違反として扱う */
        fun from(apiModel: PostResponseApiModel): Post {
            val vcTool = VCTool.fromRawValue(apiModel.vcTool) ?: throw ModelConversionError.UnknownVCTool(apiModel.vcTool)
            val status = PostStatus.fromRawValue(apiModel.status) ?: throw ModelConversionError.UnknownPostStatus(apiModel.status)
            val createdAt = Iso8601.instant(apiModel.createdAt) ?: throw ModelConversionError.InvalidDate(apiModel.createdAt)
            return Post(
                id = apiModel.id,
                gameId = apiModel.gameId,
                author = PublicProfile.from(apiModel.author),
                body = apiModel.body,
                vcTool = vcTool,
                capacity = apiModel.capacity,
                participantCount = apiModel.participantCount,
                status = status,
                isJoinedByMe = apiModel.isJoinedByMe,
                isOwnedByMe = apiModel.isOwnedByMe,
                createdAt = createdAt,
                inviteUrl = apiModel.inviteUrl,
            )
        }
    }
}

/** カーソルページングの1ページ */
data class PostPage(
    val items: List<Post>,
    val nextCursor: String?,
) {
    companion object {
        fun from(apiModel: PostPageResponseApiModel) = PostPage(apiModel.items.map(Post::from), apiModel.nextCursor)
    }
}

/** 募集作成の内容 */
data class NewPost(
    val gameId: String,
    val body: String,
    val vcTool: VCTool,
    val capacity: Int,
    val inviteUrl: String? = null,
) {
    fun toApiModel(clientRequestId: UUID) = CreatePostRequestApiModel(
        gameId = gameId,
        body = body,
        vcTool = vcTool.rawValue,
        capacity = capacity,
        clientRequestId = clientRequestId.toString().lowercase(),
        inviteUrl = inviteUrl,
    )
}

/** API の日時文字列（小数秒の有無どちらも受け付ける。DateTimeFormatter.ISO_INSTANT） */
object Iso8601 {
    fun instant(value: String): Instant? = try {
        Instant.parse(value)
    } catch (_: DateTimeParseException) {
        null
    }
}
