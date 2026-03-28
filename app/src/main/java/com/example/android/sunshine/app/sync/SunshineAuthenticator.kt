package com.example.android.sunshine.app.sync

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.NetworkErrorException
import android.content.Context
import android.os.Bundle

/**
 * Manages "Authentication" to Sunshine's backend service.  The SyncAdapter framework
 * requires an authenticator object, so syncing to a service that doesn't need authentication
 * typically means creating a stub authenticator like this one.
 */
class SunshineAuthenticator(context: Context) : AbstractAccountAuthenticator(context) {

    override fun editProperties(r: AccountAuthenticatorResponse, s: String): Bundle {
        throw UnsupportedOperationException()
    }

    override fun addAccount(
        r: AccountAuthenticatorResponse,
        s: String,
        s2: String,
        strings: Array<String>,
        bundle: Bundle
    ): Bundle? = null

    override fun confirmCredentials(
        r: AccountAuthenticatorResponse,
        account: Account,
        bundle: Bundle
    ): Bundle? = null

    override fun getAuthToken(
        r: AccountAuthenticatorResponse,
        account: Account,
        s: String,
        bundle: Bundle
    ): Bundle {
        throw UnsupportedOperationException()
    }

    override fun getAuthTokenLabel(s: String): String {
        throw UnsupportedOperationException()
    }

    override fun updateCredentials(
        r: AccountAuthenticatorResponse,
        account: Account,
        s: String,
        bundle: Bundle
    ): Bundle {
        throw UnsupportedOperationException()
    }

    override fun hasFeatures(
        r: AccountAuthenticatorResponse,
        account: Account,
        strings: Array<String>
    ): Bundle {
        throw UnsupportedOperationException()
    }
}
