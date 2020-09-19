package app.spidy.spidy.viewmodels

import android.app.Application
import android.content.Context
import android.os.CountDownTimer
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.room.Room
import app.spidy.kotlinutils.debug
import app.spidy.kotlinutils.onUiThread
import app.spidy.spidy.data.Script
import app.spidy.spidy.databases.SpidyDatabase
import kotlin.concurrent.thread

class MainActivityViewModel(application: Application): AndroidViewModel(application) {
    private val context: Context = application

    val viewUpdateSwitch = MutableLiveData<Boolean>()
    val isBackgroundProcessBinned = MutableLiveData<Boolean>()
    fun updateView() {
        viewUpdateSwitch.value = !(viewUpdateSwitch.value ?: false)
    }
}