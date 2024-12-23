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
import androidx.compose.material3.TextField
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.hellodroid.ui.theme.HelloDroidTheme
import com.google.gson.GsonBuilder
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.CurlUserAgent
import io.ktor.client.request.HttpRequest
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.util.toMap
import kotlinx.coroutines.launch

object Common {
    val gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    val httpClient = HttpClient(CIO) {
        CurlUserAgent()
    }
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
            HelloDroidTheme(darkTheme = false) {
                Scaffold(
                    snackbarHost = {
                        SnackbarHost(snackBarHostState)
                    },
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("YOLO")
//                                    Spacer(Modifier.width(10.dp))
//                                    Badge(containerColor = Color.Green) {
//                                        Text("wifi")
//                                    }
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
    val scope = rememberCoroutineScope()
//    val initialUrl = "https://postman-echo.com/get"
    val initialUrl = "https://ifconfig.me//"
    var expanded by remember { mutableStateOf(false) }
    var selectedHttpMethod by remember { mutableStateOf("GET") }
    var requestBodyJson by remember { mutableStateOf("") }
    var location by remember { mutableStateOf(initialUrl) }
    var httpResponse by remember { mutableStateOf("") }
    var methodDropDownButtonPos by remember { mutableStateOf(Offset.Zero) }
    var useCellular by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    var enableSendButton by remember { mutableStateOf(true) }

    val supportedMethod = setOf<String>("GET", "POST")
    val sampleJson = mapOf(
        Pair("requestHeaders", emptyMap<String, String>()),
        Pair("requestParams", emptyMap()),
        Pair("requestBody", emptyMap())
    )
    requestBodyJson = Common.gson.toJson(sampleJson)

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
//                    enableSendButton = false
                    val request = NetworkRequest.Builder().let { request ->
                        if (it) {
                            Log.d("HomeScreen: ", "Switching to CELLULAR")
                            request.addTransportType(Network.CELLULAR.value)
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
                                scope.launch {
                                    val text = "Switched to ${if (it) "CELLULAR" else "WIFI"}"
                                    snackBarHostState
                                        .showSnackbar(
                                            message = text,
                                            duration = SnackbarDuration.Short
                                        )
                                }
                                enableSendButton = true
                            }

                            override fun onUnavailable() {
                                super.onUnavailable()
                                scope.launch {
                                    val text =
                                        "Failed to switch to ${if (it) "CELLULAR" else "WIFI"}"
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
            TextField(
                value = location,
                onValueChange = { location = it }
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 5.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("JSON Payload")
            OutlinedTextField(
                value = requestBodyJson,
                onValueChange = { requestBodyJson = it },
                minLines = 10,
                maxLines = 20,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(10.dp))
        if (enableSendButton) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = {
                    httpResponse = ""
                    scope.launch {
                        val response = Common
                            .httpClient
                            .get(location)
                            .let {
                                val body = it.bodyAsText().let { originalBody ->
                                    if (isJsonResponse(it)) {
                                        Common.gson.fromJson(
                                            originalBody,
                                            mutableMapOf<String, Any>()::class.java
                                        )
                                    } else {
                                        originalBody
                                    }
                                }
                                mapOf(
                                    Pair(
                                        "status",
                                        mapOf(
                                            Pair("statusCode", it.status.value),
                                            Pair("statusDescription", it.status.description)
                                        )
                                    ),
                                    Pair("body", body),
                                    Pair("responseHeaders", responseHeader(it)),
                                    Pair("requestHeaders", requestHeaders(it.request))
                                )
                            }
                        httpResponse = Common.gson.toJson(response)
                    }
                }) {

                    Text("Send")
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


private fun isJsonResponse(response: HttpResponse) =
    response.headers.get("content-type").let {
        val isJson = it?.contains("application/json") == true
        isJson
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
            Pair(headerName, values)
        }
        .toMap()
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    HelloDroidTheme {
        Greeting("Android")
    }
}