package com.example.handtranslator

import android.content.Context
import com.example.handtranslator.Helper.floatArrayToByteBuffer
import com.example.handtranslator.ml.AslModel
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class AslClassifier(context: Context) {

    private var model: AslModel? = AslModel.newInstance(context)

    data class PredictionResult(
        val index: Int,
        val confidence: Float
    )


    fun predict(features: FloatArray): PredictionResult {
        if (features.size != FEATURE_VECTOR_SIZE) {
            return PredictionResult(index = -1, confidence = 0f)
        }

        val currentModel = model ?: return PredictionResult(index = -1, confidence = 0f)

        val byteBuffer = floatArrayToByteBuffer(features)

        val inputFeature = TensorBuffer.createFixedSize(intArrayOf(1, 210), DataType.FLOAT32)
        inputFeature.loadBuffer(byteBuffer)

        val outputs = currentModel.process(inputFeature)
        val probabilities = outputs.outputFeature0AsTensorBuffer.floatArray
        val predictedIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1
        val confidence = probabilities.getOrNull(predictedIndex) ?: 0f
        return PredictionResult(predictedIndex, confidence)
    }

    fun close() {
        model?.close()
        model = null
    }

    private companion object {
        const val FEATURE_VECTOR_SIZE = 210
    }
}