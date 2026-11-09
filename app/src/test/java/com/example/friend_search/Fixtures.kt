package com.example.friend_search

import com.example.friend_search.model.Post
import com.example.friend_search.model.PostPage
import com.example.friend_search.model.PostStatus
import com.example.friend_search.model.PublicProfile
import com.example.friend_search.model.VCTool
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** 各層のテストで共有する Model のテストデータ（Model はどの層からも参照される） */
object Fixture {
    val author = PublicProfile("u", "ユーザー", 1)

    fun post(id: String, count: Int = 0, joined: Boolean = false) = Post(
        id = id, gameId = "g", author = author,
        body = "テスト用の募集本文です。よろしくお願いします。", vcTool = VCTool.DISCORD, capacity = 3, participantCount = count,
        status = PostStatus.OPEN, isJoinedByMe = joined, isOwnedByMe = false, createdAt = Instant.EPOCH,
    )

    fun page(ids: List<String>, next: String? = null) = PostPage(ids.map { post(it) }, next)
}

/** モックで未設定の処理が呼ばれたことを示す */
class Unimplemented : Exception()

/**
 * テストで非同期処理の順番を決めるための関門。モックの中で `pass()` を呼ぶと、`open()` されるまでそこで止まる。
 * 時間待ち（sleep・delay）に頼らずに「通信中」「後から応答が届く」状況を作る。
 */
class Gate {
    private val opened = CompletableDeferred<Unit>()
    private val arrivalCount = MutableStateFlow(0)
    val arrivals: Int get() = arrivalCount.value

    /** モックの中で呼ぶ。open されるまで待つ */
    suspend fun pass() {
        arrivalCount.update { it + 1 }
        opened.await()
    }

    /** pass に count 回到達する（＝処理が通信中で止まる）まで待つ */
    suspend fun waitForArrival(count: Int = 1) {
        arrivalCount.first { it >= count }
    }

    fun open() {
        opened.complete(Unit)
    }
}

/**
 * Dispatchers.Main をテスト用に置き換える（viewModelScope は Main で動く）。
 * runTest は Main に置いたテスト用 Dispatcher の scheduler を使うため、viewModelScope の処理もテストの中で進む。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

/** block が expected と等しい例外を投げることを確かめる（suspend の処理も書ける） */
inline fun expectThrows(expected: Throwable, block: () -> Unit) {
    try {
        block()
    } catch (e: Throwable) {
        assertEquals(expected, e)
        return
    }
    fail("例外が投げられなかった: $expected")
}
