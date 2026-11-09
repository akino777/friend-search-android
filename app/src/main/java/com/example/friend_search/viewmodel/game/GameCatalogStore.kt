package com.example.friend_search.viewmodel.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.friend_search.model.AppError
import com.example.friend_search.model.Game
import com.example.friend_search.usecase.game.DefaultGetGamesUseCase
import com.example.friend_search.usecase.game.GetGamesUseCase
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** アプリ全体で共有するゲームマスタ */
class GameCatalogStore(
    private val getGames: GetGamesUseCase = DefaultGetGamesUseCase(),
) : ViewModel() {
    /** 有効・無効を含むゲームマスタ。未取得なら null */
    var games by mutableStateOf<List<Game>?>(null)
        private set
    var error by mutableStateOf<AppError?>(null)
        private set

    /** 実行中の取得。同時に呼ばれたら新しく取得せず、この完了を待つ */
    private var loading by mutableStateOf<Job?>(null)
    private var index: GameSearchIndex? = null

    val isLoading: Boolean get() = loading != null

    suspend fun loadIfNeeded() {
        if (games == null || error != null) refresh()
    }

    suspend fun refresh() {
        loading?.let {
            it.join()
            return
        }
        // 画面を離れても取得は続け、共有するマスタを最後まで更新する（呼び出し元が取り消されても続く）。
        // 開始前に loading へ入れてから始める（すぐ終わる取得でも、終了時に loading を空に戻せるように）
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                fetch()
            } finally {
                loading = null
            }
        }
        loading = job
        job.join()
    }

    fun search(query: String): List<Game> = index?.search(query) ?: emptyList()

    /** マスタにない ID（未取得など）はそのまま表示する */
    fun displayName(gameId: String): String = games?.firstOrNull { it.id == gameId }?.displayName ?: gameId

    private suspend fun fetch() {
        try {
            val games = getGames.execute()
            this.games = games
            index = GameSearchIndex(games)
            error = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = AppError.from(e)
        }
    }
}
