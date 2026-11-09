package com.example.friend_search.viewmodel.shared

import com.example.friend_search.model.Post
import com.example.friend_search.model.PostStatus
import com.example.friend_search.model.VCTool
import java.net.URI
import java.net.URISyntaxException

// 募集・VC ツールの表示用の値（ボタンの有効・無効は便利機能で、最終判定はサーバー）

/** 画面に表示する募集状態（満員はサーバー上の状態ではなく派生状態） */
enum class PostDisplayState(val label: String) {
    RECRUITING("募集中"),
    FULL("満員"),
    CLOSED("終了"),
}

val Post.displayState: PostDisplayState
    get() = when {
        status == PostStatus.CLOSED -> PostDisplayState.CLOSED
        participantCount >= capacity -> PostDisplayState.FULL
        else -> PostDisplayState.RECRUITING
    }

val Post.canJoin: Boolean get() = !isOwnedByMe && !isJoinedByMe && displayState == PostDisplayState.RECRUITING
val Post.canClose: Boolean get() = isOwnedByMe && status == PostStatus.OPEN

/** 参加ボタンの文言（参加できない状態ならその理由） */
val Post.joinButtonTitle: String get() = if (displayState == PostDisplayState.RECRUITING) "参加する" else displayState.label

/** 招待リンク。投稿者と参加者にだけ見せ、https 以外は開かない */
val Post.visibleInviteLink: String?
    get() {
        val link = inviteUrl ?: return null
        if (!isOwnedByMe && !isJoinedByMe) return null
        val scheme = try {
            URI(link).scheme
        } catch (_: URISyntaxException) {
            null
        }
        return if (scheme == "https") link else null
    }

/** 未参加の人に「参加すると招待リンクを見られる」と案内するか */
val Post.showsInviteLinkHint: Boolean get() = !isOwnedByMe && !isJoinedByMe && vcTool.usesInviteLink

val VCTool.displayName: String
    get() = when (this) {
        VCTool.NONE -> "VCなし"
        VCTool.DISCORD -> "Discord"
        VCTool.LINE -> "LINE"
        VCTool.IN_GAME -> "ゲーム内VC"
        VCTool.PLAYSTATION_PARTY -> "PlayStationパーティー"
        VCTool.XBOX_PARTY -> "Xboxパーティー"
        VCTool.OTHER -> "その他"
    }

/** 招待リンクを扱うツールか */
val VCTool.usesInviteLink: Boolean get() = PostInputRule.inviteHosts(this).isNotEmpty()
