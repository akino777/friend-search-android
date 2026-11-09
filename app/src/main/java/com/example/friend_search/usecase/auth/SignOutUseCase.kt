package com.example.friend_search.usecase.auth

import com.example.friend_search.repository.AuthRepository
import com.example.friend_search.repository.DefaultAuthRepository

interface SignOutUseCase {
    suspend fun execute()
}

class DefaultSignOutUseCase(
    private val repository: AuthRepository = DefaultAuthRepository(),
) : SignOutUseCase {
    override suspend fun execute() = repository.signOut()
}
