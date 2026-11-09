package com.example.friend_search.viewmodel

import com.example.friend_search.Fixture
import com.example.friend_search.model.PublicProfile
import com.example.friend_search.viewmodel.profile.MyPageViewModel
import com.example.friend_search.viewmodel.profile.ProfileSetupViewModel
import com.example.friend_search.viewmodel.shared.DisplayNameRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// プロフィールの ViewModel の入力検証。依存は UseCase のモックのみ。

class ProfileNameTests {
    @Test
    fun 表示名を整えて作成する() = runTest {
        val bootstrap = MockBootstrapProfileUseCase()
        bootstrap.handler = { Fixture.author }
        val viewModel = ProfileSetupViewModel(bootstrap)
        viewModel.displayName = " ハンター "
        assertEquals(Fixture.author, viewModel.submit())
        assertEquals(listOf("ハンター"), bootstrap.names)
    }

    @Test
    fun 規則外の表示名は送らない() = runTest {
        for (name in listOf(" あ ", "a".repeat(25), "名前\u0007")) {
            val bootstrap = MockBootstrapProfileUseCase()
            val viewModel = ProfileSetupViewModel(bootstrap)
            viewModel.displayName = name
            assertFalse(viewModel.canSubmit)
            assertNull(viewModel.submit())
            assertEquals(DisplayNameRule.INVALID_MESSAGE, viewModel.errorMessage)
            assertTrue(bootstrap.names.isEmpty())
        }
    }

    @Test
    fun 表示名の変更は規則を満たすときだけ保存してルートへ伝える() = runTest {
        val update = MockUpdateProfileUseCase()
        val renamed = PublicProfile("u", "新しい名前", 1)
        update.handler = { renamed }
        var notified: PublicProfile? = null
        val viewModel = MyPageViewModel(Fixture.author, MockListMyPostsUseCase(), update, onProfileUpdated = { notified = it })
        viewModel.beginEditingName()
        assertEquals(Fixture.author.displayName, viewModel.editingName)
        viewModel.editingName = "x"
        assertFalse(viewModel.canSaveName)
        assertFalse(viewModel.saveName())
        assertTrue(update.names.isEmpty())
        viewModel.editingName = " 新しい名前 "
        assertTrue(viewModel.saveName())
        assertEquals(listOf("新しい名前"), update.names)
        assertEquals(renamed, viewModel.profile)
        assertEquals(renamed, notified)
    }
}
