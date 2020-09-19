package app.spidy.spidy.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import app.spidy.spidy.utils.IO

class BrowserActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context = application

    val isRecordButtonVisible = MutableLiveData<Boolean>()
    val isTerminalButtonVisible = MutableLiveData<Boolean>()
    val logs = MutableLiveData<String>()
    var code: String? = null

    var isFabVisible = MutableLiveData<Boolean>()

}