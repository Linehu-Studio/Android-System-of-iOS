package com.linehu.asi.data.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Shader
import android.util.DisplayMetrics
import com.linehu.asi.core.designsystem.BackdropImages
import com.linehu.asi.core.util.scaledBlur
import com.linehu.asi.data.settings.SettingsRepository
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Built-in wallpapers: named vertical gradients rendered at screen size. */
data class GradientWallpaper(val name: String, val colors: IntArray) {
    override fun equals(other: Any?) = other is GradientWallpaper && other.name == name
    override fun hashCode() = name.hashCode()
}

val BUILTIN_WALLPAPERS = listOf(
    GradientWallpaper("午夜", intArrayOf(0xFF05050F.toInt(), 0xFF1B2735.toInt(), 0xFF090A0F.toInt())),
    GradientWallpaper("暮光", intArrayOf(0xFF355C7D.toInt(), 0xFF6C5B7B.toInt(), 0xFFC06C84.toInt())),
    GradientWallpaper("极光", intArrayOf(0xFF0B486B.toInt(), 0xFF4BB3FD.toInt(), 0xFF7BE0AD.toInt())),
    GradientWallpaper("日落", intArrayOf(0xFFFF7E5F.toInt(), 0xFFFEB47B.toInt())),
    GradientWallpaper("墨黑", intArrayOf(0xFF101010.toInt(), 0xFF2E2E2E.toInt())),
    GradientWallpaper("海洋", intArrayOf(0xFF000428.toInt(), 0xFF004E92.toInt())),
)

/**
 * Owns the current backdrop (regular + pre-blurred) and wallpaper
 * persistence. `wallpaperId == 0` mirrors the system wallpaper; 1..N picks
 * a built-in gradient (which can also be pushed to the system).
 */
class WallpaperRepository(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
) {

    val backdropFlow: Flow<BackdropImages> = settingsRepository.settings
        .map { it.wallpaperId }
        .distinctUntilChanged()
        .map { id -> loadBackdrop(id) }

    suspend fun loadBackdrop(wallpaperId: Int): BackdropImages = withContext(Dispatchers.IO) {
        val bitmap = when {
            wallpaperId in 1..BUILTIN_WALLPAPERS.size ->
                renderGradient(BUILTIN_WALLPAPERS[wallpaperId - 1])
            else -> systemWallpaper()
        }
        BackdropImages(
            regular = bitmap?.asImageBitmap(),
            blurred = bitmap?.scaledBlur()?.asImageBitmap(),
        )
    }

    fun systemWallpaper(): Bitmap? = runCatching {
        WallpaperManager.getInstance(context).drawable?.let { drawable ->
            val metrics = context.resources.displayMetrics
            val bmp = Bitmap.createBitmap(
                metrics.widthPixels.coerceAtLeast(1),
                metrics.heightPixels.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888,
            )
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bmp
        }
    }.getOrNull()

    private fun renderGradient(wallpaper: GradientWallpaper): Bitmap {
        val metrics: DisplayMetrics = context.resources.displayMetrics
        val w = metrics.widthPixels.coerceAtLeast(1)
        val h = metrics.heightPixels.coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = android.graphics.Paint().apply {
            shader = LinearGradient(
                0f, 0f, w * 0.4f, h.toFloat(),
                wallpaper.colors,
                null,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        return bmp
    }

    /** Push the current wallpaper to the system (SET_WALLPAPER, normal perm). */
    fun applyToSystem(wallpaperId: Int): Boolean = runCatching {
        if (wallpaperId in 1..BUILTIN_WALLPAPERS.size) {
            val bmp = renderGradient(BUILTIN_WALLPAPERS[wallpaperId - 1])
            WallpaperManager.getInstance(context).setBitmap(bmp)
            true
        } else false
    }.getOrDefault(false)
}
