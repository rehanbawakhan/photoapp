package com.photoapp.data.security

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HiddenSecurityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun isPinSet(): Boolean {
        return prefs.contains(KEY_PIN)
    }

    fun savePin(pin: String) {
        prefs.edit().putString(KEY_PIN, pin).apply()
    }

    fun verifyPin(pin: String): Boolean {
        val savedPin = prefs.getString(KEY_PIN, null)
        return savedPin != null && savedPin == pin
    }

    fun clearPin() {
        prefs.edit().remove(KEY_PIN).apply()
    }

    companion object {
        private const val PREFS_NAME = "hidden_security_prefs"
        private const val KEY_PIN = "security_pin"
    }
}
