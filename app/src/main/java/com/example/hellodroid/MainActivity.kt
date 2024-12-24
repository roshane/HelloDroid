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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.hellodroid.AppConstants.FORM_PARAMS
import com.example.hellodroid.AppConstants.LOG_TAG
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
    const val LOG_TAG = "@HelloDroid"

    const val REQUEST_HEADERS = "requestHeader"
    const val FORM_PARAMS = "formParams"
    const val REQUEST_BODY = "requestBody"
    const val QUERY_PARAMS = "queryParams"

    const val TAG_CELLULAR = "tag_cellular"
    const val TAG_HTTP_METHOD_DROPDOWN = "tag_http_method_dropdown"
    const val TAG_URL = "tag_url"
    const val TAG_REQUEST_DATA_JSON = "tag_request_data_json"
    const val TAG_SEND_BUTTON = "tag_send_button"
    const val TAG_SEND_BUTTON_TEXT = "tag_send_button_text"
    const val TAG_RESPONSE_DATA_JSON = "tag_response_data_json"
}

enum class Network(val value: Int) {
    WIFI(NetworkCapabilities.TRANSPORT_WIFI),
    CELLULAR(NetworkCapabilities.TRANSPORT_CELLULAR)
}

class MainActivity : ComponentActivity() {
    override fun onTopResumedActivityChanged(isTopResumedActivity: Boolean) {
        super.onTopResumedActivityChanged(isTopResumedActivity)
        log(">> onTopResumedActivityChanged")
    }

    override fun onDestroy() {
        super.onDestroy()
        log("cleaning up resource")
        httpClient.close()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        setContent {
            var snackBarHostState by remember { mutableStateOf(SnackbarHostState()) }
            HelloDroidTheme(darkTheme = false) {
                Scaffold(
                    snackbarHost = {
                        SnackbarHost(snackBarHostState)
                    },
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    "YOLO App",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            },
                            colors = TopAppBarDefaults
                                .topAppBarColors()
                                .copy(containerColor = MaterialTheme.colorScheme.primary)
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
//    val defaultUrl = "https://postman-echo.com/get"
    val defaultUrl = "https://ifconfig.me/"
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val supportedMethod = setOf<String>("GET", "POST")
    val isUsingCellular = connectivityManager.activeNetwork?.let {
        connectivityManager.getNetworkCapabilities(it)
            ?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
    } == true
    val requestJsonSample = mapOf(
        REQUEST_HEADERS to mapOf("x-developer" to "goat@singtel.com"),
        FORM_PARAMS to emptyMap(),
        REQUEST_BODY to emptyMap(),
        QUERY_PARAMS to mapOf("x-query-developer" to "goat@singtel.com")
    )
    var expanded by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf(defaultUrl) }
    var useCellular by remember { mutableStateOf(isUsingCellular) }
    var httpResponse by remember { mutableStateOf("") }
    var httpRequestDataJson by remember { mutableStateOf(gson.toJson(requestJsonSample)) }
    var enableSendButton by remember { mutableStateOf(true) }
    var selectedHttpMethod by remember { mutableStateOf("GET") }
    var methodDropDownButtonPos by remember { mutableStateOf(Offset.Zero) }

    Column(
        modifier = Modifier
            .padding(paddingValues)
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Use Cellular")

            }
            Column(modifier = Modifier.weight(3f)) {
                Switch(
                    modifier = Modifier.testTag(AppConstants.TAG_CELLULAR),
                    checked = useCellular,
                    thumbContent = {
                        if (useCellular) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "",
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        } else {
                            null
                        }
                    },
                    onCheckedChange = {
                        useCellular = it
                        enableSendButton = false
                        val request = NetworkRequest.Builder().let { request ->
                            if (it) {
                                log("Switching to CELLULAR")
                                request.addTransportType(Network.CELLULAR.value)
                                    .addCapability(NetworkCapabilities.NET_CAPABILITY_MMS)
                            } else {
                                log("Switching to WIFI")
                                request.addTransportType(Network.WIFI.value)
                            }
                            request.build()
                        }
                        connectivityManager.requestNetwork(
                            request,
                            object : ConnectivityManager.NetworkCallback() {
                                override fun onUnavailable() {
                                    super.onUnavailable()
                                    useCellular = !useCellular
                                }

                                override fun onAvailable(network: android.net.Network) {
                                    connectivityManager.bindProcessToNetwork(network)
                                    val text = "Switched to ${if (it) "CELLULAR" else "WIFI"}"
                                    log(text)
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
                            },
                            500
                        )
                    }
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("HTTP $selectedHttpMethod")
                    IconButton(
                        onClick = { expanded = true },
                        modifier = Modifier
                            .onGloballyPositioned {
                                methodDropDownButtonPos = it.localToWindow(Offset.Zero)
                            }
                            .testTag(AppConstants.TAG_HTTP_METHOD_DROPDOWN)
                    ) {
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
            }
            Column(modifier = Modifier.weight(3f)) {
                OutlinedTextField(
                    value = location,
                    singleLine = true,
                    onValueChange = { location = it },
                    modifier = Modifier.testTag(AppConstants.TAG_URL)
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("RequestData JSON")
            }
            Column(modifier = Modifier.weight(3f)) {
                OutlinedTextField(
                    value = httpRequestDataJson,
                    onValueChange = { value -> httpRequestDataJson = value },
                    minLines = 10,
                    maxLines = 20,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(AppConstants.TAG_REQUEST_DATA_JSON)
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Row {
            Column(modifier = Modifier.weight(1f)) {

            }
            Column(modifier = Modifier.weight(3f)) {
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
                    enabled = !isLoading,
                    modifier = Modifier.testTag(AppConstants.TAG_SEND_BUTTON)
                ) {
                    Text(
                        if (isLoading) "Loading..." else "Send",
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.testTag(AppConstants.TAG_SEND_BUTTON_TEXT)
                    )
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Column {
            AnimatedVisibility(httpResponse.isNotBlank()) {
                OutlinedTextField(
                    value = httpResponse,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.testTag(AppConstants.TAG_RESPONSE_DATA_JSON)
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
    val requestBodyAsString = requestData[REQUEST_BODY]?.let {
        when (it) {
            is String -> it
            is Number -> it.toString()
            else -> gson.toJson(it)
        }
    } ?: ""
    log("requestBodyAsString: $requestBodyAsString")
    val formParams = requestData[FORM_PARAMS] ?: emptyMap<String, String>()
    val queryParams = requestData[QUERY_PARAMS]?.let {
        it as Map<*, *>
    } ?: emptyMap<String, String>()
    val requestHeaders = requestData[REQUEST_HEADERS]?.let {
        it as Map<*, *>
    } ?: emptyMap<String, String>()

    return try {
        val response = httpClient.request(urlString = selectedUrl) {
            method = HttpMethod(selectedMethod)
            if (selectedMethod == "POST") {
                setBody(requestBodyAsString)

            }
            requestHeaders.forEach {
                headers.append(it.key.toString(), it.value.toString())
            }
            url {
                queryParams.forEach {
                    parameters.append(it.key.toString(), it.value.toString())
                }
            }
        }.let {
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
                "requestHeaders" to requestHeaders(it.request),
                "responseHeaders" to responseHeader(it),
                "responseBody" to body
            )
        }
        return gson.toJson(response)
    } catch (e: Exception) {
        log(e.message ?: "client error", true)
        return e.message ?: "client error"
    }

}

private fun isJsonResponse(response: HttpResponse) =
    response.headers["content-type"].let {
        it?.contains("application/json") == true
    }

private fun requestHeaders(request: HttpRequest): Map<String, String> {
    return request
        .headers
        .entries()
        .filter { !it.value.isEmpty() }
        .map { it.key to it.value.first() }
        .toMap()
}

private fun responseHeader(response: HttpResponse): Map<String, String> {
    return response
        .headers
        .entries()
        .filter { !it.value.isEmpty() }
        .map { it.key to it.value.first() }
        .toMap()
}

private fun log(message: String, error: Boolean = false) {
    if (error) {
        Log.e(LOG_TAG, message)
    } else {
        Log.d(LOG_TAG, message)
    }
}