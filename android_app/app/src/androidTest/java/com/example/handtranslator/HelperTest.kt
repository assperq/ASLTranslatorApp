package com.example.handtranslator

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import org.junit.Assert.assertEquals
import org.junit.Test

class HelperTest {

    @Test
    fun landmarksTo210Features_returnsCorrectFeatureCount() {

        val landmarks = List(21) {
            NormalizedLandmark.create(
                it.toFloat(),
                it.toFloat(),
                it.toFloat()
            )
        }

        val features = Helper.landmarksTo210Features(landmarks)

        assertEquals(210, features.size)
    }
}