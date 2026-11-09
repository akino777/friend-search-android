package com.example.friend_search.apimodel

import kotlinx.serialization.Serializable

@Serializable
data class PostResponseApiModel(
    val id: String,
    val gameId: String,
    val body: String,
    val vcTool: String,
    val capacity: Int,
    val participantCount: Int,
    val status: String,
    val isJoinedByMe: Boolean,
    val isOwnedByMe: Boolean,
    val createdAt: String,
    val author: ProfileResponseApiModel,
    val inviteUrl: String? = null,
)

@Serializable
data class PostPageResponseApiModel(
    val items: List<PostResponseApiModel>,
    val nextCursor: String? = null,
)

@Serializable
data class PostSearchResponseApiModel(val items: List<PostResponseApiModel>)
