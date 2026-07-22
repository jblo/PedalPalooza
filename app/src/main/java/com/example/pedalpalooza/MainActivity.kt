package com.example.pedalpalooza

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pedalpalooza.ui.EventListScreen
import com.example.pedalpalooza.ui.theme.PedalPaloozaTheme
import com.google.firebase.messaging.FirebaseMessaging
import java.net.URLEncoder

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PedalPaloozaTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                NotificationPermissionHandler()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    if (currentRoute?.startsWith("web") == true) "Event Details"
                                    else if (currentRoute == "list") "PedalPalooza Calendar"
                                    else "Shift2Bikes"
                                )
                            },
                            navigationIcon = {
                                if (currentRoute?.startsWith("web") == true) {
                                    IconButton(onClick = { navController.popBackStack() }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                                selected = currentRoute == "list",
                                onClick = {
                                    if (currentRoute != "list") {
                                        navController.navigate("list") {
                                            popUpTo("list") { inclusive = true }
                                        }
                                    }
                                }
                            )
                            NavigationBarItem(
                                icon = { Icon(Icons.Default.Web, contentDescription = null) },
                                label = { Text("Website") },
                                selected = currentRoute?.startsWith("web_root") == true,
                                onClick = {
                                    val rootUrl = URLEncoder.encode("https://www.shift2bikes.org/calendar/", "UTF-8")
                                    navController.navigate("web_root/$rootUrl") {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "list",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("list") {
                            EventListScreen { event ->
                                val url = URLEncoder.encode(event.shareable ?: "https://www.shift2bikes.org/calendar/", "UTF-8")
                                navController.navigate("web/$url")
                            }
                        }
                        composable(
                            route = "web/{url}",
                            arguments = listOf(navArgument("url") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val url = backStackEntry.arguments?.getString("url") ?: ""
                            WebViewScreen(url = url)
                        }
                        composable(
                            route = "web_root/{url}",
                            arguments = listOf(navArgument("url") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val url = backStackEntry.arguments?.getString("url") ?: ""
                            WebViewScreen(url = url)
                        }
                    }
                }
            }
        }

        // Log the FCM token for testing
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

@Composable
fun NotificationPermissionHandler() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val context = LocalContext.current
        var hasPermission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            )
        }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                hasPermission = isGranted
            }
        )

        LaunchedEffect(Unit) {
            if (!hasPermission) {
                launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(url: String, modifier: Modifier = Modifier) {
    var webView: WebView? by remember { mutableStateOf(null) }
    var isLoading by remember { mutableStateOf(value = true) }
    var progress by remember { mutableIntStateOf(0) }

    BackHandler(enabled = webView?.canGoBack() == true) {
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
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?,
                        ): Boolean {
                            val requestUrl = request?.url?.toString() ?: return false
                            return if (requestUrl.contains("shift2bikes.org")) {
                                // Open internal links in the WebView
                                false
                            } else {
                                // Open external links in the system browser
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, requestUrl.toUri())
                                    context.startActivity(intent)
                                    true
                                } catch (_: Exception) {
                                    false
                                }
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
                    loadUrl(url)
                    webView = this
                }
            },
        ) { view ->
            webView = view
        }

        if (isLoading) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}