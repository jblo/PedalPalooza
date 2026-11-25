package com.example.pedalpalooza

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pedalpalooza.ui.EventListScreen
import com.example.pedalpalooza.ui.EventListViewModel
import com.example.pedalpalooza.ui.theme.PedalPaloozaTheme
import com.google.firebase.messaging.FirebaseMessaging
import java.net.URI

private const val ROUTE_LIST = "list"
private const val ROUTE_WEBSITE = "website"
private const val ROUTE_EVENT_WEB = "event_web/{url}"
private const val SHIFT_CALENDAR_URL = "https://www.shift2bikes.org/calendar/"

class MainActivity : ComponentActivity() {
    private val pendingNotificationUrl = mutableStateOf<String?>(null)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleNotificationIntent(intent)
        enableEdgeToEdge()
        setContent {
            PedalPaloozaTheme {
                val navController = rememberNavController()
                val eventListViewModel: EventListViewModel = viewModel(
                    factory = EventListViewModel.factory(applicationContext)
                )
                val snackbarHostState = remember { SnackbarHostState() }
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val notificationUrl by pendingNotificationUrl

                LaunchedEffect(notificationUrl) {
                    val targetUrl = notificationUrl ?: return@LaunchedEffect
                    pendingNotificationUrl.value = null
                    if (isInternalShiftUrl(targetUrl)) {
                        navController.navigate("event_web/${Uri.encode(targetUrl)}")
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    when (currentRoute) {
                                        ROUTE_EVENT_WEB -> "Event Details"
                                        ROUTE_LIST -> "PedalPalooza Calendar"
                                        ROUTE_WEBSITE -> "Shift2Bikes"
                                        else -> "PedalPalooza"
                                    }
                                )
                            },
                            navigationIcon = {
                                if (currentRoute == ROUTE_EVENT_WEB) {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Back"
                                        )
                                    }
                                }
                            }
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                                label = { Text("Daily List") },
                                selected = currentRoute == ROUTE_LIST || currentRoute == ROUTE_EVENT_WEB,
                                onClick = {
                                    if (currentRoute == ROUTE_EVENT_WEB) {
                                        navController.popBackStack(ROUTE_LIST, inclusive = false)
                                    } else if (currentRoute != ROUTE_LIST) {
                                        navController.navigate(ROUTE_LIST) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Web, contentDescription = null) },
                                label = { Text("Website") },
                                selected = currentRoute == ROUTE_WEBSITE,
                                onClick = {
                                    if (currentRoute != ROUTE_WEBSITE) {
                                        navController.navigate(ROUTE_WEBSITE) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_LIST,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(ROUTE_LIST) {
                            EventListScreen(
                                viewModel = eventListViewModel,
                                snackbarHostState = snackbarHostState
                            ) { event ->
                                val rawUrl = event.shareable
                                    ?.takeIf { isInternalShiftUrl(it) }
                                    ?: SHIFT_CALENDAR_URL
                                navController.navigate("event_web/${Uri.encode(rawUrl)}")
                            }
                        }
                        composable(
                            route = ROUTE_EVENT_WEB,
                            arguments = listOf(navArgument("url") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val url = backStackEntry.arguments?.getString("url")
                                ?.takeIf { isInternalShiftUrl(it) }
                                ?: SHIFT_CALENDAR_URL
                            WebViewScreen(url = url)
                        }
                        composable(ROUTE_WEBSITE) {
                            WebViewScreen(url = SHIFT_CALENDAR_URL)
                        }
                    }
                }
            }
        }

        // Log the FCM token in debug builds only
        if (BuildConfig.DEBUG) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }
                val token = task.result
                Log.d("FCM", "Current FCM token: $token")
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val url = intent?.getStringExtra(MyFirebaseMessagingService.EXTRA_NOTIFICATION_URL)
        if (!url.isNullOrBlank()) {
            pendingNotificationUrl.value = url
        }
    }
}

internal fun isInternalShiftUrl(scheme: String?, host: String?): Boolean {
    val normalizedScheme = scheme?.lowercase() ?: return false
    if (normalizedScheme != "https" && normalizedScheme != "http") return false
    val normalizedHost = host?.lowercase() ?: return false
    return normalizedHost == "shift2bikes.org" || normalizedHost.endsWith(".shift2bikes.org")
}

internal fun isInternalShiftUrl(url: String?): Boolean {
    if (url.isNullOrBlank()) return false
    return try {
        val uri = URI(url)
        isInternalShiftUrl(uri.scheme, uri.host)
    } catch (_: Exception) {
        false
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(url: String, modifier: Modifier = Modifier) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loadedUrl by remember { mutableStateOf<String?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var progress by remember { mutableIntStateOf(0) }

    BackHandler(enabled = canGoBack) {
        webView?.goBack()
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                            canGoBack = view?.canGoBack() == true
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                            canGoBack = view?.canGoBack() == true
                        }

                        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                            super.doUpdateVisitedHistory(view, url, isReload)
                            canGoBack = view?.canGoBack() == true
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?,
                        ): Boolean {
                            val requestUri = request?.url ?: return true
                            return if (isInternalShiftUrl(requestUri.scheme, requestUri.host)) {
                                // Open internal Shift2Bikes links in the WebView
                                false
                            } else {
                                // Open external links in the system browser/app; block unsupported schemes from WebView
                                val scheme = requestUri.scheme?.lowercase()
                                if (scheme == "https" || scheme == "http" || scheme == "mailto" || scheme == "tel" || scheme == "geo") {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, requestUri)
                                        context.startActivity(intent)
                                    } catch (_: ActivityNotFoundException) {
                                        // Ignore if no handler is installed
                                    }
                                }
                                true
                            }
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?,
                        ) {
                            super.onReceivedError(view, request, error)
                            // In a real app, you might show a custom error UI here
                            isLoading = false
                            canGoBack = view?.canGoBack() == true
                        }
                    }

                    webChromeClient = object : android.webkit.WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                        }
                    }

                    // JavaScript is necessary for the Shift2Bikes calendar to function correctly.
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    loadUrl(url)
                    loadedUrl = url
                    webView = this
                }
            },
            update = { view ->
                webView = view
                if (url.isNotBlank() && loadedUrl != url) {
                    view.loadUrl(url)
                    loadedUrl = url
                }
            },
            onRelease = { view ->
                view.stopLoading()
                view.destroy()
                webView = null
            }
        )

        if (isLoading) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}