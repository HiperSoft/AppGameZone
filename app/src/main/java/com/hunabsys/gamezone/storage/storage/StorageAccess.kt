package com.hunabsys.gamezone.storage.storage

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.util.Log
import java.io.File

class StorageAccess {
    fun deleteLatestImageFile() {
        val filePath = File(Environment.getExternalStorageDirectory()
                .toString() + "/DCIM/Camera")

        val files = filePath.listFiles()

        Log.e("Quantity files:", files.size.toString())

        if (files.isNotEmpty()) {
            val filename = files[0].name
            if (files[0].delete()) {
                Log.e("Deleted Image:", filename)
            } else {
                Log.e("Couldn't delete image:", filename)
            }
        } else {
            Log.e("Error", "Images Folder is empty")
        }
    }

    fun deleteAllImageFiles(tag: String, context: Context) {
        val storageDir: File = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        if (storageDir.deleteRecursively()) {
            Log.e(tag, "Images deleted")
        }
    }

    fun checkInternalStorageAvailable(): Boolean {
        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        val bytesAvailable = stat.blockSizeLong * stat.availableBlocksLong

        val megAvailable = bytesAvailable / (1024 * 1024)
        Log.e("Available MB", megAvailable.toString())

        return megAvailable > 100L
    }
}