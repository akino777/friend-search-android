package com.example.friend_search.apimodel

import kotlinx.serialization.Serializable

/** 共通エラー形式 `{ "error": { "code", "message", "pendingAvatar" } }` */
@Serializable
data class ErrorResponseApiModel(val error: Body) {
    @Serializable
    data class Body(
        val code: String,
        val message: String,
        val pendingAvatar: PendingAvatar? = null,
    )

    @Serializable
    data class PendingAvatar(
        val publicUserId: String,
        val avatarVersion: Int,
    )
}

/** HTTP エラー応答。Gateway 生成の応答など本文が共通形式でない場合、body は null */
data class HttpErrorApiModel(
    val statusCode: Int,
    val body: ErrorResponseApiModel?,
) : Exception("HTTP $statusCode")
