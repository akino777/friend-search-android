package com.example.friend_search.usecase.profile

import com.example.friend_search.model.ProfileLookup
import com.example.friend_search.repository.DefaultProfileRepository
import com.example.friend_search.repository.ProfileRepository

interface GetProfileUseCase {
    suspend fun execute(): ProfileLookup
}

class DefaultGetProfileUseCase(
    private val repository: ProfileRepository = DefaultProfileRepository(),
) : GetProfileUseCase {
    override suspend fun execute(): ProfileLookup = repository.myProfile()
}
