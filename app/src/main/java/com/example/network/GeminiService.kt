package com.example.network

import com.example.BuildConfig
import com.example.model.Architecture
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

object GeminiService {
    
    suspend fun explainCode(code: String, architecture: Architecture): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "AI Tutor offline. Enter a real Gemini API Key in AI Studio Secrets to unlock the AI tutor!"
        }

        val prompt = """
            You are "Prof. Assembly", an elite, friendly computer architect and Assembly language tutor.
            The user wants you to explain their code.
            
            Architecture Context: ${architecture.displayName} (${architecture.tag})
            
            Assembly Code to Explain:
            ```assembly
            $code
            ```
            
            Instructions:
            1. Provide a short, ultra-clear explanation of what this program does in plain English.
            2. Break down the code line-by-line, explaining the registers and operations.
            3. Give a pro tip for optimization or architectural nuance (e.g., pipeline hazard for ARM, real-mode memory segmentation for OS, thread divergence or SIMD layout for GPU).
            
            Formatting:
            - Keep it concise, professional, and visually engaging.
            - Use bullet points and bold key terms.
            - Speak directly and encouragement-focused. Avoid long preambles.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "No response from AI Tutor. Please check your assembly code and try again!"
        } catch (e: Exception) {
            "AI Tutor unavailable: ${e.localizedMessage ?: "Network connection error"}. Please ensure you have configured your GEMINI_API_KEY."
        }
    }

    suspend fun troubleshootCode(code: String, errorMsg: String, architecture: Architecture): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "AI Tutor offline. Enter a real Gemini API Key in AI Studio Secrets to unlock the AI tutor!"
        }

        val prompt = """
            You are "Prof. Assembly", an elite, friendly computer architect and Assembly language tutor.
            The user is running code on the simulator and encountered an error.
            
            Architecture: ${architecture.displayName}
            Error Encountered: $errorMsg
            
            Assembly Code:
            ```assembly
            $code
            ```
            
            Provide:
            1. Why did this error occur on the ${architecture.displayName} architecture?
            2. Clear, exact correction steps or code replacement.
            3. Short encouraging advice.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "No response from AI Tutor."
        } catch (e: Exception) {
            "AI Tutor unavailable: ${e.localizedMessage ?: "Network connection error"}."
        }
    }
}
