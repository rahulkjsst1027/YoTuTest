package com.youtubeapis.stack

import android.content.Context
import android.util.DisplayMetrics
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class StackLayoutManager(context: Context) : LinearLayoutManager(context) {
    private val maxOffset: Float
    private var activePosition = -1
    private var currentTranslationY = 0f
    private val itemOffset: Int

    init {
        val metrics: DisplayMetrics = context.resources.displayMetrics
        maxOffset = metrics.heightPixels * 0.6f
        itemOffset = (60 * metrics.density).toInt()
        orientation = VERTICAL
    }

    fun setTranslation(position: Int, translation: Float) {
        currentTranslationY = translation.coerceIn(0f, maxOffset)
        activePosition = position
        requestLayout()
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
        super.onLayoutChildren(recycler, state)
        if (recycler == null || itemCount == 0) return

        for (i in 0 until childCount) {
            val child = getChildAt(i) ?: continue
            val position = getPosition(child)

            // Calculate base position with stacking
            val baseTop = paddingTop + (itemCount - position - 1) * itemOffset

            if (activePosition != -1 && position > activePosition) {
                // Apply swipe translation to cards below active card
                child.translationY = baseTop + currentTranslationY

                // Scale and alpha effects during swipe
                val progress = currentTranslationY / maxOffset
                child.scaleX = 1 - progress * 0.2f
                child.scaleY = 1 - progress * 0.2f
                child.alpha = 1 - progress * 0.3f
            } else {
                // Normal stacked position
                child.translationY = baseTop.toFloat()
                child.scaleX = 1f
                child.scaleY = 1f
                child.alpha = 1f
            }

            // Z-index for proper stacking
            child.translationZ = (itemCount - position).toFloat()
        }
    }
}





