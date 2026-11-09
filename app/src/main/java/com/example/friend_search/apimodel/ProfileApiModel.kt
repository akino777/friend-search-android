package com.example.friend_search.apimodel

import kotlinx.serialization.Serializable

@Serializable
data class ProfileResponseApiModel(
    val publicUserId: String,
    val displayName: String,
    val avatarVersion: Int,
)

@Serializable
data class DisplayNameRequestApiModel(val displayName: String)
