package com.example.sourceformapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText

    private lateinit var btnSignup: Button
    private lateinit var btnLogin: Button
    private lateinit var btnGoogle: Button
    private lateinit var btnGithub: Button
    private lateinit var btnFacebook: Button

    private lateinit var progressBar: ProgressBar
    private lateinit var tvSwitchMode: TextView

    private var isSignupMode = true

    private lateinit var sessionManager: SessionManager

    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var auth: FirebaseAuth

    private lateinit var callbackManager: CallbackManager

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        sessionManager = SessionManager(this)

        if (
            sessionManager.isLoggedIn()
            ||
            FirebaseAuth.getInstance().currentUser != null
        ) {

            startActivity(
                Intent(
                    this,
                    MainActivity::class.java
                )
            )

            finish()
        }

        etName =
            findViewById(R.id.etName)

        etEmail =
            findViewById(R.id.etEmail)

        etPassword =
            findViewById(R.id.etPassword)

        btnSignup =
            findViewById(R.id.btnSignup)

        btnLogin =
            findViewById(R.id.btnLogin)

        btnGoogle =
            findViewById(R.id.btnGoogle)

        btnGithub =
            findViewById(R.id.btnGithub)

        btnFacebook =
            findViewById(R.id.btnFacebook)

        progressBar =
            findViewById(R.id.progressBar)

        tvSwitchMode =
            findViewById(R.id.tvSwitchMode)

        auth =
            FirebaseAuth.getInstance()

        callbackManager =
            CallbackManager.Factory.create()

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

            GoogleSignIn.getClient(
                this,
                gso
            )

        tvSwitchMode.setOnClickListener {

            isSignupMode =
                !isSignupMode

            updateModeUI()
        }

        btnSignup.setOnClickListener {

            signup()
        }

        btnLogin.setOnClickListener {

            login()
        }

        btnGoogle.setOnClickListener {

            signInGoogle()
        }

        btnGithub.setOnClickListener {

            githubLogin()
        }

        btnFacebook.setOnClickListener {

            facebookLogin()
        }

        updateModeUI()
    }

    // -----------------------------------------
    // UI MODE
    // -----------------------------------------

    private fun updateModeUI() {

        if (isSignupMode) {

            etName.visibility =
                View.VISIBLE

            btnSignup.visibility =
                View.VISIBLE

            btnLogin.visibility =
                View.GONE

            tvSwitchMode.text =
                "Already have an account? Login"

        } else {

            etName.visibility =
                View.GONE

            btnSignup.visibility =
                View.GONE

            btnLogin.visibility =
                View.VISIBLE

            tvSwitchMode.text =
                "Don't have an account? Signup"
        }
    }

    // -----------------------------------------
    // SIGNUP
    // -----------------------------------------

    private fun signup() {

        val name =
            etName.text.toString().trim()

        val email =
            etEmail.text.toString().trim()

        val password =
            etPassword.text.toString().trim()
        if (
            name.isEmpty() ||
            email.isEmpty() ||
            password.isEmpty()
        ) {

            Toast.makeText(
                this,
                "Please fill all fields",
                Toast.LENGTH_LONG
            ).show()

            return
        }
        progressBar.visibility =
            View.VISIBLE

        CoroutineScope(
            Dispatchers.IO
        ).launch {

            try {

                val response =

                    RetrofitClient.api.signup(

                        SignupRequest(
                            name,
                            email,
                            password
                        )
                    )

                withContext(
                    Dispatchers.Main
                ) {

                    progressBar.visibility =
                        View.GONE

                    if (
                        response.isSuccessful
                    ) {

                        val body =
                            response.body()

                        if (
                            body != null
                            &&
                            body.success
                            &&
                            body.token != null
                        ) {

                            sessionManager.saveToken(
                                body.token
                            )

                            sessionManager.setLoggedIn(
                                true
                            )

                            body.user?.let {

                                getSharedPreferences(
                                    "sourceform_user",
                                    MODE_PRIVATE
                                )
                                    .edit()
                                    .putString(
                                        "name",
                                        it.name
                                    )
                                    .putString(
                                        "email",
                                        it.email
                                    )
                                    .apply()
                            }

                            Toast.makeText(

                                this@LoginActivity,

                                "Signup Successful",

                                Toast.LENGTH_SHORT

                            ).show()

                            startActivity(

                                Intent(

                                    this@LoginActivity,

                                    MainActivity::class.java
                                )
                            )

                            finish()
                        }

                    } else {

                        Toast.makeText(

                            this@LoginActivity,

                            response.message(),

                            Toast.LENGTH_LONG

                        ).show()
                    }
                }

            } catch (e: Exception) {

                withContext(
                    Dispatchers.Main
                ) {

                    progressBar.visibility =
                        View.GONE

                    Toast.makeText(

                        this@LoginActivity,

                        e.message,

                        Toast.LENGTH_LONG

                    ).show()
                }
            }
        }
    }

    // -----------------------------------------
    // LOGIN
    // -----------------------------------------

    private fun login() {

        val email =
            etEmail.text.toString().trim()

        val password =
            etPassword.text.toString().trim()
        if (
            email.isEmpty() ||
            password.isEmpty()
        ) {

            Toast.makeText(
                this,
                "Please enter email and password",
                Toast.LENGTH_LONG
            ).show()

            return
        }
        progressBar.visibility =
            View.VISIBLE

        CoroutineScope(
            Dispatchers.IO
        ).launch {

            try {

                val response =

                    RetrofitClient.api.login(

                        LoginRequest(
                            email,
                            password
                        )
                    )

                withContext(
                    Dispatchers.Main
                ) {

                    progressBar.visibility =
                        View.GONE

                    if (
                        response.isSuccessful
                    ) {

                        val body =
                            response.body()

                        if (
                            body != null
                            &&
                            body.success
                            &&
                            body.token != null
                        ) {

                            sessionManager.saveToken(
                                body.token
                            )

                            sessionManager.setLoggedIn(
                                true
                            )
                            body.user?.let {

                                getSharedPreferences(
                                    "sourceform_user",
                                    MODE_PRIVATE
                                )
                                    .edit()
                                    .putString(
                                        "name",
                                        it.name
                                    )
                                    .putString(
                                        "email",
                                        it.email
                                    )
                                    .apply()
                            }
                            Toast.makeText(

                                this@LoginActivity,

                                "Login Successful",

                                Toast.LENGTH_SHORT

                            ).show()

                            startActivity(

                                Intent(

                                    this@LoginActivity,

                                    MainActivity::class.java
                                )
                            )

                            finish()
                        }

                    } else {

                        Toast.makeText(

                            this@LoginActivity,

                            response.message(),

                            Toast.LENGTH_LONG

                        ).show()
                    }
                }

            } catch (e: Exception) {

                withContext(
                    Dispatchers.Main
                ) {

                    progressBar.visibility =
                        View.GONE

                    Toast.makeText(

                        this@LoginActivity,

                        e.message,

                        Toast.LENGTH_LONG

                    ).show()
                }
            }
        }
    }

    // -----------------------------------------
    // GOOGLE LOGIN
    // -----------------------------------------

    private fun signInGoogle() {

        launcher.launch(
            googleSignInClient.signInIntent
        )
    }

    // -----------------------------------------
    // GITHUB LOGIN
    // -----------------------------------------

    private fun githubLogin() {

        val provider =
            OAuthProvider.newBuilder(
                "github.com"
            )

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
                sessionManager.setLoggedIn(true)
                saveOAuthUser()
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

    // -----------------------------------------
    // FACEBOOK LOGIN
    // -----------------------------------------

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

    // -----------------------------------------
    // FACEBOOK TOKEN
    // -----------------------------------------

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
                    sessionManager.setLoggedIn(true)
                    saveOAuthUser()
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

                        "Facebook Auth Failed",

                        Toast.LENGTH_LONG

                    ).show()
                }
            }
    }

    // -----------------------------------------
    // GOOGLE RESULT
    // -----------------------------------------

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
                            sessionManager.setLoggedIn(true)
                            saveOAuthUser()
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
    private fun saveOAuthUser() {

        auth.currentUser?.let { user ->

            getSharedPreferences(
                "sourceform_user",
                MODE_PRIVATE
            )
                .edit()
                .putString(
                    "name",
                    user.displayName ?: "OAuth User"
                )
                .putString(
                    "email",
                    user.email ?: "Unknown User"
                )
                .apply()
        }
    }
}
