package com.example.friend_search.usecase.profile

import com.example.friend_search.model.PublicProfile
import com.example.friend_search.repository.DefaultProfileRepository
import com.example.friend_search.repository.ProfileRepository

interface UpdateProfileUseCase {
    suspend fun execute(displayName: String): PublicProfile
}

class DefaultUpdateProfileUseCase(
    private val repository: ProfileRepository = DefaultProfileRepository(),
) : UpdateProfileUseCase {
    override suspend fun execute(displayName: String): PublicProfile = repository.updateDisplayName(displayName)
}
