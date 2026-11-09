package com.example.friend_search.api

/**
 * 接続先（prod フレーバー: prod 環境）。いずれも公開値（秘密情報ではない）。client secret や IAM 資格情報は置かない。
 * prod 環境は未構築。構築後に値を入れる。
 */
object Endpoint {
    const val AWS_REGION = "ap-northeast-1"
    /** Retrofit の基準 URL は末尾を `/` にする */
    const val API_BASE_URL = "https://prod.invalid/v1/"
    const val USER_POOL_ID = ""
    const val USER_POOL_CLIENT_ID = ""
}
