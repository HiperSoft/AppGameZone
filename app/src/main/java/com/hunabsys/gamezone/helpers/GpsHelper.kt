package com.hunabsys.gamezone.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.hunabsys.gamezone.R

class GpsHelper {

    private var alertDialog: AlertDialog? = null
    private var availableGps: Boolean = false
    private lateinit var locationManager: LocationManager

    companion object {
        var locationGps: Location? = null
        var statusLocation = 0
    }

    fun startLocationService(tag: String, context: Context) {
        this.locationManager = context.getSystemService(AppCompatActivity.LOCATION_SERVICE)
                as LocationManager
        availableGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

        if (availableGps) {
            checkGpsLocation(tag, locationManager)
        } else {
            if (statusLocation == 0) {
                val message = context.getString(R.string.message_disable_location)

                alertDialog = AlertDialog.Builder(context)
                        .setMessage(message)
                        .setPositiveButton(R.string.message_enable_location) { dialog, which ->
                            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                            statusLocation = 1
                            startLocationService(tag, context)
                        }
                        .setNegativeButton(R.string.action_cancel) { dialog, which -> dialog.dismiss() }.show()
            }
            statusLocation = 0
            locationGps = null
        }
    }

    @SuppressLint("MissingPermission")
    fun checkGpsLocation(tag: String, locationManager: LocationManager) {
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000,
                0F, object : LocationListener {
            override fun onLocationChanged(location: Location?) {
                if (location != null) {
                    locationGps = location

                    if (locationGps == null) {
                        Log.e(tag, "NULL GPS location")
                    }
                }
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
            }

            override fun onProviderEnabled(provider: String?) {
            }

            override fun onProviderDisabled(provider: String?) {
            }
        })

        val localGpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (localGpsLocation != null)
            locationGps = localGpsLocation
    }
}