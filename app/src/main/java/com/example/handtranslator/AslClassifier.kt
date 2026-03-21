package com.example.handtranslator

import android.content.Context
import com.example.handtranslator.Helper.floatArrayToByteBuffer
import com.example.handtranslator.ml.AslModel
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class AslClassifier(context: Context) {

    private val model = AslModel.newInstance(context)

    data class PredictionResult(
        val index: Int,
        val confidence: Float
    )


    fun predict(features: FloatArray): PredictionResult {
        val byteBuffer = floatArrayToByteBuffer(features)

        // Создаём TensorBuffer для входа
        val inputFeature = TensorBuffer.createFixedSize(intArrayOf(1, 210), DataType.FLOAT32)
        inputFeature.loadBuffer(byteBuffer)

        // Инференс
        val outputs = model.process(inputFeature)
        val outputFeature = outputs.outputFeature0AsTensorBuffer

        // Находим индекс с максимальной вероятностью
        val probabilities = outputFeature.floatArray
        val predictedIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1
        val confidence = probabilities.getOrNull(predictedIndex) ?: 0f
        return PredictionResult(predictedIndex, confidence)
    }

    fun close() {
        model.close()
    }
}