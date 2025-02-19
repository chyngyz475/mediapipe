/*
 * Copyright 2023 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.mediapipe.examples.imagesegmenter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

class OverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {
    companion object {

        const val ALPHA_COLOR = 128
    }

    private var scaleBitmap: Bitmap? = null
    private var runningMode: RunningMode = RunningMode.IMAGE
    private var listener: OverlayViewListener? = null

    fun setOnOverlayViewListener(listener: OverlayViewListener) {
        this.listener = listener
    }

    fun clear() {
        scaleBitmap = null
        invalidate()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        scaleBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }
    }

    fun setRunningMode(runningMode: RunningMode) {
        this.runningMode = runningMode
    }

    fun setResults(
        byteBuffer: ByteBuffer,
        outputWidth: Int,
        outputHeight: Int
    ) {
        val colorLabel = HashSet<Pair<String, Int>>()
        // Create the mask bitmap with colors and the set of detected labels.
        val pixels = IntArray(byteBuffer.capacity())
        for (i in pixels.indices) {
            val index = byteBuffer.get(i).toInt() % 20
            val color = ImageSegmenterHelper.labelColors[index].toAlphaColor()
            pixels[i] = color
        }
        val image = Bitmap.createBitmap(
            pixels,
            outputWidth,
            outputHeight,
            Bitmap.Config.ARGB_8888
        )

        val scaleFactor = when (runningMode) {
            RunningMode.IMAGE,
            RunningMode.VIDEO -> {
                min(width * 1f / outputWidth, height * 1f / outputHeight)
            }
            RunningMode.LIVE_STREAM -> {
                // PreviewView is in FILL_START mode. So we need to scale up the
                // landmarks to match with the size that the captured images will be
                // displayed.
                max(width * 1f / outputWidth, height * 1f / outputHeight)
            }
        }

        val scaleWidth = (outputWidth * scaleFactor).toInt()
        val scaleHeight = (outputHeight * scaleFactor).toInt()

        scaleBitmap = Bitmap.createScaledBitmap(
            image, scaleWidth, scaleHeight, false
        )
        invalidate()
        listener?.onLabels(colorLabel.toList())
    }

    interface OverlayViewListener {
        fun onLabels(colorLabels: List<Pair<String, Int>>)
    }
}

fun Int.toAlphaColor(): Int {
    return Color.argb(
        OverlayView.ALPHA_COLOR,
        Color.red(this),
        Color.green(this),
        Color.blue(this)
    )
}