package com.example.snmkbillingsolution

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.MediaStore
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (filePathCallback != null) {
            val results = if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val data = result.data
                if (data?.clipData != null) {
                    val count = data.clipData!!.itemCount
                    Array(count) { i -> data.clipData!!.getItemAt(i).uri }
                } else if (data?.data != null) {
                    arrayOf(data.data!!)
                } else {
                    null
                }
            } else {
                null
            }
            filePathCallback?.onReceiveValue(results)
            filePathCallback = null
        } else {
            filePathCallback?.onReceiveValue(null)
            filePathCallback = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable hardware acceleration for smooth rendering
        window.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )

        WindowCompat.setDecorFitsSystemWindows(window, true)
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.parseColor("#0f172a")
        @Suppress("DEPRECATION")
        window.navigationBarColor = android.graphics.Color.parseColor("#0f172a")
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        webView = WebView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }

        val container = FrameLayout(this)
        container.addView(webView)
        setContentView(container)

        setupWebViewSettings()
        setupBackHandler()

        webView.loadUrl("file:///android_asset/index.html")
    }

    @Suppress("DEPRECATION")
    private fun setupWebViewSettings() {
        val settings = webView.settings

        // Performance & Rendering Optimizations
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.allowFileAccessFromFileURLs = true
        settings.allowUniversalAccessFromFileURLs = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        // Viewport & Responsiveness Optimizations
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.setSupportZoom(false)
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        // High priority rendering & smooth scrolling
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            settings.safeBrowsingEnabled = false
        }
        settings.offscreenPreRaster = true

        webView.isVerticalScrollBarEnabled = true
        webView.isHorizontalScrollBarEnabled = false

        webView.addJavascriptInterface(WebAppInterface(this, webView), "AndroidInterface")

        webView.setDownloadListener { url, _, contentDisposition, mimetype, _ ->
            if (url.startsWith("data:")) {
                try {
                    val commaIndex = url.indexOf(",")
                    if (commaIndex != -1) {
                        val header = url.substring(0, commaIndex)
                        val base64Data = url.substring(commaIndex + 1)
                        val mime = if (header.contains(";")) header.split(";")[0].replace("data:", "") else mimetype
                        val filename = URLUtil.guessFileName(url, contentDisposition, mime)
                        saveFileToDownloads(base64Data, mime, filename)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                this@MainActivity.filePathCallback?.onReceiveValue(null)
                this@MainActivity.filePathCallback = filePathCallback

                val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }
                try {
                    filePickerLauncher.launch(intent)
                } catch (e: Exception) {
                    this@MainActivity.filePathCallback?.onReceiveValue(null)
                    this@MainActivity.filePathCallback = null
                    return false
                }
                return true
            }

            override fun onJsAlert(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult?
            ): Boolean {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Billing Sastra Nusantara")
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
                    .setOnCancelListener { result?.cancel() }
                    .show()
                return true
            }

            override fun onJsConfirm(
                view: WebView?,
                url: String?,
                message: String?,
                result: JsResult?
            ): Boolean {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Konfirmasi")
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm() }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> result?.cancel() }
                    .setOnCancelListener { result?.cancel() }
                    .show()
                return true
            }

            override fun onJsPrompt(
                view: WebView?,
                url: String?,
                message: String?,
                defaultValue: String?,
                result: JsPromptResult?
            ): Boolean {
                val input = EditText(this@MainActivity)
                input.setText(defaultValue)
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Input")
                    .setMessage(message)
                    .setView(input)
                    .setPositiveButton(android.R.string.ok) { _, _ -> result?.confirm(input.text.toString()) }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> result?.cancel() }
                    .setOnCancelListener { result?.cancel() }
                    .show()
                return true
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("file://")) {
                    return false
                }
                return try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                    true
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Aplikasi tidak ditemukan untuk membuka link ini.", Toast.LENGTH_SHORT).show()
                    true
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                webView.evaluateJavascript(
                    """
                    (function() {
                        window.print = function() {
                            if (window.AndroidInterface && window.AndroidInterface.printReceipt) {
                                window.AndroidInterface.printReceipt();
                            }
                        };
                        
                        var oldDownloadBlob = window.downloadBlob;
                        window.downloadBlob = function(content, mime, filename) {
                            if (window.AndroidInterface && window.AndroidInterface.saveFile) {
                                try {
                                    var base64 = btoa(unescape(encodeURIComponent(content)));
                                    window.AndroidInterface.saveFile(base64, mime, filename);
                                } catch (e) {
                                    if (typeof oldDownloadBlob === 'function') oldDownloadBlob(content, mime, filename);
                                }
                            } else if (typeof oldDownloadBlob === 'function') {
                                oldDownloadBlob(content, mime, filename);
                            }
                        };
                    })();
                    """.trimIndent(),
                    null
                )
            }
        }
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    fun printWebView(webViewToPrint: WebView) {
        try {
            val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
            val jobName = "Billing_Sastra_Nusantara_Receipt_${System.currentTimeMillis()}"
            val printAdapter = webViewToPrint.createPrintDocumentAdapter(jobName)
            printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Gagal mencetak: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveFileToDownloads(base64Data: String, mimeType: String, fileName: String) {
        try {
            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { os ->
                        os.write(bytes)
                    }
                    Toast.makeText(this, "File disimpan ke Downloads: $fileName", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Gagal menyimpan file: $fileName", Toast.LENGTH_SHORT).show()
                }
            } else {
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { os ->
                    os.write(bytes)
                }
                Toast.makeText(this, "File disimpan ke Downloads: $fileName", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error simpan file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    class WebAppInterface(private val activity: MainActivity, private val webView: WebView) {
        @JavascriptInterface
        fun printReceipt() {
            activity.runOnUiThread {
                activity.printWebView(webView)
            }
        }

        @JavascriptInterface
        fun saveFile(base64Data: String, mimeType: String, fileName: String) {
            activity.runOnUiThread {
                activity.saveFileToDownloads(base64Data, mimeType, fileName)
            }
        }
    }
}