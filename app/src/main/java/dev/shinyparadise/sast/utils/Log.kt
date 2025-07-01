package dev.shinyparadise.sast.utils

import android.util.Log

inline fun <reified T> T.log(message: String = ""): T {
    Log.d(TAG, "${this?.javaClass?.name}: $this $message")
    return this
}

const val TAG = "SAST"
