package app.spidy.tamilmemecreator.utils

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionHandler {
    const val RESULT_CAMERA_CODE = 11

    private const val STORAGE_PERMISSION_CODE = 1
    private const val LOCATION_PERMISSION_CODE = 2
    private const val CAMERA_PERMISSION_CODE = 3
    private lateinit var m: () -> Unit


    fun isInRequestCode(requestCode: Int): Boolean {
        return requestCode == STORAGE_PERMISSION_CODE ||
                requestCode == LOCATION_PERMISSION_CODE ||
                requestCode == CAMERA_PERMISSION_CODE
    }


    fun requestStorage(context: Context, reason: String, callback: () -> Unit) {

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            callback()
            return
        }

        m = callback


        if (ActivityCompat.shouldShowRequestPermissionRationale((context as Activity), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            val builder = AlertDialog.Builder(context)
            builder.setMessage(reason)
            builder.setPositiveButton("Ok") { dialog, _ ->
                dialog.cancel()
                ActivityCompat.requestPermissions(context, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
                )
            }
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            builder.create().show()
        } else {
            ActivityCompat.requestPermissions(context, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE
            )
        }
    }


    fun requestLocation(context: Context, reason: String, callback: () -> Unit) {

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            callback()
            return
        }

        m = callback


        if (ActivityCompat.shouldShowRequestPermissionRationale((context as Activity), Manifest.permission.ACCESS_FINE_LOCATION)) {
            val builder = AlertDialog.Builder(context)
            builder.setMessage(reason)
            builder.setPositiveButton("Ok") { dialog, _ ->
                dialog.cancel()
                ActivityCompat.requestPermissions(context, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_CODE
                )
            }
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            builder.create().show()
        } else {
            ActivityCompat.requestPermissions(context, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_CODE
            )
        }
    }

    fun requestCamera(context: Context, reason: String, callback: () -> Unit) {

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            callback()
            return
        }

        m = callback


        if (ActivityCompat.shouldShowRequestPermissionRationale((context as Activity), Manifest.permission.CAMERA)) {
            val builder = AlertDialog.Builder(context)
            builder.setMessage(reason)
            builder.setPositiveButton("Ok") { dialog, _ ->
                dialog.cancel()
                ActivityCompat.requestPermissions(context, arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_CODE
                )
            }
            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }
            builder.create().show()
        } else {
            ActivityCompat.requestPermissions(context, arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }
    }

    fun execute() {
        m()
    }
}