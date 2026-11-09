package com.example.friend_search.repository

import com.example.friend_search.api.DefaultFriendSearchApi
import com.example.friend_search.api.FriendSearchApi
import com.example.friend_search.apimodel.DisplayNameRequestApiModel
import com.example.friend_search.model.ProfileLookup
import com.example.friend_search.model.PublicProfile
import kotlin.coroutines.cancellation.CancellationException

interface ProfileRepository {
    suspend fun myProfile(): ProfileLookup
    suspend fun bootstrap(displayName: String): PublicProfile
    suspend fun updateDisplayName(displayName: String): PublicProfile
}

class DefaultProfileRepository(
    private val api: FriendSearchApi = DefaultFriendSearchApi(),
) : ProfileRepository {
    override suspend fun myProfile(): ProfileLookup = withAppErrors {
        try {
            ProfileLookup.Found(PublicProfile.from(api.fetchMe()))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ProfileLookup.fromNotFoundError(e) ?: throw e
        }
    }

    override suspend fun bootstrap(displayName: String) =
        withAppErrors { PublicProfile.from(api.bootstrap(DisplayNameRequestApiModel(displayName))) }

    override suspend fun updateDisplayName(displayName: String) =
        withAppErrors { PublicProfile.from(api.updateMe(DisplayNameRequestApiModel(displayName))) }
}
