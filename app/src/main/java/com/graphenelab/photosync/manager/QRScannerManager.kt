package com.graphenelab.photosync.manager

import androidx.activity.result.ActivityResultLauncher
import com.graphenelab.photosync.manager.interfaces.IQRScanner
import com.journeyapps.barcodescanner.ScanOptions
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

/**
 * Concrete implementation of [IQRScanner].
 * Manages the scanning of QR codes using the ZXing library.
 */
@ViewModelScoped
class QRScanner @Inject constructor() : IQRScanner {
    private var barcodeLauncher: ActivityResultLauncher<ScanOptions>? = null

    override fun setLauncher(launcher: ActivityResultLauncher<ScanOptions>) {
        this.barcodeLauncher = launcher
    }

    override fun startScan() {
        val options = ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Scan a QR code")
            setCameraId(0)
            setBeepEnabled(false)
            setBarcodeImageEnabled(true)
            setOrientationLocked(false)
        }
        barcodeLauncher?.launch(options)
    }
}