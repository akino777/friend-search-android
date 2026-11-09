package com.example.friend_search.model

import com.example.friend_search.apimodel.ErrorResponseApiModel
import com.example.friend_search.apimodel.HttpErrorApiModel
import com.example.friend_search.apimodel.ProfileResponseApiModel

/** 公開プロフィール（他ユーザーにも表示される情報のみ） */
data class PublicProfile(
    /** 認証用 ID とは別の乱数 UUID */
    val publicUserId: String,
    val displayName: String,
    val avatarVersion: Int,
) {
    // MARK: - APIModel ⇄ Model

    companion object {
        fun from(apiModel: ProfileResponseApiModel) = PublicProfile(apiModel.publicUserId, apiModel.displayName, apiModel.avatarVersion)
    }
}

/** アイコンの元になる情報（表示名が決まる前でも扱える） */
data class AvatarIdentity(
    val publicUserId: String,
    val avatarVersion: Int,
) {
    companion object {
        fun from(apiModel: ErrorResponseApiModel.PendingAvatar) = AvatarIdentity(apiModel.publicUserId, apiModel.avatarVersion)
    }
}

/** 自分のプロフィールの取得結果 */
sealed interface ProfileLookup {
    data class Found(val profile: PublicProfile) : ProfileLookup

    /** 未作成。avatar はサーバーが返す、作成後と同じアイコンの情報 */
    data class NotCreated(val avatar: AvatarIdentity? = null) : ProfileLookup

    companion object {
        /** エラー応答が「プロフィール未作成」（404 PROFILE_NOT_FOUND）なら NotCreated。それ以外は null */
        fun fromNotFoundError(error: Throwable): ProfileLookup? {
            val http = error as? HttpErrorApiModel ?: return null
            val body = http.body?.error
            if (http.statusCode != 404 || body?.code != "PROFILE_NOT_FOUND") return null
            return NotCreated(body.pendingAvatar?.let(AvatarIdentity::from))
        }
    }
}
