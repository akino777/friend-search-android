package com.example.friend_search.viewmodel

import com.example.friend_search.Fixture
import com.example.friend_search.model.AppError
import com.example.friend_search.model.AuthFailure
import com.example.friend_search.model.Game
import com.example.friend_search.model.Post
import com.example.friend_search.model.PostStatus
import com.example.friend_search.model.VCTool
import com.example.friend_search.viewmodel.game.GameSearchIndex
import com.example.friend_search.viewmodel.shared.IdenticonPalette
import com.example.friend_search.viewmodel.shared.IdenticonPattern
import com.example.friend_search.viewmodel.shared.PostDisplayState
import com.example.friend_search.viewmodel.shared.canClose
import com.example.friend_search.viewmodel.shared.canJoin
import com.example.friend_search.viewmodel.shared.displayState
import com.example.friend_search.viewmodel.shared.joinButtonTitle
import com.example.friend_search.viewmodel.shared.showsInviteLinkHint
import com.example.friend_search.viewmodel.shared.userMessage
import com.example.friend_search.viewmodel.shared.usesInviteLink
import com.example.friend_search.viewmodel.shared.visibleInviteLink
import java.time.Instant
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// ViewModel 層の表示用の処理: ゲーム候補検索・Identicon・募集の表示状態・エラーの文言

class GameSearchIndexTests {
    private val index = GameSearchIndex(
        listOf(
            Game("monster-hunter-wilds", "モンスターハンターワイルズ", listOf("Monster Hunter Wilds", "モンハン", "MHWs"), true),
            Game("apex-legends", "Apex Legends", listOf("エーペックス"), true),
            Game("hunter-x", "Hunter X Quest", emptyList(), true),
            Game("retired", "モンスター旧作", emptyList(), false),
        ),
    )

    private fun ids(query: String) = index.search(query).map(Game::id)

    @Test
    fun 別名と大文字小文字を吸収する() {
        assertEquals(listOf("monster-hunter-wilds"), ids("mhws"))
        assertEquals(listOf("apex-legends"), ids("APEX"))
    }

    @Test
    fun 全角英数とひらがなと空白を吸収する() {
        assertEquals(listOf("monster-hunter-wilds"), ids("ＭＨＷｓ"))
        assertEquals(listOf("monster-hunter-wilds"), ids("もんはん"))
        assertEquals(listOf("apex-legends"), ids("  えーぺっくす "))
        assertEquals(listOf("monster-hunter-wilds"), ids("monster   hunter"))
    }

    @Test
    fun 前方一致を部分一致より優先する() {
        assertEquals(listOf("hunter-x", "monster-hunter-wilds"), ids("hunter"))
    }

    @Test
    fun 無効なゲームは出さず空の入力なら有効なゲームをすべて出す() {
        assertFalse("retired" in ids("モンスター"))
        assertTrue(ids("存在しないゲーム").isEmpty())
        assertEquals(setOf("monster-hunter-wilds", "apex-legends", "hunter-x"), ids("").toSet())
    }
}

/** iOS / Android 共通の Identicon 仕様 */
class IdenticonPatternTests {
    @Test
    fun 公開IDの大文字小文字を区別しない() {
        assertEquals(
            IdenticonPattern.of("B24683EC-304D-40F6-A1D1-8C8E52A3D09D", 1),
            IdenticonPattern.of("b24683ec-304d-40f6-a1d1-8c8e52a3d09d", 1),
        )
    }

    @Test
    fun 既知の入力で期待値と一致する() {
        // iOS の IdenticonPatternTests と同じ期待値（入力 "1:b24683ec-304d-40f6-a1d1-8c8e52a3d09d" の SHA-256 から仕様どおりに求めた値）
        val pattern = IdenticonPattern.of("b24683ec-304d-40f6-a1d1-8c8e52a3d09d", 1)
        val expected = listOf(listOf(0, 1, 1, 1, 0), listOf(0, 0, 0, 0, 0), listOf(1, 0, 0, 0, 1), listOf(1, 0, 1, 0, 1), listOf(0, 1, 1, 1, 0))
        assertEquals(expected, pattern.cells.map { row -> row.map { if (it) 1 else 0 } })
        assertEquals(IdenticonPalette.all[4], pattern.palette)
        assertEquals(0xEFF6FF, pattern.palette.background)
    }

    @Test
    fun 左右対称の5x5() {
        val pattern = IdenticonPattern.of(UUID.randomUUID().toString(), 1)
        assertEquals(5, pattern.cells.size)
        for (row in pattern.cells) {
            assertEquals(5, row.size)
            assertTrue(row[0] == row[4] && row[1] == row[3])
        }
    }

    @Test
    fun 版が変われば別のパターンになる() {
        assertNotEquals(
            IdenticonPattern.of("b24683ec-304d-40f6-a1d1-8c8e52a3d09d", 1),
            IdenticonPattern.of("b24683ec-304d-40f6-a1d1-8c8e52a3d09d", 2),
        )
    }
}

class PostPresentationTests {
    private fun post(count: Int, status: PostStatus = PostStatus.OPEN, owned: Boolean = false, joined: Boolean = false, link: String? = null) = Post(
        id = "p", gameId = "g", author = Fixture.author, body = "本文", vcTool = VCTool.DISCORD, capacity = 3, participantCount = count,
        status = status, isJoinedByMe = joined, isOwnedByMe = owned, createdAt = Instant.EPOCH, inviteUrl = link,
    )

    @Test
    fun 表示状態() {
        assertEquals(PostDisplayState.RECRUITING, post(2).displayState)
        assertEquals(PostDisplayState.FULL, post(3).displayState)
        assertEquals(PostDisplayState.CLOSED, post(0, status = PostStatus.CLOSED).displayState)
    }

    @Test
    fun 参加と終了のボタン() {
        assertTrue(post(0).canJoin)
        assertFalse(post(3).canJoin)
        assertFalse(post(0, joined = true).canJoin)
        assertFalse(post(0, owned = true).canJoin)
        assertTrue(post(0, owned = true).canClose)
        assertFalse(post(0, status = PostStatus.CLOSED, owned = true).canClose)
    }

    @Test
    fun 招待リンクは投稿者と参加者にだけhttpsのものを見せる() {
        val link = "https://discord.gg/abc"
        assertEquals(link, post(0, joined = true, link = link).visibleInviteLink)
        assertNotNull(post(0, owned = true, link = link).visibleInviteLink)
        assertNull(post(0, link = link).visibleInviteLink)
        assertTrue(post(0, link = link).showsInviteLinkHint)
        assertNull(post(0, joined = true, link = "http://discord.gg/abc").visibleInviteLink)
    }

    @Test
    fun 参加ボタンの文言() {
        assertEquals("参加する", post(0).joinButtonTitle)
        assertEquals(PostDisplayState.FULL.label, post(3).joinButtonTitle)
    }

    @Test
    fun 招待リンクを扱うツール() {
        assertEquals(setOf(VCTool.DISCORD, VCTool.LINE), VCTool.entries.filter { it.usesInviteLink }.toSet())
    }
}

class ErrorMessageTests {
    @Test
    fun サーバーのメッセージがあればそれを表示する() {
        assertEquals("自分の募集には参加できません", AppError.Forbidden("自分の募集には参加できません").userMessage)
        assertEquals("この操作は許可されていません", AppError.Forbidden(null).userMessage)
        assertEquals("サーバーエラーが発生しました（500）", AppError.Server(500, null).userMessage)
    }
}
