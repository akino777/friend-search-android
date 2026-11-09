package com.example.friend_search.usecase.profile

import com.example.friend_search.model.PublicProfile
import com.example.friend_search.repository.DefaultProfileRepository
import com.example.friend_search.repository.ProfileRepository

interface BootstrapProfileUseCase {
    suspend fun execute(displayName: String): PublicProfile
}

class DefaultBootstrapProfileUseCase(
    private val repository: ProfileRepository = DefaultProfileRepository(),
) : BootstrapProfileUseCase {
    override suspend fun execute(displayName: String): PublicProfile = repository.bootstrap(displayName)
}
