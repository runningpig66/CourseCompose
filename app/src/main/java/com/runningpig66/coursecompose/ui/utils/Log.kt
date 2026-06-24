package com.runningpig66.coursecompose.ui.utils

/**
 * @author runningpig66
 * @date 2026-06-24
 * @time 0:14
 */
const val DEBUG = true
private var zeroTime = System.currentTimeMillis()
fun log(message: Any? = "") {
    println(
        "${System.currentTimeMillis() - zeroTime} " +
                "[${Thread.currentThread().name}] " +
                "${if (message.toString().isBlank()) "Process start" else message}"
    )
}

fun resetLog(message: String = "================================") {
    println(message)
    zeroTime = System.currentTimeMillis()
}
