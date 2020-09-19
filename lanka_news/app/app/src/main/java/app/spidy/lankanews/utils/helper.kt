package app.spidy.lankanews.utils

import android.util.DisplayMetrics
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import kotlin.math.sqrt


fun FragmentActivity.isTablet(): Boolean {
    val metrics = DisplayMetrics()
    windowManager.defaultDisplay.getMetrics(metrics)

    val yInches = metrics.heightPixels / metrics.ydpi
    val xInches = metrics.widthPixels / metrics.xdpi
    val diagonalInches = sqrt(xInches * xInches + yInches * yInches.toDouble())
    return diagonalInches >= 6.5
}