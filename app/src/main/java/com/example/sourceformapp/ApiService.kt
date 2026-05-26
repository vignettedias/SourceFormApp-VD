package com.example.sourceformapp

import okhttp3.MultipartBody
import okhttp3.RequestBody

import retrofit2.Response

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {

    // -------------------------------------
    // SIGNUP
    // -------------------------------------

    @POST("auth/signup")

    suspend fun signup(

        @Body
        request: SignupRequest

    ): Response<AuthResponse>

    // -------------------------------------
    // LOGIN
    // -------------------------------------

    @POST("auth/login")

    suspend fun login(

        @Body
        request: LoginRequest

    ): Response<AuthResponse>

    // -------------------------------------
    // SECURE FORM UPLOAD
    // -------------------------------------

    @Multipart
    @POST("api/submit")

    suspend fun uploadForm(

        @Header("Authorization")
        authToken: String,

        @Part("name")
        name: RequestBody,

        @Part("email")
        email: RequestBody,

        @Part("phone")
        phone: RequestBody,

        @Part("description")
        description: RequestBody,

        @Part
        file: MultipartBody.Part

    ): Response<Unit>
}
