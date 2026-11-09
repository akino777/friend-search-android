package com.example.friend_search.model

import com.amplifyframework.auth.result.AuthSignInResult
import com.amplifyframework.auth.result.step.AuthSignInStep

/** サインインの結果 */
enum class SignInOutcome {
    SIGNED_IN,

    /** MFA など、このアプリが対応しない追加手順 */
    UNSUPPORTED_STEP;

    companion object {
        fun from(apiModel: AuthSignInResult): SignInOutcome = when (apiModel.nextStep.signInStep) {
            AuthSignInStep.DONE -> SIGNED_IN
            else -> UNSUPPORTED_STEP
        }
    }
}
