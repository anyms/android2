package app.spidy.lankanews.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView
import app.spidy.kotlinutils.TinyDB
import app.spidy.lankanews.R
import app.spidy.lankanews.utils.API
import app.spidy.lankanews.utils.C
import kotlinx.android.synthetic.main.activity_language_select.*

class LanguageSelectActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_select)

        setSupportActionBar(toolbar)

        val tinyDB = TinyDB(applicationContext)
        val webView = WebView(this)
        API.userAgent = webView.settings.userAgentString

        if(tinyDB.getString("lang") != null) {
            startMainActivity()
        }

        langEnglish.setOnClickListener {
            tinyDB.putString("lang", "")
            startMainActivity()
        }

        langSinhala.setOnClickListener {
            tinyDB.putString("lang", "sinhala")
            startMainActivity()
        }

        langTamil.setOnClickListener {
            tinyDB.putString("lang", "tamil")
            startMainActivity()
        }
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}