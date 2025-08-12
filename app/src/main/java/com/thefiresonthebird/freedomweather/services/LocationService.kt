package com.thefiresonthebird.freedomweather.services

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.OnTokenCanceledListener

private const val TAG = "LocationService"

/**
 * Service class for handling location-related operations including:
 * - Permission checking and requesting
 * - Current location retrieval
 * - Geocoding coordinates to addresses
 */
class LocationService(private val context: Context) {
    
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    
    /**
     * Check if location permissions are granted
     * @return true if permissions are granted, false otherwise
     */
    fun hasLocationPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Get current location using GPS or network
     * @param onLocationReceived callback with the resolved location string
     * @param onError callback when location retrieval fails
     */
    @SuppressLint("MissingPermission")
    fun getCurrentLocation(
        onLocationReceived: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        Log.d(TAG, "getCurrentLocation: Attempting to get current location")
        
        if (!hasLocationPermissions()) {
            Log.w(TAG, "getCurrentLocation: Location permissions not granted")
            onError("Location permission denied")
            return
        }
        
        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                object : CancellationToken() {
                    override fun onCanceledRequested(listener: OnTokenCanceledListener): CancellationToken {
                        return this
                    }
                    
                    override fun isCancellationRequested(): Boolean {
                        return false
                    }
                }
            ).addOnSuccessListener { location: Location? ->
                location?.let {
                    Log.d(TAG, "getCurrentLocation: Location received - lat: ${it.latitude}, lon: ${it.longitude}")
                    getAddressFromLocation(it, onLocationReceived, onError)
                } ?: run {
                    Log.w(TAG, "getCurrentLocation: Location is null")
                    onError("Location unavailable")
                }
            }.addOnFailureListener { exception ->
                Log.e(TAG, "getCurrentLocation: Failed to get location", exception)
                onError("Location service error")
            }
        } catch (e: Exception) {
            Log.e(TAG, "getCurrentLocation: Exception occurred", e)
            onError("Location service error")
        }
    }
    
    /**
     * Convert coordinates to human-readable address
     * @param location the Location object with coordinates
     * @param onAddressResolved callback with the resolved address
     * @param onError callback when geocoding fails
     */
    @SuppressLint("Deprecation")
    private fun getAddressFromLocation(
        location: Location,
        onAddressResolved: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        Log.d(TAG, "getAddressFromLocation: Converting coordinates to address")
        
        try {
            val geocoder = Geocoder(context)
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            
            addresses?.firstOrNull()?.let { address ->
                val city = address.locality ?: "Unknown City"
                val state = address.adminArea ?: ""
                val country = address.countryName ?: ""
                
                val resolvedLocation = when {
                    state.isNotEmpty() -> "$city, $state"
                    country.isNotEmpty() -> "$city, $country"
                    else -> city
                }
                
                Log.d(TAG, "getAddressFromLocation: Address resolved to: $resolvedLocation")
                onAddressResolved(resolvedLocation)
            } ?: run {
                Log.w(TAG, "getAddressFromLocation: No address found for coordinates")
                onError("Unknown location")
            }
        } catch (e: Exception) {
            Log.e(TAG, "getAddressFromLocation: Geocoding failed", e)
            // Fallback to coordinates if geocoding fails
            val coordinateLocation = "%.2f, %.2f".format(location.latitude, location.longitude)
            onAddressResolved(coordinateLocation)
        }
    }
    
    /**
     * Check if location services are available on the device
     * @return true if location services are available, false otherwise
     */
    @SuppressLint("MissingPermission")
    fun isLocationServicesAvailable(): Boolean {
        return try {
            fusedLocationClient.lastLocation.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "isLocationServicesAvailable: Error checking location services", e)
            false
        }
    }
    
    /**
     * Get the current location service instance
     * @return the FusedLocationProviderClient instance
     */
    fun getLocationClient(): FusedLocationProviderClient {
        return fusedLocationClient
    }
    
    /**
     * Check if the device has Google Play Services available
     * @return true if Google Play Services is available, false otherwise
     */
    fun hasGooglePlayServices(): Boolean {
        return try {
            com.google.android.gms.common.GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context) == com.google.android.gms.common.ConnectionResult.SUCCESS
        } catch (e: Exception) {
            Log.e(TAG, "hasGooglePlayServices: Error checking Google Play Services", e)
            false
        }
    }
}
