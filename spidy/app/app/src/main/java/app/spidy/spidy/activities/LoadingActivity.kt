package app.spidy.spidy.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import app.spidy.kotlinutils.TinyDB
import app.spidy.kotlinutils.onUiThread
import app.spidy.spidy.R
import app.spidy.spidy.utils.IO
import app.spidy.spidy.utils.Injector
import kotlin.concurrent.thread

class LoadingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        Injector.HAMMER_JS = IO.readFromAsset(this, "libs/hammer.js")!!
        Injector.SPIDY_JS = IO.readFromAsset(this, "spidy.js")!!
        val tinyDB = TinyDB(this)
        thread {
            Thread.sleep(1000)
            onUiThread {
                if (tinyDB.getBoolean("isGettingStartedShown")) {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    val intent = Intent(this, GettingStartedActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        }
    }
}