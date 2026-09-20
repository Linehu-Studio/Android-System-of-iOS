package com.linehu.asi.feature.photos

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.linehu.asi.core.designsystem.IosStatusBar
import com.linehu.asi.core.designsystem.IosText

/** Photos: permission flow → MediaStore grid → fullscreen zoomable viewer. */
@Composable
fun PhotosScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    fun hasAllImagesPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun hasPartialPermission(): Boolean =
        Build.VERSION.SDK_INT >= 34 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            ) == PackageManager.PERMISSION_GRANTED

    var granted by remember { mutableStateOf(hasAllImagesPermission() || hasPartialPermission()) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted = hasAllImagesPermission() || hasPartialPermission() }

    if (!granted) {
        LaunchedEffect(Unit) {
            val permissions = if (Build.VERSION.SDK_INT >= 34) {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                )
            } else if (Build.VERSION.SDK_INT >= 33) {
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            permissionLauncher.launch(permissions)
        }
        Column(
            modifier
                .fillMaxSize()
                .background(Color(0xFF1C1C1E))
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            IosText("需要照片访问权限", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            IosText("请在系统弹窗中允许（可选“仅选中的照片”）", color = Color(0x99EBEBF5), fontSize = 14.sp)
        }
        return
    }

    val photos by produceState(initialValue = emptyList<PhotoItem>()) {
        value = queryPhotos(context)
    }
    var viewerIndex by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier
            .fillMaxSize()
            .background(Color(0xFF1C1C1E)),
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            IosStatusBar(contentColor = Color.White)
            IosText(
                "照片",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                Modifier.weight(1f).padding(horizontal = 3.dp),
            ) {
                items(photos, key = { it.id }) { photo ->
                    AsyncImage(
                        model = photo.uri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .padding(1.5.dp)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { viewerIndex = photos.indexOf(photo) },
                    )
                }
            }
        }
        viewerIndex?.let { index ->
            PhotoViewer(
                photos = photos,
                initialIndex = index,
                onClose = { viewerIndex = null },
            )
        }
    }
}

data class PhotoItem(val id: Long, val uri: android.net.Uri)

private fun queryPhotos(context: android.content.Context): List<PhotoItem> {
    val collection = if (Build.VERSION.SDK_INT >= 29) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }
    val photos = mutableListOf<PhotoItem>()
    runCatching {
        context.contentResolver.query(
            collection,
            arrayOf(MediaStore.Images.Media._ID),
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                photos += PhotoItem(id, ContentUris.withAppendedId(collection, id))
            }
        }
    }
    return photos
}

/** Fullscreen viewer: horizontal page + pinch-to-zoom. */
@Composable
private fun PhotoViewer(
    photos: List<PhotoItem>,
    initialIndex: Int,
    onClose: () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { photos.size })
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            ZoomableImage(photos[page].uri)
        }
        // Top bar
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "关闭",
                tint = Color.White,
                modifier = Modifier
                    .clickable(onClick = onClose)
                    .padding(8.dp),
            )
            Spacer(Modifier.weight(1f))
            IosText(
                "${pagerState.currentPage + 1} / ${photos.size}",
                color = Color.White,
                fontSize = 15.sp,
            )
            Spacer(Modifier.width(24.dp))
        }
    }
}

@Composable
private fun ZoomableImage(uri: android.net.Uri) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 6f)
        if (scale > 1f) {
            offsetX += panChange.x
            offsetY += panChange.y
        } else {
            offsetX = 0f
            offsetY = 0f
        }
    }
    AsyncImage(
        model = uri,
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offsetX,
                translationY = offsetY,
            )
            .transformable(transformState)
            .clickable {
                if (scale > 1f) {
                    scale = 1f; offsetX = 0f; offsetY = 0f
                }
            },
    )
}
