package com.example.friend_search.usecase.auth

import com.example.friend_search.model.SignInOutcome
import com.example.friend_search.repository.AuthRepository
import com.example.friend_search.repository.DefaultAuthRepository

interface SignInUseCase {
    suspend fun execute(email: String, password: String): SignInOutcome
}

class DefaultSignInUseCase(
    private val repository: AuthRepository = DefaultAuthRepository(),
) : SignInUseCase {
    override suspend fun execute(email: String, password: String) = repository.signIn(email, password)
}
