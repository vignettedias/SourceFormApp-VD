package com.example.sourceformapp

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import java.io.File
import java.io.FileOutputStream
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

import com.google.firebase.auth.FirebaseAuth
class MainActivity : AppCompatActivity() {

    // -----------------------------------------
    // INPUTS
    // -----------------------------------------

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etDescription: EditText

    // -----------------------------------------
    // STATUS TEXT
    // -----------------------------------------

    private lateinit var tvFileName: TextView
    private lateinit var tvWordCount: TextView
    private lateinit var tvEmailStatus: TextView
    private lateinit var tvPhoneStatus: TextView

    private lateinit var tvConnectionStatus: TextView

    private lateinit var tvUserEmail: TextView
    private lateinit var tvAuthProvider: TextView

    // -----------------------------------------
    // BUTTONS
    // -----------------------------------------

    private lateinit var btnSubmit: Button
    private lateinit var btnLogout: Button

    // -----------------------------------------
    // UI
    // -----------------------------------------

    private lateinit var progressBar: ProgressBar

    private lateinit var successCard: View

    private lateinit var statusDot: View

    // -----------------------------------------
    // FILE
    // -----------------------------------------

    private var selectedFileUri: Uri? = null

    // -----------------------------------------
    // FIREBASE
    // -----------------------------------------

    private val auth =
        FirebaseAuth.getInstance()
    private lateinit var sessionManager: SessionManager
    // -----------------------------------------
    // FILE PICKER
    // -----------------------------------------

    private val filePicker =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->

            uri?.let {

                selectedFileUri = it

                tvFileName.text =
                    queryFileName(it)

                validateFields()
            }
        }

    // -----------------------------------------
    // FORM INACTIVITY TIMER
    // -----------------------------------------
    private val inactivityHandler =
        android.os.Handler(
            android.os.Looper.getMainLooper()
        )

    private val logoutRunnable =
        Runnable {

            performLogout()
        }

    private fun resetInactivityTimer() {

        inactivityHandler.removeCallbacks(
            logoutRunnable
        )

        inactivityHandler.postDelayed(

            logoutRunnable,

            300000
        )
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)
        sessionManager =
            SessionManager(this)
        setContentView(
            R.layout.activity_main
        )

        initializeViews()

        initializeButtons()

        setupValidation()

        loadUserInformation()

        updateConnectionStatus(true)

        validateFields()
        resetInactivityTimer()
    }
    // -----------------------------------------
// INITIALIZE VIEWS
// -----------------------------------------

    private fun initializeViews() {

        etName =
            findViewById(R.id.etName)

        etEmail =
            findViewById(R.id.etEmail)

        etPhone =
            findViewById(R.id.etPhone)

        etDescription =
            findViewById(R.id.etDescription)

        tvFileName =
            findViewById(R.id.tvFileName)

        tvWordCount =
            findViewById(R.id.tvWordCount)

        tvEmailStatus =
            findViewById(R.id.tvEmailStatus)

        tvPhoneStatus =
            findViewById(R.id.tvPhoneStatus)

        tvConnectionStatus =
            findViewById(R.id.tvConnectionStatus)

        tvUserEmail =
            findViewById(R.id.tvUserEmail)

        tvAuthProvider =
            findViewById(R.id.tvAuthProvider)

        btnSubmit =
            findViewById(R.id.btnSubmit)

        btnLogout =
            findViewById(R.id.btnLogout)

        progressBar =
            findViewById(R.id.progressBar)

        successCard =
            findViewById(R.id.successCard)

        statusDot =
            findViewById(R.id.statusDot)
    }
    // -----------------------------------------
// INITIALIZE BUTTONS
// -----------------------------------------

    private fun initializeButtons() {

        val btnChooseFile =
            findViewById<Button>(
                R.id.btnChooseFile
            )

        btnChooseFile.setOnClickListener {

            filePicker.launch("*/*")
        }

        btnSubmit.setOnClickListener {

            validateAndUpload()
        }

        btnLogout.setOnClickListener {

            performLogout()
        }
    }
    // -----------------------------------------
// USER INFORMATION
// -----------------------------------------

    private fun loadUserInformation() {

        val firebaseUser =
            auth.currentUser

        if (firebaseUser != null) {

            tvUserEmail.text =
                firebaseUser.email ?: "Unknown User"

            val provider =

                firebaseUser.providerData
                    .lastOrNull()
                    ?.providerId

            tvAuthProvider.text =

                when (provider) {

                    "google.com" -> "Google"
                    "github.com" -> "GitHub"
                    "facebook.com" -> "Facebook"
                    else -> "Firebase"
                }

        } else {

            val prefs =

                getSharedPreferences(
                    "sourceform_user",
                    MODE_PRIVATE
                )

            tvUserEmail.text =

                prefs.getString(
                    "email",
                    "Unknown User"
                )

            tvAuthProvider.text =
                "JWT Authentication"
        }
    }
    // -----------------------------------------
// CONNECTION STATUS
// -----------------------------------------

    private fun updateConnectionStatus(
        connected: Boolean
    ) {

        if (connected) {

            tvConnectionStatus.text =
                " Cloud Connected"

            statusDot.backgroundTintList =

                ColorStateList.valueOf(
                    Color.parseColor("#22C55E")
                )

        } else {

            tvConnectionStatus.text =
                " Cloud Offline"

            statusDot.backgroundTintList =

                ColorStateList.valueOf(
                    Color.RED
                )
        }
    }
    // -----------------------------------------
// VALIDATION
// -----------------------------------------

    private fun setupValidation() {

        etEmail.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    val email =
                        s.toString()

                    val valid =

                        android.util.Patterns
                            .EMAIL_ADDRESS
                            .matcher(email)
                            .matches()

                    if (email.isEmpty()) {

                        tvEmailStatus.text = ""

                    } else if (valid) {

                        tvEmailStatus.text =
                            "✓ Valid Email Address"

                        tvEmailStatus.setTextColor(
                            Color.parseColor("#22C55E")
                        )

                    } else {

                        tvEmailStatus.text =
                            "✗ Invalid Email Address"

                        tvEmailStatus.setTextColor(
                            Color.RED
                        )
                    }

                    validateFields()
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {
                }
            }
        )

        etPhone.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    val phone =
                        s.toString()

                    if (phone.isEmpty()) {

                        tvPhoneStatus.text = ""

                    } else if (

                        phone.length == 10
                        &&
                        phone.all {
                            it.isDigit()
                        }

                    ) {

                        tvPhoneStatus.text =
                            "✓ Valid Mobile Number"

                        tvPhoneStatus.setTextColor(
                            Color.parseColor("#22C55E")
                        )

                    } else {

                        tvPhoneStatus.text =
                            "✗ Must contain 10 digits"

                        tvPhoneStatus.setTextColor(
                            Color.RED
                        )
                    }

                    validateFields()
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {
                }
            }
        )

        etDescription.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    val words =

                        s.toString()
                            .trim()
                            .split("\\s+".toRegex())
                            .filter {
                                it.isNotEmpty()
                            }
                            .size

                    tvWordCount.text =
                        "Description Length: $words / 140 words"

                    if (words > 140) {

                        tvWordCount.setTextColor(
                            Color.RED
                        )

                    } else {

                        tvWordCount.setTextColor(
                            Color.parseColor("#9CA3AF")
                        )
                    }

                    validateFields()
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {
                }
            }
        )
    }
    // -----------------------------------------
// FORM VALIDATION
// -----------------------------------------

    private fun validateFields() {

        val name =
            etName.text.toString().trim()

        val email =
            etEmail.text.toString().trim()

        val phone =
            etPhone.text.toString().trim()

        val description =
            etDescription.text.toString().trim()

        val words =

            description
                .split("\\s+".toRegex())
                .filter {
                    it.isNotEmpty()
                }
                .size

        val valid =

            name.isNotEmpty()
                    &&
                    name.length <= 50
                    &&
                    android.util.Patterns
                        .EMAIL_ADDRESS
                        .matcher(email)
                        .matches()
                    &&
                    phone.length == 10
                    &&
                    phone.all {
                        it.isDigit()
                    }
                    &&
                    words <= 140
                    &&
                    selectedFileUri != null

        btnSubmit.isEnabled = valid

        btnSubmit.alpha =
            if (valid) 1f else 0.5f
    }

    // -----------------------------------------
// FIREBASE UPLOAD
// -----------------------------------------
    private fun validateAndUpload() {

        val fileUri =
            selectedFileUri ?: return

        progressBar.visibility =
            View.VISIBLE

        btnSubmit.isEnabled =
            false

        btnSubmit.text =
            "Uploading..."

        CoroutineScope(
            Dispatchers.IO
        ).launch {

            try {

                val tempFile =

                    File(
                        cacheDir,
                        queryFileName(fileUri)
                    )

                contentResolver
                    .openInputStream(fileUri)
                    ?.use { input ->

                        FileOutputStream(
                            tempFile
                        ).use { output ->

                            input.copyTo(output)
                        }
                    }

                val requestFile =

                    tempFile
                        .asRequestBody(
                            "*/*"
                                .toMediaTypeOrNull()
                        )

                val multipartFile =

                    MultipartBody.Part
                        .createFormData(

                            "file",

                            tempFile.name,

                            requestFile
                        )

                val nameBody =

                    etName.text
                        .toString()
                        .toRequestBody(
                            "text/plain"
                                .toMediaTypeOrNull()
                        )

                val emailBody =

                    etEmail.text
                        .toString()
                        .toRequestBody(
                            "text/plain"
                                .toMediaTypeOrNull()
                        )

                val phoneBody =

                    etPhone.text
                        .toString()
                        .toRequestBody(
                            "text/plain"
                                .toMediaTypeOrNull()
                        )

                val descriptionBody =

                    etDescription.text
                        .toString()
                        .toRequestBody(
                            "text/plain"
                                .toMediaTypeOrNull()
                        )

                var authToken =
                    sessionManager.getToken()

                if (authToken == null) {

                    val firebaseUser =
                        FirebaseAuth
                            .getInstance()
                            .currentUser

                    if (firebaseUser != null) {

                        val tokenResult =
                            firebaseUser
                                .getIdToken(false)
                                .await()

                        authToken =
                            tokenResult.token
                    }
                }

                if (authToken == null) {

                    withContext(
                        Dispatchers.Main
                    ) {

                        progressBar.visibility =
                            View.GONE

                        btnSubmit.isEnabled =
                            true

                        btnSubmit.text =
                            "Submit Form"

                        Toast.makeText(

                            this@MainActivity,

                            "Authentication Required",

                            Toast.LENGTH_LONG

                        ).show()
                    }

                    return@launch
                }

                val response =

                    RetrofitClient.api.uploadForm(

                        authToken =
                            "Bearer $authToken",

                        name =
                            nameBody,

                        email =
                            emailBody,

                        phone =
                            phoneBody,

                        description =
                            descriptionBody,

                        file =
                            multipartFile
                    )

                withContext(
                    Dispatchers.Main
                ) {

                    progressBar.visibility =
                        View.GONE

                    btnSubmit.isEnabled =
                        true

                    btnSubmit.text =
                        "Submit Form"

                    if (
                        response.isSuccessful
                    ) {

                        successCard.visibility =
                            View.VISIBLE

                        updateConnectionStatus(
                            true
                        )

                        Toast.makeText(

                            this@MainActivity,

                            "Secure Upload Successful",

                            Toast.LENGTH_LONG

                        ).show()

                        clearForm()
                        android.os.Handler(
                            mainLooper
                        ).postDelayed({

                            performLogout()

                        }, 5000)
                    } else {

                        Toast.makeText(

                            this@MainActivity,

                            "Upload Failed: ${response.code()}",

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

                    btnSubmit.isEnabled =
                        true

                    btnSubmit.text =
                        "Submit Form"

                    updateConnectionStatus(
                        false
                    )

                    Toast.makeText(

                        this@MainActivity,

                        e.message,

                        Toast.LENGTH_LONG

                    ).show()
                }
            }
        }
    }
    // -----------------------------------------
// CLEAR FORM
// -----------------------------------------

    private fun clearForm() {

        etName.text.clear()

        etEmail.text.clear()

        etPhone.text.clear()

        etDescription.text.clear()

        tvEmailStatus.text = ""

        tvPhoneStatus.text = ""

        tvWordCount.text =
            "Description Length: 0 / 140 words"

        tvFileName.text =
            "📄 No Secure Document Selected"

        selectedFileUri = null

        validateFields()
    }
    // -----------------------------------------
// FILE NAME
// -----------------------------------------

    private fun queryFileName(
        uri: Uri
    ): String {

        var name =
            "document"

        val cursor =

            contentResolver.query(

                uri,

                null,

                null,

                null,

                null
            )

        cursor?.use {

            val index =

                it.getColumnIndex(
                    OpenableColumns.DISPLAY_NAME
                )

            if (

                it.moveToFirst()
                &&
                index != -1

            ) {

                name =
                    it.getString(index)
            }
        }

        return name
    }

    override fun onUserInteraction() {

        super.onUserInteraction()

        resetInactivityTimer()
    }
    private fun performLogout() {

        sessionManager.clearSession()

        auth.signOut()

        GoogleSignIn
            .getClient(
                this,
                GoogleSignInOptions.Builder(
                    GoogleSignInOptions.DEFAULT_SIGN_IN
                ).build()
            )
            .signOut()

        com.facebook.login.LoginManager
            .getInstance()
            .logOut()
        getSharedPreferences(
            "sourceform_user",
            MODE_PRIVATE
        )
            .edit()
            .clear()
            .apply()
        startActivity(

            Intent(
                this,
                LoginActivity::class.java
            )
        )

        finish()
    }
}