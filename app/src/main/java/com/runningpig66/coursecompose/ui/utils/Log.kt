package com.runningpig66.coursecompose.ui.utils

import android.util.Log

/**
 * @author runningpig66
 * @date 2026-06-24
 * @time 0:14
 */
const val DEBUG = true
private var zeroTime = System.currentTimeMillis()

@Deprecated(message = "")
fun logD(message: Any? = "") {
    println(
        "${System.currentTimeMillis() - zeroTime} " +
                "[${Thread.currentThread().name}] " +
                "${if (message.toString().isBlank()) "Process start" else message}"
    )
}

fun log(tag: String = "CourseCompose", message: Any? = "") {
    val elapsed = System.currentTimeMillis() - zeroTime
    val thread = Thread.currentThread().name
    val text = message?.toString().orEmpty().ifBlank { "Process start" }

    text.lineSequence().forEachIndexed { index, line ->
        val marker = if (index == 0) "" else "│ "
        Log.d(tag, "$elapsed [$thread] $marker$line")
    }
}

fun resetLog(message: String = "================================") {
    if (message.isNotBlank()) {
        Log.d("CourseCompose", message)
    }
    zeroTime = System.currentTimeMillis()
}
