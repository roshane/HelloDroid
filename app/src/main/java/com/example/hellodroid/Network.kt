package com.example.hellodroid

import android.net.NetworkCapabilities

enum class Network(val value: Int) {
    WIFI(NetworkCapabilities.TRANSPORT_WIFI),
    CELLULAR(NetworkCapabilities.TRANSPORT_CELLULAR)
}