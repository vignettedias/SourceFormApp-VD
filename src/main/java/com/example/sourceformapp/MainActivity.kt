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

import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

import java.util.UUID

class MainActivityFirebase : AppCompatActivity() {

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

    private val storage =
        FirebaseStorage.getInstance()

    private val firestore =
        FirebaseFirestore.getInstance()

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

        loadUserInformation()

        updateConnectionStatus(true)

        validateFields()
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

            auth.signOut()

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
// USER INFORMATION
// -----------------------------------------

    private fun loadUserInformation() {

        val user =
            auth.currentUser

        tvUserEmail.text =
            user?.email
                ?: "Unknown User"

        val provider =

            user?.providerData
                ?.lastOrNull()
                ?.providerId

        tvAuthProvider.text =

            when (provider) {

                "google.com" ->
                    "Google"

                "github.com" ->
                    "GitHub"

                "facebook.com" ->
                    "Facebook"

                "password" ->
                    "Email / Password"

                else ->
                    "Firebase"
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
                ) {}
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

        successCard.visibility =
            View.GONE

        progressBar.visibility =
            View.VISIBLE

        btnSubmit.isEnabled =
            false

        btnSubmit.text =
            "Uploading..."

        val fileName =

            UUID.randomUUID()
                .toString() +

                    "_" +

                    queryFileName(fileUri)

        val storageRef =

            storage.reference
                .child(
                    "documents/$fileName"
                )

        storageRef.putFile(fileUri)

            .addOnSuccessListener {

                storageRef.downloadUrl

                    .addOnSuccessListener {

                            downloadUrl ->

                        val uploadData =

                            hashMapOf(

                                "name" to
                                        etName.text
                                            .toString(),

                                "email" to
                                        etEmail.text
                                            .toString(),

                                "phone" to
                                        etPhone.text
                                            .toString(),

                                "description" to
                                        etDescription.text
                                            .toString(),

                                "fileUrl" to
                                        downloadUrl.toString(),

                                "fileName" to
                                        fileName,

                                "uploadedBy" to
                                        auth.currentUser?.uid,

                                "uploadedEmail" to
                                        auth.currentUser?.email,

                                "timestamp" to
                                        System.currentTimeMillis(),

                                "status" to
                                        "secured"
                            )

                        firestore

                            .collection(
                                "uploads"
                            )

                            .add(
                                uploadData
                            )

                            .addOnSuccessListener {

                                progressBar.visibility =
                                    View.GONE

                                btnSubmit.isEnabled =
                                    true

                                btnSubmit.text =
                                    "Submit Form"

                                successCard.visibility =
                                    View.VISIBLE

                                updateConnectionStatus(
                                    true
                                )

                                Toast.makeText(

                                    this,

                                    "Secure Upload Successful",

                                    Toast.LENGTH_LONG

                                ).show()

                                clearForm()
                            }

                            .addOnFailureListener {

                                progressBar.visibility =
                                    View.GONE

                                btnSubmit.isEnabled =
                                    true

                                btnSubmit.text =
                                    "Submit Form"

                                Toast.makeText(

                                    this,

                                    "Metadata Save Failed",

                                    Toast.LENGTH_LONG

                                ).show()
                            }
                    }
            }

            .addOnFailureListener {

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

                    this,

                    "File Upload Failed",

                    Toast.LENGTH_LONG

                ).show()
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
}