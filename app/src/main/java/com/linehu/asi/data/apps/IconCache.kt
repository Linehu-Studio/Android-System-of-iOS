package com.linehu.asi.data.apps

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.linehu.asi.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * LRU cache of rendered squircle icons. Keys are `appKey/iconVersion` so a
 * package update invalidates its icon automatically.
 */
class IconCache(private val context: Context) {

    private class Holder {
        val lru = object : LruCache<String, ImageBitmap>(64) {}
    }

    private val cache = Holder()

    suspend fun iconFor(app: AppInfo, sizePx: Int): ImageBitmap? = withContext(Dispatchers.Default) {
        val key = "${app.key}/$sizePx"
        cache.lru.get(key) ?: run {
            SquircleIconFactory.render(context, app.componentName, sizePx)?.let { bmp ->
                val image = bmp.asImageBitmap()
                cache.lru.put(key, image)
                image
            }
        }
    }

    fun clear() = cache.lru.evictAll()
}
