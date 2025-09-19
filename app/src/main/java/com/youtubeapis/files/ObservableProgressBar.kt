package com.youtubeapis.files

import android.content.Context
import android.util.AttributeSet
import android.widget.ProgressBar

class ObservableProgressBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ProgressBar(context, attrs) {

    interface OnProgressChangeListener {
        fun onProgressChanged(progress: Int)
    }

    private var listener: OnProgressChangeListener? = null

    fun setOnProgressChangeListener(l: OnProgressChangeListener) {
        listener = l
    }

    override fun setProgress(progress: Int) {
        super.setProgress(progress)
        listener?.onProgressChanged(progress)
    }
}
