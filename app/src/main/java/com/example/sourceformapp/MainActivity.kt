package com.example.sourceformapp

import kotlinx.coroutines.tasks.await

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

import com.google.firebase.auth.FirebaseAuth

import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    // -----------------------------------------
    // UI
    // -----------------------------------------

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etDescription: EditText

    private lateinit var tvFileName: TextView
    private lateinit var tvWordCount: TextView
    private lateinit var tvEmailStatus: TextView
    private lateinit var tvPhoneStatus: TextView

    private lateinit var btnSubmit: Button
    private lateinit var btnLogout: Button

    private lateinit var progressBar: ProgressBar

    private lateinit var successCard: View

    private lateinit var tvConnectionStatus: TextView
    private lateinit var statusDot: View

    private var selectedFileUri: Uri? = null

    // -----------------------------------------
    // BACKEND URL
    // -----------------------------------------

    private lateinit var backendURL: String

    // -----------------------------------------
    // QR SCANNER
    // -----------------------------------------

    private val qrLauncher =
        registerForActivityResult(
            ScanContract()
        ) { result ->

            if (result.contents != null) {

                val scannedURL =
                    result.contents

                backendURL =
                    scannedURL

                RetrofitClient.setBaseUrl(
                    backendURL
                )

                Toast.makeText(

                    this,

                    "QR Backend Connected",

                    Toast.LENGTH_LONG

                ).show()

                updateConnectionStatus(true)
            }
        }

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
    // ON CREATE
    // -----------------------------------------

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_main
        )

        initializeViews()

        initializeButtons()

        setupValidation()

        setupFocusEffects()

        setupBackendConnection()

        validateFields()
    }

    // -----------------------------------------
    // BACKEND CONNECTION
    // -----------------------------------------

    private fun setupBackendConnection() {

        val isEmulator =

            Build.FINGERPRINT.contains("generic")
                    ||
                    Build.MODEL.contains("google_sdk")
                    ||
                    Build.MODEL.contains("Emulator")
                    ||
                    Build.MODEL.contains("Android SDK")

        backendURL =

            if (isEmulator) {

                // Emulator backend

                "http://10.0.2.2:5000/"

            } else {

                // Real device backend

                "http://10.56.39.223:5000/"
            }

        RetrofitClient.setBaseUrl(
            backendURL
        )

        Toast.makeText(

            this,

            "Backend Connected",

            Toast.LENGTH_SHORT

        ).show()

        updateConnectionStatus(true)
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

        btnSubmit =
            findViewById(R.id.btnSubmit)

        btnLogout =
            findViewById(R.id.btnLogout)

        progressBar =
            findViewById(R.id.progressBar)

        successCard =
            findViewById(R.id.successCard)

        tvConnectionStatus =
            findViewById(R.id.tvConnectionStatus)

        statusDot =
            findViewById(R.id.statusDot)
    }

    // -----------------------------------------
    // BUTTONS
    // -----------------------------------------

    private fun initializeButtons() {

        val btnChooseFile =
            findViewById<Button>(
                R.id.btnChooseFile
            )

        val btnScanQR =
            findViewById<Button>(
                R.id.btnScanQR
            )

        btnChooseFile.setOnClickListener {

            filePicker.launch("*/*")
        }

        btnScanQR.setOnClickListener {

            openQRScanner()
        }

        btnSubmit.setOnClickListener {

            validateAndUpload()
        }

        btnLogout.setOnClickListener {

            FirebaseAuth
                .getInstance()
                .signOut()

            GoogleSignIn
                .getClient(

                    this,

                    GoogleSignInOptions.Builder(
                        GoogleSignInOptions.DEFAULT_SIGN_IN
                    ).build()
                )

                .signOut()

            val sessionManager =
                SessionManager(this)

            sessionManager.clearSession()

            startActivity(

                Intent(
                    this,
                    LoginActivity::class.java
                )
            )

            finish()
        }
    }

    // -----------------------------------------
    // FOCUS EFFECTS
    // -----------------------------------------

    private fun setupFocusEffects() {

        val fields = listOf(

            etName,
            etEmail,
            etPhone,
            etDescription
        )

        fields.forEach { field ->

            field.setOnFocusChangeListener {

                    view,
                    hasFocus ->

                if (hasFocus) {

                    view.setBackgroundResource(
                        R.drawable.edittext_focused
                    )

                } else {

                    view.setBackgroundResource(
                        R.drawable.edittext_bg
                    )
                }
            }
        }
    }

    // -----------------------------------------
    // VALIDATION
    // -----------------------------------------

    private fun setupValidation() {

        // EMAIL

        etEmail.addTextChangedListener(

            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}

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
                            "Valid Email"

                        tvEmailStatus.setTextColor(
                            Color.parseColor("#22C55E")
                        )

                    } else {

                        tvEmailStatus.text =
                            "Invalid Email"

                        tvEmailStatus.setTextColor(
                            Color.RED
                        )
                    }

                    validateFields()
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {}
            }
        )

        // PHONE

        etPhone.addTextChangedListener(

            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}

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
                        phone.all { it.isDigit() }

                    ) {

                        tvPhoneStatus.text =
                            "Valid Phone Number"

                        tvPhoneStatus.setTextColor(
                            Color.parseColor("#22C55E")
                        )

                    } else {

                        tvPhoneStatus.text =
                            "Phone must contain 10 digits"

                        tvPhoneStatus.setTextColor(
                            Color.RED
                        )
                    }

                    validateFields()
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {}
            }
        )

        // DESCRIPTION

        etDescription.addTextChangedListener(

            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}

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
                        "$words / 140 words"

                    validateFields()
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {}
            }
        )
    }

    // -----------------------------------------
    // VALIDATE FIELDS
    // -----------------------------------------

    private fun validateFields() {

        val valid =

            etName.text.toString()
                .trim()
                .isNotEmpty()

                    &&

                    android.util.Patterns
                        .EMAIL_ADDRESS
                        .matcher(
                            etEmail.text.toString()
                        )
                        .matches()

                    &&

                    etPhone.text.toString()
                        .length == 10

                    &&

                    selectedFileUri != null

        btnSubmit.isEnabled =
            valid

        btnSubmit.alpha =
            if (valid) 1f else 0.5f
    }

    // -----------------------------------------
    // CONNECTION STATUS
    // -----------------------------------------

    private fun updateConnectionStatus(
        connected: Boolean
    ) {

        if (connected) {

            tvConnectionStatus.text =
                " Backend Connected"

            statusDot.backgroundTintList =

                ColorStateList.valueOf(
                    Color.parseColor("#22C55E")
                )

        } else {

            tvConnectionStatus.text =
                " Backend Offline"

            statusDot.backgroundTintList =

                ColorStateList.valueOf(
                    Color.RED
                )
        }
    }

    // -----------------------------------------
    // QR SCANNER
    // -----------------------------------------

    private fun openQRScanner() {

        val options =
            ScanOptions()

        options.setPrompt(
            "Scan Backend QR"
        )

        options.setBeepEnabled(true)

        options.setOrientationLocked(true)

        qrLauncher.launch(options)
    }

    // -----------------------------------------
    // UPLOAD
    // -----------------------------------------

    private fun validateAndUpload() {

        successCard.visibility =
            View.GONE

        btnSubmit.isEnabled =
            false

        btnSubmit.text =
            "Uploading..."

        progressBar.visibility =
            View.VISIBLE

        CoroutineScope(Dispatchers.IO)
            .launch {

                try {

                    val sessionManager =
                        SessionManager(
                            this@MainActivity
                        )

                    val jwtToken =
                        sessionManager.getToken()

                    val firebaseUser =
                        FirebaseAuth
                            .getInstance()
                            .currentUser

                    val authHeader =

                        if (jwtToken != null) {

                            "Bearer $jwtToken"

                        } else {

                            val tokenResult =
                                firebaseUser
                                    ?.getIdToken(false)
                                    ?.await()

                            "Bearer ${tokenResult?.token}"
                        }

                    val file =
                        uriToFile(
                            selectedFileUri!!
                        )

                    val requestFile =
                        file.asRequestBody(
                            "*/*"
                                .toMediaTypeOrNull()
                        )

                    val multipart =
                        MultipartBody.Part
                            .createFormData(

                                "file",

                                file.name,

                                requestFile
                            )

                    val response =

                        RetrofitClient
                            .getClient()

                            .uploadForm(

                                authHeader,

                                etName.text.toString()
                                    .toRequestBody(
                                        "text/plain"
                                            .toMediaTypeOrNull()
                                    ),

                                etEmail.text.toString()
                                    .toRequestBody(
                                        "text/plain"
                                            .toMediaTypeOrNull()
                                    ),

                                etPhone.text.toString()
                                    .toRequestBody(
                                        "text/plain"
                                            .toMediaTypeOrNull()
                                    ),

                                etDescription.text.toString()
                                    .toRequestBody(
                                        "text/plain"
                                            .toMediaTypeOrNull()
                                    ),

                                multipart
                            )

                    runOnUiThread {

                        progressBar.visibility =
                            View.GONE

                        btnSubmit.isEnabled =
                            true

                        btnSubmit.text =
                            "Submit Form"

                        if (response.isSuccessful) {

                            updateConnectionStatus(true)

                            successCard.visibility =
                                View.VISIBLE

                            val animation =

                                AnimationUtils
                                    .loadAnimation(

                                        this@MainActivity,

                                        R.anim.success_popup
                                    )

                            successCard.startAnimation(
                                animation
                            )

                            Toast.makeText(

                                this@MainActivity,

                                "Upload Successful",

                                Toast.LENGTH_SHORT

                            ).show()

                            clearForm()

                        } else {

                            updateConnectionStatus(false)

                            Toast.makeText(

                                this@MainActivity,

                                "Upload Failed",

                                Toast.LENGTH_LONG

                            ).show()
                        }

                        validateFields()
                    }

                } catch (e: Exception) {

                    runOnUiThread {

                        updateConnectionStatus(false)

                        progressBar.visibility =
                            View.GONE

                        btnSubmit.isEnabled =
                            true

                        btnSubmit.text =
                            "Submit Form"

                        Toast.makeText(

                            this@MainActivity,

                            e.message,

                            Toast.LENGTH_LONG

                        ).show()

                        validateFields()
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

        tvFileName.text =
            "No file selected"

        tvWordCount.text =
            "0 / 140 words"

        selectedFileUri = null
    }

    // -----------------------------------------
    // FILE NAME
    // -----------------------------------------

    private fun queryFileName(
        uri: Uri
    ): String {

        var name =
            "unknown_file"

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

    // -----------------------------------------
    // URI TO FILE
    // -----------------------------------------

    private fun uriToFile(
        uri: Uri
    ): File {

        val file =
            File(
                cacheDir,
                queryFileName(uri)
            )

        val input =
            contentResolver
                .openInputStream(uri)

        val output =
            FileOutputStream(file)

        input!!.copyTo(output)

        input.close()

        output.close()

        return file
    }
}
