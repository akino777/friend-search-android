package com.example.friend_search

import android.util.Base64
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.friend_search.view.root.MainActivity
import kotlin.random.Random
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * モックを使わず、実アプリから dev 環境へ接続して、投稿・新着一覧・AI検索・参加・取消・終了をサーバーまで通す。
 * 画面の細かい状態はユニットテストで確かめるので、ここでは扱わない。
 * scripts/android-live-uitest.sh がインストルメンテーション引数 liveAccounts（Base64 の JSON）で専用アカウントを渡す（未指定ならスキップ）。
 */
@RunWith(AndroidJUnit4::class)
class LiveUiTests {
    @Serializable
    private data class Accounts(val password: String, val emails: List<String>)

    companion object {
        private const val GAME = "apex-legends"
        private const val TIMEOUT = 30_000L
    }

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private lateinit var accounts: Accounts

    @Before
    fun setUp() {
        val encoded = InstrumentationRegistry.getArguments().getString("liveAccounts")
        assumeTrue("liveAccounts が未指定のためスキップ（scripts/android-live-uitest.sh で実行）", encoded != null)
        accounts = Json.decodeFromString(String(Base64.decode(encoded, Base64.DEFAULT)))
        assertTrue(accounts.emails.size >= 2)
    }

    @Test
    fun 投稿から検索と参加と取消と終了まで通る() {
        // 合言葉の数字は実行ごとに変え、一覧・検索結果から自分の募集を見つける目印にする
        val token = "%08d".format(Random.nextInt(100_000_000))
        val body = "週末の夜にまったり遊べる人を募集しています。初心者同士、気楽にいきましょう。合言葉は「$token」で。"
        val (owner, joiner) = accounts.emails
        launchSignedOut()

        // 投稿者: 投稿する
        signIn(owner, "ハルト")
        tap(hasTestTag("tab.POST"))
        tap(hasTestTag("create.gamePicker"))
        tap(hasTestTag("game.$GAME"))
        node(hasTestTag("create.body")).performTextInput(body)
        node(hasTestTag("create.capacity")).run {
            performTextClearance()
            performTextInput("3")
        }
        node(hasTestTag("create.submit")).performScrollTo()
        tap(hasTestTag("create.submit"))
        assertTrue("投稿後に詳細へ遷移しない", appeared(hasTestTag("detail.close")))
        signOut()

        // 参加者: 新着一覧に出ることと AI 検索で見つかることを確かめ、参加して取り消す
        signIn(joiner, "りん")
        tap(hasTestTag("tab.SEARCH"))
        waitInList(token)
        searchInList(body, token)
        tap(row(token))
        tap(hasTestTag("detail.join"))
        assertCount(1)
        tap(hasTestTag("detail.leave"))
        assertCount(0)
        signOut()

        // 投稿者: マイページから募集を終了する
        signIn(owner, "ハルト")
        tap(hasTestTag("tab.MY_PAGE"))
        tap(row(token))
        tap(hasTestTag("detail.close"))
        tap(hasText("募集を終了する"))
        assertTrue("募集を終了できない", appeared(hasText("この募集は終了しました")))
    }

    // MARK: - 認証

    /** 前回のセッションが残っていても、ログイン画面から始める */
    private fun launchSignedOut() {
        val signInField = hasTestTag("signIn.email")
        val tabBar = hasTestTag("tab.SEARCH")
        val setup = hasTestTag("profile.displayName")
        compose.waitUntil(TIMEOUT) { exists(signInField) || exists(tabBar) || exists(setup) }
        if (exists(signInField)) return
        if (exists(setup)) completeProfile("ハルト")
        signOut()
    }

    /** 初回はプロフィール作成も通す。作成済みのアカウントならそのままタブ画面へ進む */
    private fun signIn(email: String, displayName: String) {
        node(hasTestTag("signIn.email")).performTextInput(email)
        node(hasTestTag("signIn.password")).performTextInput(accounts.password)
        tap(hasTestTag("signIn.submit"))
        val setup = hasTestTag("profile.displayName")
        val tabBar = hasTestTag("tab.SEARCH")
        compose.waitUntil(TIMEOUT) { exists(setup) || exists(tabBar) }
        if (exists(setup)) completeProfile(displayName)
        assertTrue("$email でログインできない", appeared(tabBar))
    }

    private fun completeProfile(displayName: String) {
        node(hasTestTag("profile.displayName")).performTextInput(displayName)
        tap(hasTestTag("profile.submit"))
        assertTrue("プロフィールを作成できない", appeared(hasTestTag("tab.SEARCH")))
    }

    private fun signOut() {
        tap(hasTestTag("tab.MY_PAGE"))
        appeared(hasTestTag("mypage.content"))
        node(hasTestTag("mypage.content")).performScrollToNode(hasTestTag("mypage.signOut"))
        tap(hasTestTag("mypage.signOut"))
        tap(hasTestTag("mypage.signOut.confirm"))
        assertTrue("ログアウトできない", appeared(hasTestTag("signIn.email")))
    }

    // MARK: - 募集

    /** 反映遅延（GSI）を許容し、引っ張って更新しながら一覧に出るのを待つ */
    private fun waitInList(token: String) {
        val target = row(token)
        repeat(10) {
            if (appeared(target, 3_000)) return
            pullToRefresh()
        }
        assertTrue("新着一覧に募集が表示されない", exists(target))
    }

    /**
     * AI 検索し、投稿した募集が結果に出るまで待つ。Embedding の生成を待つため、出るまで引っ張って更新で検索し直す。
     * 同じ条件は直前の完了から5秒以内だと送られないので10秒待ってから更新する（検索回数の上限に配慮して10回まで）
     */
    private fun searchInList(query: String, token: String) {
        node(hasTestTag("list.searchField")).performTextInput(query)
        tap(hasTestTag("list.searchButton"))
        assertTrue("検索結果に切り替わらない", appeared(hasTestTag("list.clearSearch")))
        val target = row(token)
        repeat(10) {
            if (appeared(target, 10_000)) return
            pullToRefresh()
        }
        assertTrue("AI検索の結果に投稿した募集が出ない", exists(target))
    }

    private fun pullToRefresh() {
        node(hasTestTag("list.content")).performTouchInput { swipeDown() }
    }

    private fun assertCount(expected: Int) {
        val count = hasTestTag("detail.count") and SemanticsMatcher("参加人数が $expected") { node ->
            node.config.getOrNull(SemanticsProperties.Text)?.any { it.text.startsWith("$expected / ") } == true
        }
        assertTrue("参加人数が $expected にならない", appeared(count))
    }

    // MARK: - 共通

    /** 募集の行（一覧・検索結果・マイページ）のうち、本文に token を含むもの */
    private fun row(token: String) = SemanticsMatcher("募集の行（$token）") { node ->
        node.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("post.row.") == true &&
            node.config.getOrNull(SemanticsProperties.Text)?.any { it.text.contains(token) } == true
    }

    private fun exists(matcher: SemanticsMatcher) = compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()

    private fun appeared(matcher: SemanticsMatcher, timeout: Long = TIMEOUT): Boolean = try {
        compose.waitUntil(timeout) { exists(matcher) }
        true
    } catch (_: AssertionError) {
        false
    } catch (_: androidx.compose.ui.test.ComposeTimeoutException) {
        false
    }

    private fun node(matcher: SemanticsMatcher) = compose.onAllNodes(matcher).onFirst()

    private fun tap(matcher: SemanticsMatcher) {
        assertTrue("見つからない: ${matcher.description}", appeared(matcher))
        node(matcher).performClick()
    }
}
