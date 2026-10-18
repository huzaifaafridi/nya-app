package com.nya.app.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.nya.app.R

/** Returns the Google ID token, or null when the user cancels. */
suspend fun googleIdToken(activityContext: Context): Result<String?> = try {
    val option = GetSignInWithGoogleOption.Builder(activityContext.getString(R.string.default_web_client_id)).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
    val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
    Result.success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
} catch (e: GetCredentialCancellationException) {
    Result.success(null)
} catch (e: Exception) {
    Result.failure(e)
}
