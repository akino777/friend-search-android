package com.example.friend_search.api

/**
 * 接続先（dev フレーバー: dev 環境）。値は公開リポジトリに載せないため、Git 管理外の `DevEndpoint.kt` に置く（テストする人には別途伝える）。
 * client secret や IAM 資格情報は置かない。
 */
object Endpoint {
    const val AWS_REGION = "ap-northeast-1"
    /** Retrofit の基準 URL は末尾を `/` にする */
    const val API_BASE_URL = DevEndpoint.API_BASE_URL
    const val USER_POOL_ID = DevEndpoint.USER_POOL_ID
    const val USER_POOL_CLIENT_ID = DevEndpoint.USER_POOL_CLIENT_ID
}
