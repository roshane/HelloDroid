package com.example.hellodroid

import android.annotation.SuppressLint
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

@SuppressLint("CustomX509TrustManager")
class InsecureTrustManager : X509TrustManager {
    @SuppressLint("TrustAllX509TrustManager")
    override fun checkClientTrusted(
        chain: Array<out X509Certificate?>?,
        authType: String?
    ) {
    }

    @SuppressLint("TrustAllX509TrustManager")
    override fun checkServerTrusted(
        chain: Array<out X509Certificate?>?,
        authType: String?
    ) {
    }

    override fun getAcceptedIssuers(): Array<out X509Certificate?>? {
        return emptyArray<X509Certificate>()
    }
}