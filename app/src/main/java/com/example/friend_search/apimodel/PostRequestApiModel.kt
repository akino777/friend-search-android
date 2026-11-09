package com.example.friend_search.apimodel

import kotlinx.serialization.Serializable

// null の項目は既定値のため JSON に含めない（kotlinx.serialization は既定値を出力しない）

@Serializable
data class CreatePostRequestApiModel(
    val gameId: String,
    val body: String,
    val vcTool: String,
    val capacity: Int,
    val clientRequestId: String,
    val inviteUrl: String? = null,
)

@Serializable
data class PostSearchRequestApiModel(
    /** null なら全ゲーム */
    val gameId: String? = null,
    val query: String,
    val vcTool: String? = null,
)
