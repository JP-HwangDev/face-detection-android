package com.example.camera

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import com.google.mlkit.vision.face.Face
import kotlin.math.ceil

class GraphicOverlay(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private val graphics: MutableList<Graphic> = mutableListOf()
    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 5.0f
    }
    var cameraPreviewWidth: Float = 0f
    var cameraPreviewHeight: Float = 0f


    fun add(graphic: Graphic) {
        graphics.add(graphic)
        postInvalidate()
    }

    fun clear() {
        graphics.clear()
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (graphic in graphics) {
            graphic.draw(canvas)
        }
    }

    abstract class Graphic(protected val overlay: GraphicOverlay) {
        abstract fun draw(canvas: Canvas)

        protected fun translateX(x: Float): Float {
            // 세로 모드에서는 카메라의 이미지(가로)가 ML Kit에서 세로로 처리됩니다.
            // 따라서 카메라의 높이를 이미지 너비로, 너비를 이미지 높이로 사용합니다.
            val imageWidth = overlay.cameraPreviewHeight
            val imageHeight = overlay.cameraPreviewWidth

            if (imageWidth == 0f || imageHeight == 0f || overlay.width == 0 || overlay.height == 0) {
                return 0f
            }

            val viewWidth = overlay.width.toFloat()
            val viewHeight = overlay.height.toFloat()

            // PreviewView의 ScaleType.FILL_CENTER에 맞춰 배율을 계산합니다.
            val scaleFactor = kotlin.math.max(viewWidth / imageWidth, viewHeight / imageHeight)

            val postScaleWidth = imageWidth * scaleFactor
            val xOffset = (postScaleWidth - viewWidth) / 2f

            val translatedX = x * scaleFactor - xOffset

            // 전면 카메라는 미러링됩니다.
            return viewWidth - translatedX
        }

        protected fun translateY(y: Float): Float {
            val imageWidth = overlay.cameraPreviewHeight
            val imageHeight = overlay.cameraPreviewWidth

            if (imageWidth == 0f || imageHeight == 0f || overlay.width == 0 || overlay.height == 0) {
                return 0f
            }

            val viewWidth = overlay.width.toFloat()
            val viewHeight = overlay.height.toFloat()

            val scaleFactor = kotlin.math.max(viewWidth / imageWidth, viewHeight / imageHeight)

            val postScaleHeight = imageHeight * scaleFactor
            val yOffset = (postScaleHeight - viewHeight) / 2f

            return y * scaleFactor - yOffset
        }
    }

    class FaceGraphic(overlay: GraphicOverlay, private val face: Face) : Graphic(overlay) {
        override fun draw(canvas: Canvas) {
            val boundingBox = face.boundingBox

            // ML Kit에서 감지한 좌표를 GraphicOverlay View의 좌표로 변환합니다.
            val left = translateX(boundingBox.left.toFloat())
            val top = translateY(boundingBox.top.toFloat())
            val right = translateX(boundingBox.right.toFloat())
            val bottom = translateY(boundingBox.bottom.toFloat())

            // 변환된 좌표를 사용하여 사각형을 그립니다.
            // translateX가 미러링을 처리하므로 left와 right를 교환할 필요가 없습니다.
            // 하지만 translateX의 결과로 right가 left보다 작아질 수 있으므로, min/max를 사용하여 올바른 사각형을 그립니다.
            canvas.drawRect(right, top, left, bottom, overlay.paint)
        }
    }
}
