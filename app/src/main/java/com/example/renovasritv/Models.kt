package com.example.renovasritv

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class GalleryItem(
    val id: String? = null,
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
    val views: Int = 0
)

typealias ProjectItem = GalleryItem

@Serializable
data class Category(
    val id: String? = null,
    val name: String
)

@Serializable
data class HomeCuration(
    val id: Long? = null, // Reverted to Long? as DB is returning Integers (causing JSON decode error)
    @SerialName("image_url") val imageUrl: String,
    val caption: String? = null,
    @SerialName("order_index") val orderIndex: Int = 0,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("target_id") val targetId: String? = null
)

@Serializable
data class MenuBackground(
    val id: Long? = null,
    @SerialName("menu_key")
    val menuKey: String,
    @SerialName("image_url")
    val imageUrl: String,
    @SerialName("overlay_opacity")
    val overlayOpacity: String? = "Standard (60%)"
)

@Serializable
data class UIConfig(
    val key: String,
    val value: String,
    val type: String,
    val screen: String? = null,
    @SerialName("page_id") val pageId: String? = null,
    @SerialName("parent_key") val parentKey: String? = null,
    @SerialName("order_index") val orderIndex: Int = 0,
    @SerialName("font_size") val fontSize: String? = null,
    @SerialName("font_weight") val fontWeight: String? = null,
    @SerialName("font_style") val fontStyle: String? = null,
    @SerialName("font_color") val fontColor: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("component_type") val componentType: String? = null,
    @SerialName("feature_group") val featureGroup: String? = null,
    val description: String? = null
)

@Serializable
data class UIModule(
    val id: Long? = null,
    val key: String,
    val label: String,
    val icon: String,
    @SerialName("order_index") val orderIndex: Int = 0,
    @SerialName("is_active") val isActive: Boolean = true
)

@Serializable
data class Favorite(
    val id: Long? = null,
    @SerialName("item_id")
    val itemId: String
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

/**
 * Converts a design token string from the database into a resolution-aware [TextUnit].
 */
fun String?.toFontSize(screenWidthDp: Int): TextUnit {
    val token = this?.split(" ")?.get(0)?.lowercase()
    val isSmallScreen = screenWidthDp < 800
    
    return when (token) {
        "caption" -> if (isSmallScreen) 12.sp else 14.sp
        "body" -> if (isSmallScreen) 16.sp else 18.sp
        "subtitle" -> if (isSmallScreen) 20.sp else 24.sp
        "title" -> if (isSmallScreen) 24.sp else 32.sp
        "headline" -> if (isSmallScreen) 36.sp else 48.sp
        "display" -> if (isSmallScreen) 50.sp else 72.sp
        else -> 18.sp
    }
}

/**
 * Cascading Visibility Engine Helper.
 * Ensures that if a Parent (Area/Container) is inactive or missing, all children are hidden.
 */
fun Map<String, UIConfig>.isModuleVisible(key: String): Boolean {
    val config = this[key] 
    
    // If the key is not found in the map (e.g., disabled/removed from DB):
    // Structural areas (AREA_/CONTAINER_) MUST default to FALSE (Hidden).
    if (config == null) {
        val isStructural = key.startsWith("AREA_") || key.startsWith("CONTAINER_")
        return !isStructural
    }
    
    // If explicitly marked inactive in the database, hide it.
    if (!config.isActive) return false
    
    // Recursive check for Parent visibility.
    val parent = config.parentKey
    if (!parent.isNullOrBlank() && parent != key) {
        return isModuleVisible(parent)
    }

    return true
}

/**
 * Filter and Sort Configs by Page.
 */
fun Map<String, UIConfig>.getConfigsByPage(pageId: String): List<UIConfig> {
    return this.values
        .filter { it.pageId == pageId && it.isActive }
        .sortedBy { it.orderIndex }
}

/**
 * Converts an opacity token string from the database into a Float alpha value.
 */
fun String?.toOverlayOpacity(): Float {
    val token = this?.split(" ")?.get(0)?.lowercase()
    return when (token) {
        "faint" -> 0.2f
        "muted" -> 0.4f
        "standard" -> 0.6f
        "deep" -> 0.8f
        "opaque" -> 1.0f
        else -> 0.6f
    }
}
