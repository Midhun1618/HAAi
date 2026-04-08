package com.voxcom.haai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class EmergencyActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency)

        val btnHospital = findViewById<Button>(R.id.btnHospital)
        val btnCall = findViewById<Button>(R.id.btnCall)
        val btnGuidelines = findViewById<Button>(R.id.btnGuidelines)
        webView = findViewById(R.id.webView)

        // 🗺️ Map setup
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment

        mapFragment.getMapAsync(this)

        // 📍 Open Google Maps externally
        btnHospital.setOnClickListener {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("geo:0,0?q=hospitals near me")
            )
            startActivity(intent)
        }

        // 📞 Call Emergency
        btnCall.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:112")
            startActivity(intent)
        }

        // 🌐 Show Guidelines in WebView
        btnGuidelines.setOnClickListener {
            webView.visibility = View.VISIBLE

            webView.webViewClient = WebViewClient()

            val settings: WebSettings = webView.settings
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true

            webView.loadUrl("https://www.mohfw.gov.in/")
        }
    }

    // 🗺️ Map Ready Callback
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Default location (India)
        val defaultLocation = LatLng(20.5937, 78.9629)

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 5f))

        mMap.addMarker(
            MarkerOptions()
                .position(defaultLocation)
                .title("Nearby Hospitals")
        )

        // Enable current location if permission granted
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
        }
    }

    // 🔙 Back press handling for WebView
    override fun onBackPressed() {
        if (webView.visibility == View.VISIBLE && webView.canGoBack()) {
            webView.goBack()
        } else if (webView.visibility == View.VISIBLE) {
            webView.visibility = View.GONE
        } else {
            super.onBackPressed()
        }
    }
}