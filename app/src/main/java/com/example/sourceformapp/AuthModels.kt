package com.example.sourceformapp

// -------------------------------------
// SIGNUP REQUEST
// -------------------------------------

data class SignupRequest(

    val name: String,

    val email: String,

    val password: String
)

// -------------------------------------
// LOGIN REQUEST
// -------------------------------------

data class LoginRequest(

    val email: String,

    val password: String
)

// -------------------------------------
// USER RESPONSE
// -------------------------------------

data class UserData(

    val id: String,

    val name: String,

    val email: String
)

// -------------------------------------
// AUTH RESPONSE
// -------------------------------------

data class AuthResponse(

    val success: Boolean,

    val token: String?,

    val user: UserData?,

    val message: String?
)
