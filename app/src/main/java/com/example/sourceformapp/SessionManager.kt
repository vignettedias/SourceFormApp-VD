package com.example.sourceformapp

import android.content.Context

class SessionManager(

    context: Context
) {

    // -------------------------------------
    // SHARED PREFS
    // -------------------------------------

    private val prefs =

        context.getSharedPreferences(

            "sourceform_session",

            Context.MODE_PRIVATE
        )

    // -------------------------------------
    // SAVE JWT TOKEN
    // -------------------------------------

    fun saveToken(
        token: String
    ) {

        prefs.edit()

            .putString(
                "jwt_token",
                token
            )

            .apply()
    }

    // -------------------------------------
    // GET JWT TOKEN
    // -------------------------------------

    fun getToken(): String? {

        return prefs.getString(

            "jwt_token",

            null
        )
    }

    // -------------------------------------
    // SAVE LOGIN STATE
    // -------------------------------------

    fun setLoggedIn(
        value: Boolean
    ) {

        prefs.edit()

            .putBoolean(
                "logged_in",
                value
            )

            .apply()
    }

    // -------------------------------------
    // CHECK LOGIN STATE
    // -------------------------------------

    fun isLoggedIn(): Boolean {

        return prefs.getBoolean(

            "logged_in",

            false
        )
    }

    // -------------------------------------
    // LOGOUT
    // -------------------------------------

    fun clearSession() {

        prefs.edit()

            .clear()

            .apply()
    }
}
