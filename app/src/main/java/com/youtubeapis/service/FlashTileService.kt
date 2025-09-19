package com.youtubeapis.service


import android.content.Context
import android.graphics.drawable.Icon
import android.hardware.camera2.CameraManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.youtubeapis.R

class FlashTileService : TileService() {

    private var isTorchOn = false

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        isTorchOn = !isTorchOn
        toggleFlashlight(isTorchOn)
        updateTileState()
    }

    private fun updateTileState() {
        qsTile?.apply {
            state = if (isTorchOn) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = if (isTorchOn) "Torch" else "Torch"

            icon = if (isTorchOn) {
                Icon.createWithResource(this@FlashTileService, R.drawable.ic_flash_on_yellow)
            } else {
                Icon.createWithResource(this@FlashTileService, R.drawable.ic_flash_off_white)
            }
            updateTile()
        }
    }

    private fun toggleFlashlight(enable: Boolean) {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = cameraManager.cameraIdList[0] // back camera
            cameraManager.setTorchMode(cameraId, enable)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
