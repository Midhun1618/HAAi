package com.voxcom.haai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.webkit.*
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*

class EmergencyActivity : AppCompatActivity() {

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

        // Initially hide WebView
        mapWebView.visibility = View.GONE

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        setupMapWebView()

        btnHospital.setOnClickListener {
            loadMapWithHospitals()
        }

        btnCall.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:112")
            startActivity(intent)
        }
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

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url.toString()

                if (url.contains("google.com/maps")) {
                    return try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                placeholder.visibility = View.GONE
                mapWebView.visibility = View.VISIBLE
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                Toast.makeText(this@EmergencyActivity, "Failed to load map", Toast.LENGTH_SHORT).show()
                placeholder.visibility = View.GONE
            }
        }
    }

    private fun loadMapWithHospitals() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {

            placeholder.visibility = View.VISIBLE

            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        injectOsmHtml(location.latitude, location.longitude)
                    } else {
                        requestFreshLocation()
                    }
                }
            } catch (e: SecurityException) {
                Toast.makeText(this, "Location permission error", Toast.LENGTH_SHORT).show()
            }

        } else {
            checkLocationPermission()
        }
    }

    private fun injectOsmHtml(lat: Double, lng: Double) {
        val html = """ <!DOCTYPE html> <html> <head> <meta charset="utf-8" /> <meta name="viewport" content="width=device-width, initial-scale=1.0"> <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" /> <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>

```
    <style>
        body { margin: 0; font-family: sans-serif; }
        #map { height: 60vh; width: 100%; }
        #list { height: 40vh; overflow-y: auto; padding: 10px; }
        .item { padding: 10px; border-bottom: 1px solid #ddd; }
        .btn { background: green; color: white; padding: 5px 10px; text-decoration: none; }
    </style>
</head>
<body>

    <div id="map"></div>
    <div id="list">Loading hospitals...</div>

    <script>
        var map = L.map('map').setView([$lat, $lng], 14);

        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png').addTo(map);

        L.marker([$lat, $lng]).addTo(map).bindPopup("You are here");

        var query = '[out:json];node["amenity"~"hospital|clinic"](around:8000,$lat,$lng);out;';
        var url = 'https://overpass-api.de/api/interpreter?data=' + encodeURIComponent(query);

        fetch(url)
        .then(res => res.json())
        .then(data => {
            var list = document.getElementById('list');
            list.innerHTML = "";

            if (!data.elements || data.elements.length === 0) {
                list.innerHTML = "No hospitals found nearby.";
                return;
            }

            data.elements.forEach(el => {
                var name = el.tags && el.tags.name ? el.tags.name : "Hospital";
                var lat2 = el.lat;
                var lon2 = el.lon;

                L.marker([lat2, lon2]).addTo(map)
                    .bindPopup(name);

                var item = document.createElement("div");
                item.className = "item";

                var link = "https://www.google.com/maps/dir/?api=1&destination=" + lat2 + "," + lon2;

                item.innerHTML =
                    "<b>" + name + "</b><br><br>" +
                    "<a class='btn' href='" + link + "'>Navigate</a>";

                list.appendChild(item);
            });
        })
        .catch(err => {
            document.getElementById('list').innerHTML = "Error loading hospitals.";
        });
    </script>

</body>
</html>
""".trimIndent()

        mapWebView.loadDataWithBaseURL(
            "https://unpkg.com",
            html,
            "text/html",
            "UTF-8",
            null
        )

    }


    private fun requestFreshLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setMaxUpdates(1)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                request,
                object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        fusedLocationClient.removeLocationUpdates(this)
                        result.lastLocation?.let {
                            injectOsmHtml(it.latitude, it.longitude)
                        } ?: run {
                            Toast.makeText(
                                this@EmergencyActivity,
                                "Unable to get location",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            Toast.makeText(this, "Location error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            loadMapWithHospitals()
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        mapWebView.destroy()
        super.onDestroy()
    }

}
