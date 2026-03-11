package com.example.handtranslator

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import java.nio.ByteBuffer
import java.nio.ByteOrder

object Helper {
    fun landmarksTo210Features(landmarks: List<NormalizedLandmark>): FloatArray {
        val n = landmarks.size
        require(n == 21) { "Expected 21 landmarks" }

        val features = FloatArray(210)
        var idx = 0

        for (i in 0 until n) {
            val xi = landmarks[i].x()
            val yi = landmarks[i].y()
            val zi = landmarks[i].z()

            for (j in i + 1 until n) {
                val xj = landmarks[j].x()
                val yj = landmarks[j].y()
                val zj = landmarks[j].z()

                val d = kotlin.math.sqrt(
                    (xi - xj) * (xi - xj) +
                            (yi - yj) * (yi - yj) +
                            (zi - zj) * (zi - zj)
                )
                features[idx++] = d
            }
        }

        return features
    }

    fun floatArrayToByteBuffer(features: FloatArray): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(features.size * 4) // 4 байта на float
        byteBuffer.order(ByteOrder.nativeOrder())
        features.forEach { byteBuffer.putFloat(it) }
        byteBuffer.rewind()
        return byteBuffer
    }

    fun loadAslLabels(context: Context, fileName: String = "labels.txt"): List<String> {
        return try {
            context.assets.open(fileName).bufferedReader().useLines { lines ->
                lines.filter { it.isNotBlank() }.toList()
            }
        } catch (e: Exception) {
            Log.e("ASL", "Failed to load labels: ${e.message}")
            emptyList()
        }
    }
}