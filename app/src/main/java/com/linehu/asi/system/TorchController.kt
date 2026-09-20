package com.linehu.asi.system

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager

/** Flashlight toggle — free, no permission needed. */
class TorchController(private val context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    var on: Boolean = false
        private set

    fun toggle(): Boolean {
        val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return false
        return try {
            on = !on
            cameraManager.setTorchMode(cameraId, on)
            true
        } catch (t: Throwable) {
            on = false
            false
        }
    }
}
