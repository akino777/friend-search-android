package com.example.friend_search.viewmodel.shared

import com.example.friend_search.model.VCTool
import java.net.URI
import java.net.URISyntaxException

// 画面の入力検証の規則。最終的な判定はサーバーが行う（ここは送信前の補助）。

/** 文字数。サーバーと同じく、サロゲートペアを1文字として数える（コードポイント数） */
fun String.charCount(): Int = codePointCount(0, length)

object EmailRule {
    const val INVALID_MESSAGE = "メールアドレスの形式が正しくありません"
    private val pattern = Regex("""^[^@\s]+@[^@\s]+\.[^@\s]+$""")

    /** 前後の空白を除いて小文字にしたメールアドレス。形式が正しくなければ null */
    fun normalized(raw: String): String? {
        val email = raw.trim().lowercase()
        return if (pattern.matches(email)) email else null
    }
}

object DisplayNameRule {
    val range = 2..24
    val INVALID_MESSAGE = "表示名は${range.first}〜${range.last}文字で入力してください"

    /** 前後の空白を除いた表示名。文字数が範囲外か制御文字（Unicode の Cc・Cf）を含めば null */
    fun normalized(raw: String): String? {
        val name = raw.trim()
        if (name.charCount() !in range) return null
        val hasControl = name.codePoints().anyMatch {
            val type = Character.getType(it)
            type == Character.CONTROL.toInt() || type == Character.FORMAT.toInt()
        }
        return if (hasControl) null else name
    }
}

object PostInputRule {
    val bodyRange = 1..1000
    val capacityRange = 1..10
    const val DEFAULT_CAPACITY = 2
    val queryRange = 2..300

    const val GAME_MISSING_MESSAGE = "ゲームを選択してください"
    val BODY_MESSAGE = "募集本文は${bodyRange.first}〜${bodyRange.last}文字で入力してください"
    val CAPACITY_MESSAGE = "募集人数は${capacityRange.first}〜${capacityRange.last}の数字で入力してください"
    const val INVITE_LINK_MESSAGE = "選択したツールの HTTPS 招待リンクを入力してください"
    val QUERY_MESSAGE = "検索文は${queryRange.first}〜${queryRange.last}文字で入力してください"

    fun normalizedBody(text: String): String = text.trim()

    /** 募集人数。半角数字以外・範囲外なら null */
    fun capacity(text: String): Int? {
        val trimmed = text.trim { it == '\t' || Character.getType(it) == Character.SPACE_SEPARATOR.toInt() }
        if (trimmed.isEmpty() || !trimmed.all { it in '0'..'9' }) return null
        val value = trimmed.toIntOrNull() ?: return null
        return if (value in capacityRange) value else null
    }

    /** 前後の空白を除いた検索文。文字数が範囲外なら null */
    fun normalizedQuery(text: String): String? {
        val query = text.trim()
        return if (query.charCount() in queryRange) query else null
    }

    /** 招待リンクとして受け付けるホスト。空ならリンクを扱わないツール */
    fun inviteHosts(tool: VCTool): Set<String> = when (tool) {
        VCTool.DISCORD -> setOf("discord.gg", "discord.com")
        VCTool.LINE -> setOf("line.me")
        VCTool.NONE, VCTool.IN_GAME, VCTool.PLAYSTATION_PARTY, VCTool.XBOX_PARTY, VCTool.OTHER -> emptySet()
    }

    /** 招待リンク。https のみ・ユーザー情報やポート指定なし・ツールの公式ホストのみ受け付ける。不正なら null */
    fun inviteLink(raw: String, tool: VCTool): String? {
        val link = raw.trim()
        if (link.length > 2048) return null
        val uri = try {
            URI(link)
        } catch (_: URISyntaxException) {
            return null
        }
        if (uri.scheme?.lowercase() != "https" || uri.rawUserInfo != null || uri.port != -1) return null
        val host = uri.host?.lowercase() ?: return null
        return if (host in inviteHosts(tool)) link else null
    }
}
