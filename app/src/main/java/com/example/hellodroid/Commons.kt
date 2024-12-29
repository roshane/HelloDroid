package com.example.hellodroid

import android.util.Log
import com.google.gson.GsonBuilder
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.CurlUserAgent
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import java.util.UUID

object Commons {
    private const val KTOR = "KTOR"

    private fun doLog(message:String) {
        Log.d(KTOR, message)
        Log.d(KTOR, "#END#")
    }

    private val TraceIdPlugin = createClientPlugin("TraceIdPlugin") {
        onRequest { request, _ ->
            request.headers.let {
                val requestId = UUID.randomUUID().toString()
                it.append("x-request-id", requestId)
                it.append("x-correlator", requestId)
            }
        }
    }

    val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    val httpClient = HttpClient(CIO) {
        CurlUserAgent()
        install(TraceIdPlugin)
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    doLog(message)
                }
            }
            level = LogLevel.ALL
        }
        engine {
            https {
                trustManager = InsecureTrustManager()
            }
        }
    }

}