package com.voxcom.haai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*

class EmergencyActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var mapWebView: WebView
    private lateinit var placeholder: ImageView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val LOCATION_PERMISSION_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency)

        val btnHospital = findViewById<Button>(R.id.btnHospital)
        val btnCall = findViewById<Button>(R.id.btnCall)

        mapWebView = findViewById(R.id.mapWebView)
        placeholder = findViewById(R.id.map_placeholder)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        webView.apply {
            webViewClient = android.webkit.WebViewClient()
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
        }

        setupMapWebView()
        setupBackNavigation()

        btnHospital.setOnClickListener {
            loadMapWithHospitals()
        }

        btnCall.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:112")
            startActivity(intent)
        }



    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    webView.visibility == View.VISIBLE && webView.canGoBack() -> webView.goBack()
                    webView.visibility == View.VISIBLE -> {
                        webView.visibility = View.GONE
                        mapWebView.visibility = View.VISIBLE
                    }
                    else -> {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })
    }

    private fun setupMapWebView() {
        val settings = mapWebView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        mapWebView.webChromeClient = WebChromeClient()
        mapWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url == null) return false

                // FIX: Check for Google Maps intent links
                if (url.contains("maps.google.com") || url.contains("google.com/maps")) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                        return true
                    } catch (e: Exception) {
                        return false
                    }
                }
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                placeholder.visibility = View.GONE
                mapWebView.visibility = View.VISIBLE
            }
        }
    }

    private fun loadMapWithHospitals() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {

            placeholder.visibility = View.VISIBLE

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    injectOsmHtml(location.latitude, location.longitude)
                } else {
                    requestFreshLocation()
                }
            }
        } else {
            checkLocationPermission()
        }
    }

    private fun injectOsmHtml(lat: Double, lng: Double) {
        val html = """
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
        <style>
            body { margin: 0; padding: 0; font-family: sans-serif; overflow: hidden; height: 100vh; display: flex; flex-direction: column; }
            #map { flex: 6; width: 100vw; }
            #list-container { flex: 4; width: 100vw; overflow-y: auto; background: white; border-top: 2px solid #ccc; }
            .list-header { padding: 12px; background: #f8f9fa; font-weight: bold; border-bottom: 1px solid #ddd; position: sticky; top: 0; }
            .hospital-item { padding: 15px; border-bottom: 1px solid #eee; display: flex; justify-content: space-between; align-items: center; }
            .hospital-info { flex: 1; }
            .hospital-name { font-weight: bold; color: #c0392b; margin-bottom: 4px; }
            .hospital-type { font-size: 12px; color: #666; text-transform: capitalize; }
            .btn-nav { padding: 8px 12px; background: #27ae60; color: white; text-decoration: none; border-radius: 4px; font-size: 13px; font-weight: bold; }
        </style>
    </head>
    <body>
        <div id="map"></div>
        <div id="list-container">
            <div class="list-header" id="status">Finding nearby hospitals...</div>
            <div id="hospital-list"></div>
        </div>

        <script>
            var map = L.map('map', { zoomControl: false }).setView([$lat, $lng], 14);
            L.control.zoom({ position: 'topright' }).addTo(map);

            L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
                attribution: '© OpenStreetMap'
            }).addTo(map);

            L.circleMarker([$lat, $lng], { radius: 8, fillColor: '#3498db', color: '#fff', weight: 2, fillOpacity: 1 })
             .addTo(map).bindPopup("<b>You are here</b>");

            var query = '[out:json][timeout:25];(node["amenity"~"hospital|clinic"](around:8000,$lat,$lng);way["amenity"~"hospital|clinic"](around:8000,$lat,$lng););out center;';
            var url = 'https://overpass-api.de/api/interpreter?data=' + encodeURIComponent(query);

            fetch(url).then(res => res.json()).then(data => {
                const listBody = document.getElementById('hospital-list');
                const statusHeader = document.getElementById('status');
                
                if(!data.elements || data.elements.length === 0) {
                    statusHeader.innerText = "No hospitals found within 8km.";
                    return;
                }

                statusHeader.innerText = "Found " + data.elements.length + " Medical Centers";

                data.elements.forEach(el => {
                    var eLat = el.lat || (el.center && el.center.lat);
                    var eLng = el.lon || (el.center && el.center.lon);
                    var name = (el.tags && el.tags.name) ? el.tags.name : "Medical Center";
                    var type = (el.tags && el.tags.amenity) ? el.tags.amenity : "Facility";
                    
                    L.marker([eLat, eLng]).addTo(map).bindPopup("<b>" + name + "</b>");

                    var googleMapsUrl = "https://www.google.com/maps/dir/?api=1&destination=" + eLat + "," + eLng;
                    
                    var item = document.createElement('div');
                    item.className = 'hospital-item';
                    item.innerHTML = '<div class="hospital-info" onclick="focusMap(' + eLat + ',' + eLng + ')">' +
                                     '<div class="hospital-name">' + name + '</div>' +
                                     '<div class="hospital-type">' + type + '</div>' +
                                     '</div>' +
                                     '<a class="btn-nav" href="' + googleMapsUrl + '">DIR</a>';
                    listBody.appendChild(item);
                });
            }).catch(err => {
                document.getElementById('status').innerText = "Error loading data.";
            });

            function focusMap(lat, lng) {
                map.flyTo([lat, lng], 16);
            }
        </script>
    </body>
    </html>
    """.trimIndent()

        mapWebView.loadDataWithBaseURL("https://unpkg.com", html, "text/html", "UTF-8", null)
    }

    private fun requestFreshLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).setMaxUpdates(1).build()
        fusedLocationClient.requestLocationUpdates(request, object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                fusedLocationClient.removeLocationUpdates(this)
                result.lastLocation?.let { injectOsmHtml(it.latitude, it.longitude) }
            }
        }, Looper.getMainLooper())
    }


    private fun checkLocationPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadMapWithHospitals()
        }
    }
}