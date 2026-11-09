package com.example.friend_search.api

import android.content.Context
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession
import com.amplifyframework.auth.cognito.result.AWSCognitoAuthSignOutResult
import com.amplifyframework.auth.exceptions.SignedOutException
import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.core.configuration.AmplifyOutputs
import com.amplifyframework.kotlin.auth.Auth
import com.amplifyframework.kotlin.core.Amplify
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** Cognito 認証（Amplify Android Auth、コルーチン版の core-kotlin）。Amplify の結果型を APIModel としてそのまま返す（ログイン状態とトークンは値だけ返す）。 */
interface CognitoAuthApi {
    /** 端末に保存されたセッションでログイン中か */
    suspend fun isSignedIn(): Boolean

    /** 業務 API 用の Access Token（期限切れなら Amplify が更新してから返す） */
    suspend fun accessToken(): String
    suspend fun signIn(email: String, password: String): AuthSignInResult
    suspend fun signOut()
}

/** インスタンスはいくつ作ってもよい（Amplify の設定はプロセス全体で一度だけ、アプリ起動時に [configure] で行う） */
class DefaultCognitoAuthApi : CognitoAuthApi {
    companion object {
        @Volatile
        private var configuration: Result<Unit> = Result.failure(IllegalStateException("Amplify が設定されていません"))

        /**
         * Terraform 管理の既存 User Pool へ接続する。アプリ起動時（Application.onCreate）に一度だけ呼ぶ。
         * 失敗した結果も保持し、以後の呼び出しで同じエラーを返す。
         * 設定は amplify_outputs の形式の JSON で渡す（ファイルを使わない場合も同じ形式。AmplifyOutputs.fromString）。
         */
        fun configure(context: Context) {
            configuration = runCatching {
                Amplify.addPlugin(AWSCognitoAuthPlugin())
                Amplify.configure(AmplifyOutputs(outputsJson()), context)
            }
        }

        private fun outputsJson(): String = buildJsonObject {
            put("version", "1.1")
            putJsonObject("auth") {
                put("aws_region", Endpoint.AWS_REGION)
                put("user_pool_id", Endpoint.USER_POOL_ID)
                put("user_pool_client_id", Endpoint.USER_POOL_CLIENT_ID)
                putJsonArray("username_attributes") { add("email") }
            }
        }.toString()
    }

    private val auth: Auth
        get() {
            configuration.getOrThrow()
            return Amplify.Auth
        }

    override suspend fun isSignedIn(): Boolean = auth.fetchAuthSession().isSignedIn

    override suspend fun accessToken(): String {
        val session = auth.fetchAuthSession() as? AWSCognitoAuthSession ?: throw SignedOutException()
        val tokens = session.userPoolTokensResult
        tokens.error?.let { throw it }
        return tokens.value?.accessToken ?: throw SignedOutException()
    }

    override suspend fun signIn(email: String, password: String): AuthSignInResult = auth.signIn(email, password)

    /** PartialSignOut（トークン失効のみ失敗）もローカルのサインアウトは完了しているため成功扱い */
    override suspend fun signOut() {
        val result = auth.signOut()
        if (result is AWSCognitoAuthSignOutResult.FailedSignOut) throw result.exception
    }
}
