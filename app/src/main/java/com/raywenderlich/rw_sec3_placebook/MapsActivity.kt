package com.raywenderlich.rw_sec3_placebook

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.raywenderlich.rw_sec3_placebook.databinding.ActivityMapsBinding

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationRequest: LocationRequest? = null
    private lateinit var binding: ActivityMapsBinding

    companion object {
        private const val REQUEST_LOCATION = 1
        private const val TAG = "MapsActivity"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        setupLocationClient()
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        getCurrentLocation()
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions()
        } else {
            map.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnCompleteListener {
                val location = it.result
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    val update = CameraUpdateFactory.newLatLngZoom(latLng, 16.0f)
                    map.moveCamera(update)
                } else {
                    Log.e(TAG, "No location found")
                }
            }
        }
    }

    private fun setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    //Xin quyền (Permissions)
    private fun requestPermissions() {
        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            requestLocationPermissions()
        } else {
            //Nếu user deny quyền thì hiển thị Toast nói lý do
            Toast.makeText(this, "Hi there! Accept it, Pleasessssssss......", Toast.LENGTH_SHORT)
                .show()
            requestLocationPermissions()
        }
    }

    //Code Xin quyền Location
    private fun requestLocationPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_LOCATION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                Log.e(TAG, "Location permission denied")
                requestPermissions()
            }
        }
    }

    //getCurrentLocation() với Location Request
    private fun getCurrentLocation_LocationRequest() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions()
        } else {
            if (locationRequest == null) {
                locationRequest = LocationRequest.create()
                locationRequest?.let { locationRequest ->
                    /*This provides a general guide to how accurate the locations should be. The following options are allowed:
                        PRIORITY_BALANCED_POWER_ACCURACY: Use this setting if you only need accuracy to the city block level, which is around 40-100 meters. This uses very little power and only polls for location updates every 20 seconds or so. The system is likely to only use Wi-Fi or a cell tower to determine your location.
                        PRIORITY_HIGH_ACCURACY: Use this setting if you need the most accuracy possible, normally within 10 meters. This uses the most battery power and typically polls for locations about every 5 seconds.
                        PRIORITY_LOW_POWER: Use this setting if you only need accuracy at the city level within 10 kilometers. This uses a minimal amount of battery power.
                        PRIORITY_NO_POWER: You normally only use this setting if your app can live with or without location data. It will not actively request any location from the system but will return a location if another app is requesting location data.
                        Here, you set priority to LocationRequest.PRIORITY_HIGH_ACCURACY so it’ll return the most accurate location possible. In the emulator, anything less than PRIORITY_HIGH_ACCURACY may not trigger any updates to occur.
                    * */
                    locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

                    /*This lets you specify the desired interval in milliseconds to return updates. This is simply a hint to the system, and if other apps have requested faster updates, your app gets the updates at that rate as well.
                      Here, you set the requested update interval to 5 seconds by setting interval to 5000.
                    * */
                    locationRequest.interval = 5000

                    /*This sets the shortest interval in milliseconds that your app is capable of handling.
                    Since other apps can affect the update interval, this sets a hard limit on how often you’ll receive updates.
                    Here, you set the shortest interval to 1 second with locationRequest.fastestInterval = 1000.
                    * */
                    locationRequest.fastestInterval = 1000

                    /*The fused location provider calls LocationCallBack.onLocationResult when it has a new location ready.
                    You define a LocationCallBack object with onLocationResult().
                    You use this opportunity to update the map to center on the new location.
                    Although onLocationResult() receives a list of locations that you could use to center the map, you just call the existing getCurrentLocation() to grab the latest location and center the map.
                    * */
                    val locationCallback = object : LocationCallback() {
                        override fun onLocationResult(p0: LocationResult) {
                            getCurrentLocation_LocationRequest()

                            //Xóa request location
                            //fusedLocationClient.removeLocationUpdates(this)
                        }
                    }
                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback, null
                    )
                }
            }
            fusedLocationClient.lastLocation.addOnCompleteListener {
                val location = it.result
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    map.clear()
                    map.addMarker(MarkerOptions().position(latLng).title("Ban O Day Ne!!!"))
                    val update = CameraUpdateFactory.newLatLngZoom(latLng, 16.0f)
                    map.moveCamera(update)
                } else {
                    Log.e(TAG, "No location found")
                }
            }
        }
    }
}