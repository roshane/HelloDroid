package com.example.hellodroid

import android.annotation.SuppressLint
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.hellodroid.AppConstants.FORM_PARAMS
import com.example.hellodroid.AppConstants.QUERY_PARAMS
import com.example.hellodroid.AppConstants.REQUEST_BODY
import com.example.hellodroid.AppConstants.REQUEST_HEADERS
import com.example.hellodroid.Commons.gson
import com.example.hellodroid.Commons.httpClient
import com.example.hellodroid.ui.theme.HelloDroidTheme
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.CurlUserAgent
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequest
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.HttpMethod
import io.ktor.http.headers
import io.ktor.http.parameters
import io.ktor.util.toMap
import kotlinx.coroutines.launch
import java.util.UUID

object Commons {

    private val TraceIdPlugin = createClientPlugin("TraceIdPlugin") {
        onRequest { request, _ ->
            request.headers.let {
                val requestId = UUID.randomUUID().toString()
                it.append("x-request-id", requestId)
                it.append("x-correlator-id", requestId)
            }
        }
    }

    val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    val httpClient = HttpClient(CIO) {
        CurlUserAgent()
        install(TraceIdPlugin)
    }

}

object AppConstants {
    const val REQUEST_HEADERS = "requestHeader"
    const val FORM_PARAMS = "formParams"
    const val REQUEST_BODY = "requestBody"
    const val QUERY_PARAMS = "queryParams"
}

enum class Network(val value: Int) {
    WIFI(NetworkCapabilities.TRANSPORT_WIFI),
    CELLULAR(NetworkCapabilities.TRANSPORT_CELLULAR)
}

class MainActivity : ComponentActivity() {


    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        setContent {
            var snackBarHostState by remember { mutableStateOf(SnackbarHostState()) }
            HelloDroidTheme(darkTheme = true) {
                Scaffold(
                    snackbarHost = {
                        SnackbarHost(snackBarHostState)
                    },
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "YOLO App",
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            },
                        )
                    },
                    content = { padding ->
                        HomeScreen(
                            padding,
                            connectivityManager,
                            snackBarHostState
                        )
                    },
                    bottomBar = {},
                )
            }
        }
    }
}

@SuppressLint("NewApi")
@Composable
fun HomeScreen(
    paddingValues: PaddingValues,
    connectivityManager: ConnectivityManager,
    snackBarHostState: SnackbarHostState
) {
    val defaultUrl = "https://postman-echo.com/get"
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val supportedMethod = setOf<String>("GET", "POST")

    val requestJsonSample = mapOf(
        REQUEST_HEADERS to mapOf("x-developer" to "developer@acme.com"),
        FORM_PARAMS to emptyMap(),
        REQUEST_BODY to emptyMap(),
        QUERY_PARAMS to mapOf("x-query-developer" to "developer@acme.com")
    )
    var expanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf(defaultUrl) }
    var useCellular by remember { mutableStateOf(false) }
    var httpResponse by remember { mutableStateOf("") }
    var httpRequestDataJson by remember { mutableStateOf(gson.toJson(requestJsonSample)) }
    var enableSendButton by remember { mutableStateOf(true) }
    var selectedHttpMethod by remember { mutableStateOf("GET") }
    var methodDropDownButtonPos by remember { mutableStateOf(Offset.Zero) }

    Column(
        modifier = Modifier
            .padding(paddingValues)
            .padding(5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Force Cellular:")
            Spacer(Modifier.width(5.dp))
            Switch(
                checked = useCellular,
                onCheckedChange = {
                    useCellular = it
                    enableSendButton = false
                    val request = NetworkRequest.Builder().let { request ->
                        if (it) {
                            Log.d("HomeScreen: ", "Switching to CELLULAR")
                            request.addTransportType(Network.CELLULAR.value)
                                .addCapability(NetworkCapabilities.NET_CAPABILITY_MMS)
                        } else {
                            Log.d("HomeScreen: ", "Switching to WIFI")
                            request.addTransportType(Network.WIFI.value)
                        }
                        request.build()
                    }
                    connectivityManager.requestNetwork(
                        request,
                        object : ConnectivityManager.NetworkCallback() {
                            override fun onAvailable(network: android.net.Network) {
                                connectivityManager.bindProcessToNetwork(network)
                                val text = "Switched to ${if (it) "CELLULAR" else "WIFI"}"
                                Log.d("HomeScreen: ", text)
                                scope.launch {
                                    snackBarHostState.currentSnackbarData?.dismiss()
                                    snackBarHostState
                                        .showSnackbar(
                                            message = text,
                                            duration = SnackbarDuration.Short
                                        )
                                }
                                enableSendButton = true
                            }
                        }
                    )
                }
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("HTTP Method: $selectedHttpMethod")
            IconButton(
                onClick = { expanded = true },
                modifier = Modifier.onGloballyPositioned {
                    methodDropDownButtonPos = it.localToWindow(Offset.Zero)
                }) {
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Show Method")
            }
            DropdownMenu(
                expanded = expanded,
                offset = DpOffset(
                    x = with(density) { methodDropDownButtonPos.x.toDp() },
                    y = 0.dp
                ),
                onDismissRequest = { expanded = false }) {
                supportedMethod.forEachIndexed { i, it ->
                    DropdownMenuItem(
                        onClick = {
                            selectedHttpMethod = it
                            expanded = false
                        },
                        text = {
                            Text(it)
                        }
                    )
                    if (i < supportedMethod.size - 1) {
                        HorizontalDivider()
                    }
                }

            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("URL:")
            Spacer(Modifier.width(10.dp))
            OutlinedTextField(
                value = location,
                maxLines = 1,
                onValueChange = { location = it }
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Text("RequestData JSON")
            Spacer(Modifier.width(5.dp))
            OutlinedTextField(
                value = httpRequestDataJson,
                onValueChange = { value -> httpRequestDataJson = value },
                minLines = 10,
                maxLines = 20,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(10.dp))
        if (enableSendButton) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        httpResponse = ""
                        isLoading = true
                        scope.launch {
                            exchange(
                                location,
                                httpRequestDataJson,
                                selectedHttpMethod
                            ).let {
                                httpResponse = it
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading
                ) {
                    Text(if (isLoading) "Loading..." else "Send")
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Column {
            if (httpResponse.isEmpty().not()) {
                OutlinedTextField(
                    value = httpResponse,
                    onValueChange = {},
                    readOnly = true
                )
            }
        }
    }
}

private suspend fun exchange(
    selectedUrl: String,
    requestJson: String,
    selectedMethod: String
): String {
    val requestData = gson.fromJson(requestJson, object : TypeToken<Map<String, Any>>() {})
    val requestBody = requestData[REQUEST_BODY]?.let {
        when (it) {
            is String -> it
            is Number -> it.toString()
            else -> gson.toJson(it)
        }
    } ?: ""
    val formParams = requestData[FORM_PARAMS] ?: emptyMap<String, String>()
    val queryParams = requestData[QUERY_PARAMS]?.let {
        it as Map<*, *>
    } ?: emptyMap<String, String>()
    val requestHeaders = requestData[REQUEST_HEADERS]?.let {
        it as Map<*, *>
    } ?: emptyMap<String, String>()
    val request = httpClient.request(urlString = selectedUrl) {
        method = HttpMethod(selectedMethod)
        setBody(requestBody)
        headers {
            requestHeaders.forEach {
                Log.d(">>> Client", "Appending ${it.key.toString()}")
                append(it.key.toString(), it.value.toString())
            }
        }
        url {
            parameters {
                queryParams.forEach {
                    append(it.key.toString(), it.value.toString())
                }
            }
        }
    }
    val response = request
        .let {
            val body = it.bodyAsText().let { originalBody ->
                if (isJsonResponse(it)) {
                    gson.fromJson(
                        originalBody,
                        mutableMapOf<String, Any>()::class.java
                    )
                } else {
                    originalBody
                }
            }
            mapOf(
                "status" to mapOf(
                    "statusCode" to it.status.value,
                    "statusDescription" to it.status.description
                ),
                "body" to body,
                "responseHeaders" to responseHeader(it),
                "requestHeaders" to requestHeaders(it.request)
            )
        }
    return gson.toJson(response)
}

private fun isJsonResponse(response: HttpResponse) =
    response.headers["content-type"].let {
        it?.contains("application/json") == true
    }

private fun requestHeaders(request: HttpRequest): Map<String, Any> {
    return request
        .headers
        .toMap()
}

private fun responseHeader(response: HttpResponse): Map<String, Any> {
    return response
        .headers
        .names()
        .map {
            val headerName = it
            val values = response.headers.getAll(headerName) ?: listOf()
            headerName to values
        }
        .toMap()
}