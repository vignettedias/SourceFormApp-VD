package com.example.sourceformapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.FacebookAuthProvider

import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult

class LoginActivity : AppCompatActivity() {

    // ------------------------------------
    // Google
    // ------------------------------------

    private lateinit var googleSignInClient:
            GoogleSignInClient

    // ------------------------------------
    // Firebase
    // ------------------------------------

    private lateinit var auth: FirebaseAuth

    // ------------------------------------
    // Facebook
    // ------------------------------------

    private lateinit var callbackManager:
            CallbackManager

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        // ------------------------------------
        // Backend Auto Discovery
        // ------------------------------------

        BackendDiscovery.discoverBackend(this) {

                backendUrl ->

            runOnUiThread {

                RetrofitClient.setBaseUrl(
                    backendUrl
                )

                Toast.makeText(

                    this,

                    "Backend Found:\n$backendUrl",

                    Toast.LENGTH_LONG

                ).show()

                Log.d(

                    "DISCOVERY",

                    backendUrl
                )
            }
        }

        // ------------------------------------
        // Firebase Init
        // ------------------------------------

        auth = FirebaseAuth.getInstance()

        // ------------------------------------
        // Facebook Init
        // ------------------------------------

        callbackManager =
            CallbackManager.Factory.create()

        // ------------------------------------
        // Auto Login
        // ------------------------------------

        if (auth.currentUser != null) {

            startActivity(

                Intent(
                    this,
                    MainActivity::class.java
                )
            )

            finish()
        }

        // ------------------------------------
        // Google Config
        // ------------------------------------

        val gso =
            GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN
            )

                .requestIdToken(
                    getString(
                        R.string.default_web_client_id
                    )
                )

                .requestEmail()

                .build()

        googleSignInClient =
            GoogleSignIn.getClient(this, gso)

        // ------------------------------------
        // Google Button
        // ------------------------------------

        findViewById<Button>(
            R.id.btnGoogle
        ).setOnClickListener {

            signInGoogle()
        }

        // ------------------------------------
        // GitHub Button
        // ------------------------------------

        findViewById<Button>(
            R.id.btnGithub
        ).setOnClickListener {

            githubLogin()
        }

        // ------------------------------------
        // Facebook Button
        // ------------------------------------

        findViewById<Button>(
            R.id.btnFacebook
        ).setOnClickListener {

            facebookLogin()
        }
    }

    // ------------------------------------
    // Google Login
    // ------------------------------------

    private fun signInGoogle() {

        val signInIntent =
            googleSignInClient.signInIntent

        launcher.launch(signInIntent)
    }

    // ------------------------------------
    // GitHub Login
    // ------------------------------------

    private fun githubLogin() {

        val provider =
            OAuthProvider
                .newBuilder("github.com")

        auth.startActivityForSignInWithProvider(
            this,
            provider.build()
        )

            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "GitHub Login Success",
                    Toast.LENGTH_SHORT
                ).show()

                startActivity(

                    Intent(
                        this,
                        MainActivity::class.java
                    )
                )

                finish()
            }

            .addOnFailureListener {

                Toast.makeText(
                    this,
                    it.message,
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // ------------------------------------
    // Facebook Login
    // ------------------------------------

    private fun facebookLogin() {

        LoginManager.getInstance()
            .logInWithReadPermissions(

                this,

                listOf(
                    "email",
                    "public_profile"
                )
            )

        LoginManager.getInstance()
            .registerCallback(

                callbackManager,

                object :
                    FacebookCallback<LoginResult> {

                    override fun onSuccess(
                        result: LoginResult
                    ) {

                        handleFacebookAccessToken(
                            result.accessToken
                        )
                    }

                    override fun onCancel() {

                        Toast.makeText(
                            this@LoginActivity,
                            "Facebook Login Cancelled",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    override fun onError(
                        error: FacebookException
                    ) {

                        Toast.makeText(
                            this@LoginActivity,
                            error.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
    }

    // ------------------------------------
    // Facebook Firebase Auth
    // ------------------------------------

    private fun handleFacebookAccessToken(
        token: AccessToken
    ) {

        val credential =
            FacebookAuthProvider
                .getCredential(
                    token.token
                )

        auth.signInWithCredential(
            credential
        )

            .addOnCompleteListener(this) {

                if (it.isSuccessful) {

                    Toast.makeText(
                        this,
                        "Facebook Login Success",
                        Toast.LENGTH_SHORT
                    ).show()

                    startActivity(

                        Intent(
                            this,
                            MainActivity::class.java
                        )
                    )

                    finish()

                } else {

                    Toast.makeText(
                        this,
                        "Firebase Facebook Auth Failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    // ------------------------------------
    // Google Result
    // ------------------------------------

    private val launcher =
        registerForActivityResult(
            ActivityResultContracts
                .StartActivityForResult()
        ) { result ->

            val task =
                GoogleSignIn
                    .getSignedInAccountFromIntent(
                        result.data
                    )

            try {

                val account =
                    task.getResult(
                        ApiException::class.java
                    )

                val credential =
                    GoogleAuthProvider
                        .getCredential(
                            account.idToken,
                            null
                        )

                auth.signInWithCredential(
                    credential
                )

                    .addOnCompleteListener {

                        if (it.isSuccessful) {

                            Toast.makeText(
                                this,
                                "Google Login Success",
                                Toast.LENGTH_SHORT
                            ).show()

                            startActivity(

                                Intent(
                                    this,
                                    MainActivity::class.java
                                )
                            )

                            finish()

                        } else {

                            Toast.makeText(
                                this,
                                "Authentication Failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

            } catch (e: Exception) {

                Toast.makeText(
                    this,
                    e.message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    // ------------------------------------
    // Facebook Result
    // ------------------------------------

    override fun onActivityResult(

        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(

            requestCode,
            resultCode,
            data
        )

        callbackManager.onActivityResult(

            requestCode,
            resultCode,
            data
        )
    }
}
