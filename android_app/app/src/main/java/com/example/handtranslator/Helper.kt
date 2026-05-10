package com.example.handtranslator

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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

    fun getAslDrawable(context: Context, letter: String): Int {
        if (letter.uppercase() !in loadAslLabels(context)) throw Exception("Unknown letter")
        val name = "asl_${letter.lowercase()}"

        return context.resources.getIdentifier(
            name,
            "drawable",
            context.packageName
        )
    }

    fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = false
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            Log.e("Media", "Failed to load bitmap from uri: $uri", e)
            null
        }
    }
}