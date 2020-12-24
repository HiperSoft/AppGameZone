package com.hunabsys.gamezone.helpers

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat

/**
 * Helper for requesting permissions to user.
 * Created by Silvia Valdez on 14/02/2018.
 */
class PermissionsHelper(val activity: Activity) {

    companion object {
        const val REQUEST_PERMISSIONS = 1
    }

    fun validatePermissionResult(grantResults: IntArray): Boolean {
        var position = 0
        for (permission in grantResults) {
            if (grantResults[position] == PackageManager.PERMISSION_GRANTED) {
                position++
            } else {
                return false
            }
        }
        return true
    }

    fun requestPhonePermissions(): Boolean {
        return if (checkPhonePermission()) {
            true
        } else {
            val permission = arrayOf(Manifest.permission.READ_PHONE_STATE)

            ActivityCompat.requestPermissions(activity, permission, REQUEST_PERMISSIONS)
            false
        }
    }

    fun requestAllPermissions(): Boolean {
        return if (checkCameraPermissions()
                && checkLocationPermissions()
                && checkExternalStoragePermissions()) {
            true
        } else {
            // If there are no permissions granted, request them
            val permissions = arrayOf(Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION)

            ActivityCompat.requestPermissions(activity, permissions, REQUEST_PERMISSIONS)
            false
        }
    }

    private fun checkCameraPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkLocationPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkExternalStoragePermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkPhonePermission(): Boolean {
        return ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
    }
}