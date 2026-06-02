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

    private var backendURL = ""

    private val isEmulator: Boolean by lazy {

        Build.FINGERPRINT.contains("generic")
                ||
                Build.MODEL.contains("Emulator")
    }

    // -----------------------------------------
    // QR Scanner
    // -----------------------------------------

    private val qrLauncher =
        registerForActivityResult(
            ScanContract()
        ) { result ->

            if (result.contents != null) {

                backendURL =
                    result.contents
                        .trim()
                        .removeSuffix("/") + "/"

                RetrofitClient.setBaseUrl(
                    backendURL
                )

                updateConnectionStatus(true)

                Toast.makeText(
                    this,
                    "Backend Connected",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    // -----------------------------------------
    // File Picker
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
    // onCreate
    // -----------------------------------------

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        initializeViews()

        initializeButtons()

        setupValidation()

        setupFocusEffects()

        backendURL =

            if (isEmulator) {

                "http://10.0.2.2:5000/"

            } else {

                "http://172.22.8.136:5000/"
            }

        RetrofitClient.setBaseUrl(
            backendURL
        )

        updateConnectionStatus(true)

        validateFields()
    }

    // -----------------------------------------
    // Initialize Views
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
    // Initialize Buttons
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
    // Focus Effects
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

                    view.animate()
                        .scaleX(1.02f)
                        .scaleY(1.02f)
                        .setDuration(120)
                        .start()

                } else {

                    view.setBackgroundResource(
                        R.drawable.edittext_bg
                    )

                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(120)
                        .start()
                }
            }
        }
    }

    // -----------------------------------------
    // Validation Logic
    // -----------------------------------------

    private fun setupValidation() {

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

                    if (words > 140) {

                        tvWordCount.setTextColor(
                            Color.RED
                        )

                    } else {

                        tvWordCount.setTextColor(
                            Color.parseColor("#B3B3B3")
                        )
                    }

                    validateFields()
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {}
            }
        )
    }

    // -----------------------------------------
    // Validate Fields
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
                    phone.all { it.isDigit() }
                    &&
                    words <= 140
                    &&
                    selectedFileUri != null

        btnSubmit.isEnabled = valid

        btnSubmit.alpha =
            if (valid) 1f else 0.5f
    }

    // -----------------------------------------
    // Connection Status
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
    // Upload Logic
    // -----------------------------------------
    private fun validateAndUpload() {

        successCard.visibility =
            View.GONE

        btnSubmit.isEnabled = false

        btnSubmit.text =
            "Uploading..."

        progressBar.visibility =
            View.VISIBLE

        CoroutineScope(Dispatchers.IO)
            .launch {

                try {

                    // ---------------------------------
                    // GET FIREBASE TOKEN
                    // ---------------------------------

                    val firebaseUser =

                        FirebaseAuth
                            .getInstance()
                            .currentUser

                    if (firebaseUser == null) {

                        runOnUiThread {

                            progressBar.visibility =
                                View.GONE

                            btnSubmit.isEnabled =
                                true

                            btnSubmit.text =
                                "Submit Form"

                            Toast.makeText(
                                this@MainActivity,
                                "User not authenticated",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        return@launch
                    }

                    val tokenResult =

                        firebaseUser
                            .getIdToken(false)
                            .await()

                    val firebaseToken =
                        tokenResult.token

                    if (firebaseToken == null) {

                        runOnUiThread {

                            progressBar.visibility =
                                View.GONE

                            btnSubmit.isEnabled =
                                true

                            btnSubmit.text =
                                "Submit Form"

                            Toast.makeText(
                                this@MainActivity,
                                "Failed to get auth token",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        return@launch
                    }

                    // ---------------------------------
                    // FILE
                    // ---------------------------------

                    val file =
                        uriToFile(selectedFileUri!!)

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

                    // ---------------------------------
                    // API REQUEST
                    // ---------------------------------

                    val response =

                        RetrofitClient
                            .getClient()
                            .uploadForm(

                                "Bearer $firebaseToken",

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

                                AnimationUtils.loadAnimation(
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
    // Clear Form
    // -----------------------------------------

    private fun clearForm() {

        etName.text.clear()

        etEmail.text.clear()

        etPhone.text.clear()

        etDescription.text.clear()

        tvFileName.text =
            "No file selected"

        tvEmailStatus.text = ""

        tvPhoneStatus.text = ""

        tvWordCount.text =
            "0 / 140 words"

        selectedFileUri = null
    }

    // -----------------------------------------
    // Query Filename
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
    // URI → File
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
