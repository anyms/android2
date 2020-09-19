package app.spidy.tamilmemecreator.activities

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import app.spidy.tamilmemecreator.R
import app.spidy.tamilmemecreator.utils.FileChooser
import com.yalantis.ucrop.UCrop
import java.io.File


class FileChooserActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_chooser)
        FileChooser(this).choose(FileChooser.RESULT_FILE_CHOOSER)
    }

    private fun startCrop(imageUri: Uri) {
        val uCrop = UCrop.of(imageUri, Uri.fromFile(File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.absolutePath, "tmp.jpg")))
        uCrop.start(this)
    }

    private fun sendImageBack(imageUri: Uri) {
        val intent = Intent()
        intent.data = imageUri
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && data?.data != null) {
            if (requestCode == FileChooser.RESULT_FILE_CHOOSER) {
                startCrop(data.data!!)
            } else if (requestCode == UCrop.REQUEST_CROP) {
                sendImageBack(data.data!!)
            } else {
                finish()
            }
        } else {
            finish()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}

