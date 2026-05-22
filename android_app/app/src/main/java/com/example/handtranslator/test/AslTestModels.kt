package com.example.handtranslator.test

import androidx.annotation.DrawableRes
import com.example.handtranslator.R

const val ROUND_DURATION = 5L

data class AslCard(val letter: String, @DrawableRes val drawableRes: Int)
enum class GameState { READY, PLAYING }
enum class AnswerState { IDLE, CORRECT, WRONG, TIMEOUT }

val deck = listOf(
    AslCard("A", R.drawable.asl_a), AslCard("B", R.drawable.asl_b), AslCard("C", R.drawable.asl_c), AslCard("D", R.drawable.asl_d), AslCard("E", R.drawable.asl_e),
    AslCard("F", R.drawable.asl_f), AslCard("G", R.drawable.asl_g), AslCard("H", R.drawable.asl_h), AslCard("I", R.drawable.asl_i), AslCard("J", R.drawable.asl_j),
    AslCard("K", R.drawable.asl_k), AslCard("L", R.drawable.asl_l), AslCard("M", R.drawable.asl_m), AslCard("N", R.drawable.asl_n), AslCard("O", R.drawable.asl_o),
    AslCard("P", R.drawable.asl_p), AslCard("Q", R.drawable.asl_q), AslCard("R", R.drawable.asl_r), AslCard("S", R.drawable.asl_s), AslCard("T", R.drawable.asl_t),
    AslCard("U", R.drawable.asl_u), AslCard("V", R.drawable.asl_v), AslCard("W", R.drawable.asl_w), AslCard("X", R.drawable.asl_x), AslCard("Y", R.drawable.asl_y), AslCard("Z", R.drawable.asl_z)
)
