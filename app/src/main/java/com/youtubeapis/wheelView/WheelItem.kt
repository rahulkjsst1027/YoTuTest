package com.youtubeapis.wheelView

import android.graphics.Bitmap

data class WheelItem(
    var color: Int,
    var bitmap: Bitmap,
    var text: String?,
    var textColor: Int? = null,
    var textSize: Float? = null,
)


