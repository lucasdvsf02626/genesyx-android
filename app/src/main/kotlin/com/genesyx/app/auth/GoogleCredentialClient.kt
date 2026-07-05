package com.genesyx.app.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import javax.inject.Inject

/**
 * Thin wrapper over Android Credential Manager for the "Continue with Google" flow. Returns the
 * Google ID token to hand to Supabase (`signInWith(IDToken)`). Throws on cancellation / no account /
 * failure — the caller maps those to UI state. Needs an Activity context to show the account sheet.
 */
class GoogleCredentialClient @Inject constructor() {

    suspend fun getIdToken(activityContext: Context, serverClientId: String): String {
        val option = GetSignInWithGoogleOption.Builder(serverClientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        val response = CredentialManager.create(activityContext).getCredential(activityContext, request)
        val credential = response.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }
        error("Unexpected credential type: ${credential.type}")
    }
}
