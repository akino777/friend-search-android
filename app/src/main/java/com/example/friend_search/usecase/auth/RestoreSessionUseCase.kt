package com.example.friend_search.usecase.auth

import com.example.friend_search.repository.AuthRepository
import com.example.friend_search.repository.DefaultAuthRepository

interface RestoreSessionUseCase {
    /** 端末に保存されたセッションでログイン中か */
    suspend fun execute(): Boolean
}

class DefaultRestoreSessionUseCase(
    private val repository: AuthRepository = DefaultAuthRepository(),
) : RestoreSessionUseCase {
    override suspend fun execute() = repository.isSignedIn()
}
