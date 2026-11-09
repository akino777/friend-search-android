package com.example.friend_search.model

import com.example.friend_search.apimodel.GameApiModel

data class Game(
    /** 永続識別子（表示名が変わっても変わらない） */
    val id: String,
    val displayName: String,
    val aliases: List<String>,
    val isActive: Boolean,
) {
    // MARK: - APIModel → Model

    companion object {
        fun from(apiModel: GameApiModel) = Game(apiModel.id, apiModel.displayName, apiModel.aliases, apiModel.active)
    }
}
