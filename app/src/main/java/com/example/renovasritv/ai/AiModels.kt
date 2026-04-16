package com.example.renovasritv.ai

import kotlinx.serialization.Serializable

@Serializable
data class ClaudeMessage(
    val role: String,
    val content: String
)

@Serializable
data class ClaudeRequest(
    val model: String = "claude-3-opus-20240229",
    val max_tokens: Int = 1024,
    val messages: List<ClaudeMessage>,
    val system: String? = null
)

@Serializable
data class ClaudeResponse(
    val id: String,
    val content: List<ClaudeContent>,
    val model: String,
    val role: String
)

@Serializable
data class ClaudeContent(
    val text: String,
    val type: String
)

@Serializable
data class ImagePromptResponse(
    val prompt: String,
    val style: String,
    val mood: String,
    val materials_referenced: List<String>
)

// Gemini Image Generation Models (Simulated for Imagen 3 via Vertex/Google AI)
@Serializable
data class GeminiImageRequest(
    val instances: List<GeminiPromptInstance>,
    val parameters: GeminiImageParameters? = null
)

@Serializable
data class GeminiPromptInstance(
    val prompt: String
)

@Serializable
data class GeminiImageParameters(
    val sampleCount: Int = 1,
    val aspectRatio: String = "16:9"
)

@Serializable
data class GeminiImageResponse(
    val predictions: List<GeminiImagePrediction>
)

@Serializable
data class GeminiImagePrediction(
    val bytesBase64Encoded: String? = null,
    val mimeType: String? = null
)
