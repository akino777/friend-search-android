package com.example.friend_search.repository

import com.example.friend_search.api.CognitoAuthApi
import com.example.friend_search.api.DefaultCognitoAuthApi
import com.example.friend_search.model.SignInOutcome

/** 認証（Cognito） */
interface AuthRepository {
    suspend fun isSignedIn(): Boolean
    suspend fun signIn(email: String, password: String): SignInOutcome
    suspend fun signOut()
}

class DefaultAuthRepository(
    private val api: CognitoAuthApi = DefaultCognitoAuthApi(),
) : AuthRepository {
    override suspend fun isSignedIn() = withAppErrors { api.isSignedIn() }

    override suspend fun signIn(email: String, password: String) = withAppErrors { SignInOutcome.from(api.signIn(email, password)) }

    override suspend fun signOut() = withAppErrors { api.signOut() }
}
