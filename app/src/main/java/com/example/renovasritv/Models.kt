package com.example.renovasritv

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class ProjectItem(
    val id: String? = null,
    val title: String,
    val subtitle: String? = null,
    val status: String,
    @SerialName("status_color")
    val statusColor: String,
    @SerialName("image_url")
    val imageUrl: String
)

@Serializable
data class GalleryItem(
    val id: Long? = null,
    val title: String,
    val description: String? = null,
    val location: String,
    @SerialName("image_url")
    val imageUrl: String,
    @SerialName("media_gallery")
    val mediaGallery: List<String> = emptyList(),
    val category: String = "All",
    @SerialName("architect_name")
    val architectName: String? = null,
    val year: Int? = null,
    @SerialName("is_trending")
    val isTrending: Boolean = false,
    val views: Int = 0
)

@Serializable
data class Category(
    val id: String? = null,
    val name: String
)

@Serializable
data class MenuBackground(
    val id: Long? = null,
    @SerialName("menu_key")
    val menuKey: String,
    @SerialName("image_url")
    val imageUrl: String,
    @SerialName("overlay_opacity")
    val overlayOpacity: Float = 0.6f
)

@Serializable
data class UIConfig(
    val key: String,
    val value: String,
    val type: String,
    val screen: String? = null,
    @SerialName("font_size") val fontSize: Int? = null,
    @SerialName("font_weight") val fontWeight: String? = null,
    @SerialName("font_style") val fontStyle: String? = null,
    @SerialName("font_color") val fontColor: String? = null
)

@Serializable
data class Favorite(
    val id: Long? = null,
    @SerialName("item_id")
    val itemId: Long
)

@Serializable
data class ThemeColor(
    val key: String,
    @SerialName("color_hex")
    val colorHex: String
)

fun String.toComposeColor(): Color {
    return try {
        Color(android.graphics.Color.parseColor(this))
    } catch (e: Exception) {
        Color.Gray
    }
}

fun String?.toFontWeight(): androidx.compose.ui.text.font.FontWeight {
    return when (this?.lowercase()) {
        "thin" -> androidx.compose.ui.text.font.FontWeight.Thin
        "light" -> androidx.compose.ui.text.font.FontWeight.Light
        "medium" -> androidx.compose.ui.text.font.FontWeight.Medium
        "semibold" -> androidx.compose.ui.text.font.FontWeight.SemiBold
        "bold" -> androidx.compose.ui.text.font.FontWeight.Bold
        "extrabold" -> androidx.compose.ui.text.font.FontWeight.ExtraBold
        "black" -> androidx.compose.ui.text.font.FontWeight.Black
        else -> androidx.compose.ui.text.font.FontWeight.Normal
    }
}
