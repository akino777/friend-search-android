package com.example.friend_search.apimodel

import kotlinx.serialization.Serializable

@Serializable
data class GameCatalogApiModel(val games: List<GameApiModel>)

@Serializable
data class GameApiModel(
    val id: String,
    val displayName: String,
    val aliases: List<String>,
    val active: Boolean,
)
