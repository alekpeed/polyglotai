package com.polyglotai.desktop

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

// Android WebView permission requests (getUserMedia) go through a separate layer from the OS
// runtime permission dialog — both must be satisfied independently. This bridges the two:
// request the OS permission if not already held, then grant/deny the WebView's
// PermissionRequest based on the outcome. Used by the mic-recording flow in
// src/ai/voice.ts (Conversation/Live Interpreter) and Pronunciation.tsx.
class MainActivity : TauriActivity() {
  private var pendingWebViewPermissionRequest: PermissionRequest? = null

  private val requestMicPermission =
    registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      val request = pendingWebViewPermissionRequest
      pendingWebViewPermissionRequest = null
      if (request == null) return@registerForActivityResult
      if (granted) request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) else request.deny()
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
  }

  override fun onWebViewCreate(webView: WebView) {
    super.onWebViewCreate(webView)
    webView.webChromeClient = object : WebChromeClient() {
      override fun onPermissionRequest(request: PermissionRequest) {
        if (!request.resources.contains(PermissionRequest.RESOURCE_AUDIO_CAPTURE)) {
          request.deny()
          return
        }
        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO)
          == PackageManager.PERMISSION_GRANTED
        ) {
          request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
          return
        }
        pendingWebViewPermissionRequest = request
        requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
      }
    }
  }
}
