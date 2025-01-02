package com.example.hellodroid

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.gson.GsonBuilder
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.CurlUserAgent
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

object Commons {
    private const val KTOR = "KTOR"
    private var customHttpClient: HttpClient? = null

    private fun doLog(message: String) {
        Log.d(KTOR, message)
    }

    private val TraceIdPlugin = createClientPlugin("TraceIdPlugin") {
        onRequest { request, _ ->
            request.headers.let {
                val requestId = UUID.randomUUID().toString()
                if (!it.contains("x-correlator")) {
                    it.append("x-correlator", requestId)
                }
                it.append("x-request-id", requestId)
            }
        }
    }

    val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    @SuppressLint("NewApi")
    fun createHttpClient(logCollector: (Pair<String, String>) -> Unit): HttpClient {
        if (customHttpClient == null) {
            Log.d(KTOR, "creating new client")
            customHttpClient = HttpClient(CIO) {
                CurlUserAgent()
                install(TraceIdPlugin)
                install(Logging) {
                    logger = YoloClientLogger(logCollector)
                    level = LogLevel.ALL
                }
                engine {
                    https {
                        trustManager = InsecureTrustManager()
                    }
                }
            }
        }
        return customHttpClient!!
    }

    private class YoloClientLogger(val logCollector: (Pair<String, String>) -> Unit) : Logger {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun log(message: String) {
            logCollector(
                Pair(
                    LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).toString(),
                    message
                )
            )
            doLog(message)
        }
    }

    fun closeHttpClient() {
        Log.d(KTOR, "closing client")
        customHttpClient?.close()
    }

}