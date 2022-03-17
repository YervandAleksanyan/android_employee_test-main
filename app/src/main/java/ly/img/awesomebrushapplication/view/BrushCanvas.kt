package ly.img.awesomebrushapplication.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.PixelCopy
import android.view.View
import android.view.Window
import androidx.annotation.ColorInt
import ly.img.awesomebrushapplication.view.data.Brush
import ly.img.awesomebrushapplication.view.data.FifoQueue
import ly.img.awesomebrushapplication.view.data.Point
import ly.img.awesomebrushapplication.view.interfaces.OnHistoryChangeListener
import ly.img.awesomebrushapplication.view.util.scaleToFit

class BrushCanvas @JvmOverloads constructor(
    context: Context?, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), OnHistoryChangeListener {

    private var currentColor = Color.BLACK

    init {
        setWillNotDraw(false)
    }

    private var path = Path()
    private val brushData = Brush().apply {
        onHistoryChangedListener = this@BrushCanvas
    }

    private val brushStrokePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = currentColor
        strokeWidth = 10.0f
    }
    private val historyBrushPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        color = currentColor
        strokeWidth = 10.0f
    }
    private var sourceBitmap: Bitmap? = null
    private var previewBitmap: Bitmap? = null
    private var sourceRect: Rect = Rect()
    private var scaledRect: Rect = Rect()
    private var region = Region()

    var isDrawingEnabled = false
        private set

    private val pointsQueue = FifoQueue<Point?>(4)

    fun initSourceBitmap(bitmap: Bitmap) {
        sourceBitmap = bitmap
        sourceRect = Rect(0, 0, bitmap.width, bitmap.height)
        scaleIfNecessary()
        isDrawingEnabled = true
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        scaleIfNecessary()
    }

    private fun scaleIfNecessary() {
        sourceBitmap?.let {
            previewBitmap = it.scaleToFit(measuredWidth, measuredHeight)
            val centerHeight = (measuredHeight - previewBitmap!!.height) / 2
            val centerWidth = (measuredWidth - previewBitmap!!.width) / 2
            scaledRect.apply {
                left = centerWidth
                top = centerHeight
                right = previewBitmap!!.width + centerWidth
                bottom = previewBitmap!!.height + centerHeight
            }
            region = Region(scaledRect)
        }
    }

    override fun onChange(param: Brush.Params) {
        brushStrokePaint.apply {
            color = param.color
            strokeWidth = param.thickness
        }
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean = event?.let {
        if (!isDrawingEnabled) return@let false
        val pointX = event.x
        val pointY = event.y
        if (!region.contains(pointX.toInt(), pointY.toInt())) return@let false
        val currentPoint = Point(pointX, pointY)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                pointsQueue.add(null)
                pointsQueue.add(currentPoint)
                pointsQueue.add(currentPoint)
                drawBezier(true, currentPoint)
            }
            MotionEvent.ACTION_MOVE -> {
                pointsQueue.add(currentPoint)
                drawBezier(false, currentPoint)
            }
            MotionEvent.ACTION_UP -> {
                pointsQueue.add(currentPoint)
                pointsQueue.add(null)
                drawBezier(false, currentPoint)
                // save the current drawn path to the history
                brushData.add(path)
                path = Path()
                pointsQueue.clear()
            }
            else -> return@let false
        }
        postInvalidate()
        return@let true
    } ?: false

    private fun drawBezier(isFirstPoint: Boolean, currentPoint: Point) {
        if (isFirstPoint) {
            path.moveTo(currentPoint.x, currentPoint.y)
        } else {
            val listOfPoint = pointsQueue.asList()

            val point: Point = if (listOfPoint.size >= 3) listOfPoint[2] ?: currentPoint else currentPoint
            val lastPoint: Point = listOfPoint[1] ?: currentPoint
            val nextPoint: Point? = if (listOfPoint.size >= 4) listOfPoint[3] else currentPoint
            val beforeLastPoint: Point = listOfPoint[0] ?: lastPoint

            val pointDx: Float
            val pointDy: Float
            if (nextPoint == null ) {
                pointDx = (point.x - lastPoint.x) / SMOOTH_VAL
                pointDy = (point.y - lastPoint.y) / SMOOTH_VAL
            } else {
                pointDx = (nextPoint.x - lastPoint.x) / SMOOTH_VAL
                pointDy = (nextPoint.y - lastPoint.y) / SMOOTH_VAL
            }

            val lastPointDx = (point.x - beforeLastPoint.x) / SMOOTH_VAL
            val lastPointDy = (point.y - beforeLastPoint.y) / SMOOTH_VAL

            path.cubicTo(
                lastPoint.x + lastPointDx,
                lastPoint.y + lastPointDy,
                point.x - pointDx,
                point.y - pointDy,
                point.x,
                point.y
            )
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        previewBitmap?.let { bitmap ->
            canvas?.drawBitmap(bitmap, null, scaledRect, brushStrokePaint)
        }

        brushData.getCurrentHistory().forEach {
            historyBrushPaint.apply {
                color = it.param.color
                strokeWidth = it.param.thickness
            }
            canvas?.drawPath(it.path, historyBrushPaint)
        }
        canvas?.drawPath(path, brushStrokePaint)

    }

    fun setColor(@ColorInt colorInt: Int) {
        brushData.changeColor(colorInt)
    }

    fun setHardness(strokeWidth: Float) {
        brushData.changeHardness(strokeWidth)
    }

    fun undo() {
        brushData.undo()
    }

    fun redo() {
        brushData.redo()
    }

    fun clearAll() {
        brushData.clearAll()
    }

    fun captureView(window: Window, bitmapCallback: (Bitmap) -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val safeBitmap = sourceBitmap ?: return
            val bitmap = Bitmap.createBitmap(safeBitmap.width, safeBitmap.height, Bitmap.Config.ARGB_8888)
            val location = IntArray(2)
            getLocationInWindow(location)
            PixelCopy.request(window,
                Rect(location[0] + scaledRect.left, location[1] + scaledRect.top, scaledRect.right, location[1] + scaledRect.bottom),
                bitmap,
                {
                    if (it == PixelCopy.SUCCESS) {
                        bitmapCallback.invoke(bitmap)
                    }
                },
                Handler(Looper.getMainLooper()) )
        } else {
            val tBitmap = Bitmap.createBitmap(
                width, height, Bitmap.Config.RGB_565
            )
            val canvas = Canvas(tBitmap)
            draw(canvas)
            canvas.setBitmap(null)
            bitmapCallback.invoke(tBitmap)
        }
    }

    companion object {
        private const val SMOOTH_VAL = 3
    }
}