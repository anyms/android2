package app.spidy.spidy.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class EditorActivityViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context = application

    var isAlreadyCreated = false
    var isTest = false
    var scriptTitle: String? = null
    var code = MutableLiveData<String>()
    var lastUrl: String? = null

    var isToolboxShown = MutableLiveData<Boolean>()
}