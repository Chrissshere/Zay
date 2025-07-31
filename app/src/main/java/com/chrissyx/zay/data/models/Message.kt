package com.chrissyx.zay.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Message(
    val id: String = "",
    val text: String = "",
    val timestamp: Double = 0.0,
    val sender: String = "",
    val device: String = ""
) : Parcelable