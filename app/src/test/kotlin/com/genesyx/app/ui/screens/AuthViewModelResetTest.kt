package com.genesyx.app.ui.screens

import com.genesyx.app.auth.AuthRepository
import com.genesyx.app.auth.GoogleCredentialClient
import com.genesyx.app.core.log.Logger
import com.genesyx.app.core.result.DataResult
import com.genesyx.app.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/**
 * Password recovery from the signed-out Auth gate. Asking for the email is not a way past it,
 * and a failed send must not look like a confirmation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelResetTest {

    @get:Rule val main = MainDispatcherRule()

    private val authRepository = mockk<AuthRepository>()
    private val googleClient = mockk<GoogleCredentialClient>(relaxed = true)
    private val logger = mockk<Logger>(relaxed = true)

    private fun vm() = AuthViewModel(authRepository, googleClient, logger)

    @Test
    fun `a successful send shows the neutral inbox notice and does not sign her in`() = runTest {
        coEvery { authRepository.sendPasswordReset("ada@example.com") } returns DataResult.Success(Unit)
        val vm = vm()

        vm.sendPasswordReset("ada@example.com")

        assertEquals(AuthViewModel.RESET_SENT, vm.uiState.value.resetNotice)
        assertNull(vm.uiState.value.error)
        assertEquals(false, vm.uiState.value.loading)
        coVerify(exactly = 1) { authRepository.sendPasswordReset("ada@example.com") }
        coVerify(exactly = 0) { authRepository.signInWithPassword(any(), any()) }
    }

    @Test
    fun `a failed send tells her it failed instead of confirming an email that never left`() = runTest {
        coEvery { authRepository.sendPasswordReset(any()) } returns
            DataResult.Error(RuntimeException("offline"))

        val vm = vm()
        vm.sendPasswordReset("ada@example.com")

        assertEquals(AuthViewModel.RESET_FAILED, vm.uiState.value.resetNotice)
        assertNull(vm.uiState.value.error)
        coVerify(exactly = 0) { authRepository.signInWithPassword(any(), any()) }
    }
}
