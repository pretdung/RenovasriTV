package com.example.renovasritv.ai

import com.example.renovasritv.CalcEstimation
import com.example.renovasritv.MainViewModel
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import android.util.Log
import kotlinx.serialization.encodeToString

class ClaudeOrchestrator(private val viewModel: MainViewModel) {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    private val CLAUDE_API_URL = "https://api.anthropic.com/v1/messages"

    suspend fun generateImagePrompt(estimation: CalcEstimation): String? {
        val apiKey = viewModel.getDecryptedKey("Claude") ?: return null
        
        val systemPrompt = """
            You are 'The Architect', an AI interior design expert. 
            Your task is to convert renovation cost estimation data into a high-fidelity image generation prompt.
            The prompt should be optimized for Midjourney or Gemini Artist.
            Focus on lighting, textures of the chosen materials, and a professional architectural photography style.
            Return ONLY the final prompt string.
        """.trimIndent()

        val userMessage = """
            Generate an architectural visualization prompt for a renovation with these details:
            Dimensions: ${estimation.roomDimensions}
            Materials Used: ${estimation.surfacesJson}
            Estimated Quality Level: ${if (estimation.totalMax > 50000000) "Premium/Luxury" else "Standard/Modern"}
            
            Focus on creating a cohesive aesthetic based on these materials.
        """.trimIndent()

        return try {
            val response: HttpResponse = client.post(CLAUDE_API_URL) {
                header("x-api-key", apiKey)
                header("anthropic-version", "2023-06-01")
                contentType(ContentType.Application.Json)
                val request = ClaudeRequest(
                    system = systemPrompt,
                    messages = listOf(ClaudeMessage(role = "user", content = userMessage))
                )
                Log.d("ClaudeOrchestrator", "Request: ${Json.encodeToString(request)}")
                setBody(request)
            }

            if (response.status == HttpStatusCode.OK) {
                val claudeResponse: ClaudeResponse = response.body()
                claudeResponse.content.firstOrNull()?.text
            } else {
                Log.e("ClaudeOrchestrator", "Error: ${response.status} - ${response.bodyAsText()}")
                null
            }
        } catch (e: Exception) {
            Log.e("ClaudeOrchestrator", "Exception: ${e.message}")
            null
        }
    }
}
