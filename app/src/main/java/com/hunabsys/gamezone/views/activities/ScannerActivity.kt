package com.hunabsys.gamezone.views.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat
import android.util.Log
import com.rollbar.android.Rollbar
import me.dm7.barcodescanner.zbar.Result
import me.dm7.barcodescanner.zbar.ZBarScannerView

class ScannerActivity : Activity(), ZBarScannerView.ResultHandler {

    private lateinit var scannerView: ZBarScannerView

    private val tag = ScannerActivity::class.java.simpleName

    companion object {
        const val REQUEST_CODE_CAMERA = 2
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        scannerView = ZBarScannerView(this)
        setContentView(scannerView)

        hideStatusBar()
    }

    override fun onResume() {
        super.onResume()

        scannerView.setResultHandler(this)
        scannerView.startCamera()
    }

    override fun onPause() {
        super.onPause()

        returnEmptyResult()
    }

    override fun handleResult(rawResult: Result) {
        try {
            // Do something with the result here
            Log.d(tag, "------- Format: " + rawResult.barcodeFormat.name)
            Log.d(tag, "------- Code: " + rawResult.contents)

            scannerView.resumeCameraPreview(this)
            scannerView.stopCamera()

            returnResult(rawResult.contents, RESULT_OK)
        } catch (ex: Exception) {
            Log.e(tag, "Attempting to handle scan result", ex)
        }
    }

    private fun hideStatusBar() {
        window.setFlags(AccessibilityNodeInfoCompat.ACTION_NEXT_HTML_ELEMENT,
                AccessibilityNodeInfoCompat.ACTION_NEXT_HTML_ELEMENT)
        window.decorView.systemUiVisibility = 3328
    }

    private fun returnResult(code: String, resultCode: Int) {
        val intent = Intent()
        intent.putExtra(EXTRA_RESULT_CODE, code)
        setResult(resultCode, intent)
        finish()
    }

    private fun returnEmptyResult() {
        try {
            scannerView.stopCamera()
            returnResult("", RESULT_CANCELED)
        } catch (ex: Exception) {
            Log.e(tag, "Attempting to stop camera and return empty result", ex)
            Rollbar.instance().error(ex, tag)
        }
    }
}