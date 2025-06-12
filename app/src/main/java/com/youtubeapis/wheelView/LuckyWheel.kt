package com.youtubeapis.wheelView

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.annotation.AttrRes
import com.youtubeapis.R
import kotlin.math.abs

/**
 * Created by Rahul Kumar on 30/05/2025.
 */
class LuckyWheel : FrameLayout, OnTouchListener, OnRotationListener {
    private var wheelView: WheelView? = null
    private var arrow: ImageView? = null
    private var target = -1
    private var isRotate = false

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initComponent()
        applyAttribute(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, @AttrRes defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initComponent()
        applyAttribute(attrs)
    }

    private fun initComponent() {
        inflate(context, R.layout.lucky_wheel_layout, this)
        setOnTouchListener(this)
        wheelView = findViewById(R.id.wv_main_wheel)
        wheelView!!.setOnRotationListener(this)
        arrow = findViewById(R.id.iv_arrow)
    }

    /**
     * Function to add items to wheel items
     *
     * @param wheelItems Wheel items
     */
    fun addWheelItems(wheelItems: List<WheelItem>?) {
        wheelView!!.addWheelItems(wheelItems)
    }

    @SuppressLint("CustomViewStyleable")
    fun applyAttribute(attrs: AttributeSet?) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.app, 0, 0)
        try {
            val backgroundColor = typedArray.getColor(R.styleable.app_background_color, Color.GREEN)
            val arrowImage = typedArray.getResourceId(R.styleable.app_arrow_image, R.drawable.arrow)
            val imagePadding = typedArray.getDimensionPixelSize(R.styleable.app_image_padding, 0)
            val textSize = typedArray.getDimension(R.styleable.app_text_size, 30f)
            wheelView!!.setWheelBackgroundWheel(backgroundColor)
            wheelView!!.setItemsImagePadding(imagePadding)
            wheelView!!.setTextSize(textSize)
            arrow!!.setImageResource(arrowImage)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        typedArray.recycle()
    }

    /**
     * Function to set lucky wheel reach the target listener
     *
     * @param onLuckyWheelReachTheTarget Lucky wheel listener
     */
    fun setLuckyWheelReachTheTarget(onLuckyWheelReachTheTarget: OnLuckyWheelReachTheTarget) {
        wheelView!!.setWheelListener(onLuckyWheelReachTheTarget)
    }

    /**
     * @param target target to rotate before swipe
     */
    fun setTarget(target: Int) {
        this.target = target
    }

    /**
     * Function to rotate wheel to degree
     *
     * @param number Number to rotate
     */
    fun rotateWheelTo(number: Int) {
        isRotate = true
        wheelView!!.resetRotationLocationToZeroAngle(number)
    }

    val SWIPE_DISTANCE_THRESHOLD: Int = 100
    var x1: Float = 0f
    var x2: Float = 0f
    var y1: Float = 0f
    var y2: Float = 0f
    var dx: Float = 0f
    var dy: Float = 0f

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (target < 0 || isRotate) {
            return false
        }

        when (event.action) {
            (MotionEvent.ACTION_DOWN) -> {
                x1 = event.x
                y1 = event.y
            }

            MotionEvent.ACTION_UP -> {
                x2 = event.x
                y2 = event.y
                dx = x2 - x1
                dy = y2 - y1
                if (abs(dx.toDouble()) > abs(dy.toDouble())) {
                    if (dx < 0 && abs(dx.toDouble()) > SWIPE_DISTANCE_THRESHOLD) rotateWheelTo(
                        target
                    )
                } else {
                    if (dy > 0 && abs(dy.toDouble()) > SWIPE_DISTANCE_THRESHOLD) rotateWheelTo(
                        target
                    )
                }
            }

            else -> return true
        }
        return true
    }

    override fun onFinishRotation() {
        isRotate = false
    }
}
