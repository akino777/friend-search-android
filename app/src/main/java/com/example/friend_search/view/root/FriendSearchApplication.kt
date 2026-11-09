package com.example.friend_search.view.root

import android.app.Application
import com.example.friend_search.api.DefaultCognitoAuthApi

class FriendSearchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Amplify の設定はプロセス全体で一度だけ行う（Amplify は Context を必要とする）
        DefaultCognitoAuthApi.configure(this)
    }
}
