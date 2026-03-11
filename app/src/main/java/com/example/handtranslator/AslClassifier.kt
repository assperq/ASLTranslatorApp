package com.example.handtranslator

import android.content.Context
import com.example.handtranslator.Helper.floatArrayToByteBuffer
import com.example.handtranslator.ml.AslModel
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class AslClassifier(context: Context) {

    private val model = AslModel.newInstance(context)

    fun predict(features: FloatArray): Int {
        val byteBuffer = floatArrayToByteBuffer(features)

        // Создаём TensorBuffer для входа
        val inputFeature = TensorBuffer.createFixedSize(intArrayOf(1, 210), DataType.FLOAT32)
        inputFeature.loadBuffer(byteBuffer)

        // Инференс
        val outputs = model.process(inputFeature)
        val outputFeature = outputs.outputFeature0AsTensorBuffer

        // Находим индекс с максимальной вероятностью
        val probabilities = outputFeature.floatArray
        return probabilities.indices.maxByOrNull { probabilities[it] } ?: -1
    }

    fun close() {
        model.close()
    }
}