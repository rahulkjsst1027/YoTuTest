package com.youtubeapis.wheelView

import android.animation.Animator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Created by Rahul Kumar on 30/05/2025.
 */
internal class WheelView : View {
    var tag = WheelView::class.java.name
    private var range = RectF()
    private var archPaint: Paint? = null
    private var textPaint: Paint? = null
    private var padding = 0
    private var radius = 0
    private var center = 0
    private var mWheelBackground = 0
    private var mImagePadding = 0
    private var textSize = 30f
    private var mWheelItems: List<WheelItem>? = null
    private var mOnLuckyWheelReachTheTarget: OnLuckyWheelReachTheTarget? = null
    private var onRotationListener: OnRotationListener? = null

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        initComponents()
    }


    private fun initComponents() {
        //arc paint object
        archPaint = Paint()
        archPaint!!.isAntiAlias = true
        archPaint!!.isDither = true
        //text paint object
        textPaint = Paint()
       // textPaint!!.color = Color.WHITE
        textPaint!!.isAntiAlias = true
        textPaint!!.isDither = true
        //textPaint!!.textSize = 30f
        //rect rang of the arc
        range = RectF(
            padding.toFloat(),
            padding.toFloat(),
            (padding + radius).toFloat(),
            (padding + radius).toFloat()
        )
    }

    /**
     * Get the angele of the target
     *
     * @return Number of angle
     */
    private fun getAngleOfIndexTarget(target: Int): Float {
        return (360f / mWheelItems!!.size) * target
    }

    /**
     * Function to set wheel background
     *
     * @param wheelBackground Wheel background color
     */
    fun setWheelBackgroundWheel(wheelBackground: Int) {
        mWheelBackground = wheelBackground
        invalidate()
    }

    fun setItemsImagePadding(imagePadding: Int) {
        mImagePadding = imagePadding
        invalidate()
    }

    fun setTextSize(size: Float) {
        textSize = size
        invalidate()
    }

    /**
     * Function to set wheel listener
     *
     * @param onLuckyWheelReachTheTarget target reach listener
     */
    fun setWheelListener(onLuckyWheelReachTheTarget: OnLuckyWheelReachTheTarget?) {
        mOnLuckyWheelReachTheTarget = onLuckyWheelReachTheTarget
    }

    /**
     * Function to add wheels items
     *
     * @param wheelItems Wheels model item
     */
    fun addWheelItems(wheelItems: List<WheelItem>?) {
        mWheelItems = wheelItems
        invalidate()
    }

    /**
     * Function to draw wheel background
     *
     * @param canvas Canvas of draw
     */
    private fun drawWheelBackground(canvas: Canvas) {
        val backgroundPainter = Paint()
        backgroundPainter.isAntiAlias = true
        backgroundPainter.isDither = true
        backgroundPainter.color = mWheelBackground
        canvas.drawCircle(center.toFloat(), center.toFloat(), center.toFloat(), backgroundPainter)
    }

    /**
     * Function to draw image in the center of arc
     *
     * @param canvas    Canvas to draw
     * @param tempAngle Temporary angle
     * @param bitmap    Bitmap to draw
     */
    private fun drawImage(canvas: Canvas, tempAngle: Float, bitmap: Bitmap) {
        //get every arc img width and angle
        val imgWidth = (radius / mWheelItems!!.size) - mImagePadding
        val angle = ((tempAngle + 360.0 / mWheelItems!!.size / 2) * Math.PI / 180).toFloat()
        //calculate x and y
        val x = (center + radius.toDouble() / 2 / 2 * cos(angle.toDouble())).toInt()
        val y = (center + radius.toDouble() / 2 / 2 * sin(angle.toDouble())).toInt()
        //create arc to draw
        val rect = Rect(x - imgWidth / 2, y - imgWidth / 2, x + imgWidth / 2, y + imgWidth / 2)
        //rotate main bitmap
        val px = rect.exactCenterX()
        val py = rect.exactCenterY()
        val matrix = Matrix()
        matrix.postTranslate(-bitmap.width.toFloat() / 2, -bitmap.height.toFloat() / 2)
        matrix.postRotate(tempAngle + 120)
        matrix.postTranslate(px, py)
        canvas.drawBitmap(
            bitmap,
            matrix,
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG or Paint.FILTER_BITMAP_FLAG)
        )
        Log.d(tag, bitmap.width.toString() + " : " + bitmap.height)
        matrix.reset()
    }


    /**
     * Function to draw text below image
     *
     * @param canvas     Canvas to draw
     * @param tempAngle  Temporary angle
     * @param sweepAngle current index angle
     * @param text       string to show
     */
    private fun drawText(canvas: Canvas, tempAngle: Float, sweepAngle: Float, text: String, item: WheelItem?) {
        val path = Path()
        path.addArc(range, tempAngle, sweepAngle)
        textPaint?.color = item?.textColor ?: Color.WHITE
        textPaint?.textSize = item?.textSize ?: textSize
        val textWidth = textPaint!!.measureText(text)
        val hOffset = (radius * Math.PI / mWheelItems!!.size / 2 - textWidth / 2).toInt()
        val vOffset = (radius / 2 / 3) - 3
        canvas.drawTextOnPath(text, path, hOffset.toFloat(), vOffset.toFloat(), textPaint!!)
    }

    /**
     * Function to rotate wheel to target
     *
     * @param target target number
     */
    fun rotateWheelToTarget(target: Int) {
        val wheelItemCenter =
            270 - getAngleOfIndexTarget(target) + (360 / mWheelItems!!.size).toFloat() / 2
        val defaultRotationTime = 9000
        animate().setInterpolator(DecelerateInterpolator())
            .setDuration(defaultRotationTime.toLong())
            .rotation((360 * 15) + wheelItemCenter)
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (mOnLuckyWheelReachTheTarget != null) {
                        mOnLuckyWheelReachTheTarget!!.onReachTarget(target)
                    }
                    if (onRotationListener != null) {
                        onRotationListener!!.onFinishRotation()
                    }
                    clearAnimation()
                }

                override fun onAnimationCancel(animation: Animator) {
                }

                override fun onAnimationRepeat(animation: Animator) {
                }
            })
            .start()
    }

    /**
     * Function to rotate to zero angle
     *
     * @param target target to reach
     */
    fun resetRotationLocationToZeroAngle(target: Int) {
        animate().setDuration(0)
            .rotation(0f).setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                }

                override fun onAnimationEnd(animation: Animator) {
                    rotateWheelToTarget(target)
                    clearAnimation()
                }

                override fun onAnimationCancel(animation: Animator) {
                }

                override fun onAnimationRepeat(animation: Animator) {
                }
            })
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        initComponents()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawWheelBackground(canvas)
        //initComponents()

        var tempAngle = 0f
        val sweepAngle = 360f / mWheelItems!!.size

        for (i in mWheelItems!!.indices) {
            val item = mWheelItems!![i]
            archPaint!!.color = item.color
            canvas.drawArc(range, tempAngle, sweepAngle, true, archPaint!!)
            drawImage(canvas, tempAngle, item.bitmap)
            drawText(
                canvas,
                tempAngle,
                sweepAngle,
                item.text ?: "",
                item
            )
            tempAngle += sweepAngle
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val width = min(measuredWidth.toDouble(), measuredHeight.toDouble()).toInt()
        val defaultPadding = 5
        padding = if (paddingLeft == 0) defaultPadding else paddingLeft
        radius = width - padding * 2
        center = width / 2
        setMeasuredDimension(width, width)
    }

    fun setOnRotationListener(onRotationListener: OnRotationListener?) {
        this.onRotationListener = onRotationListener
    }
}