package com.example.friend_search.viewmodel.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.friend_search.model.Game

/** ゲーム選択（端末内のマスタから候補を出す） */
class GamePickerViewModel(val store: GameCatalogStore) {
    var query by mutableStateOf("")

    val candidates: List<Game> get() = store.search(query)

    /** マスタを読み込めていて、入力があるのに候補が0件か */
    val showsNoMatch: Boolean get() = store.games != null && query.isNotBlank() && candidates.isEmpty()

    suspend fun load() {
        store.loadIfNeeded()
    }
}
