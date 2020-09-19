package app.spidy.memecreator.activities

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.room.Room
import androidx.viewpager.widget.ViewPager
import app.spidy.kotlinutils.ignore
import app.spidy.kotlinutils.toast
import app.spidy.memecreator.R
import app.spidy.memecreator.adapters.ImageSliderAdapter
import app.spidy.memecreator.data.SavedMeme
import app.spidy.memecreator.databases.MemeDatabase
import app.spidy.memecreator.utils.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestOptions.bitmapTransform
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import jp.wasabeef.glide.transformations.BlurTransformation
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread


class ImageSliderActivity : AppCompatActivity() {
    companion object {
        const val URLS = "app.spidy.memecreator.ImageSliderActivity.URLS"
        const val CAPTIONS = "app.spidy.memecreator.ImageSliderActivity.CAPTIONS"
        const val INDEX = "app.spidy.memecreator.ImageSliderActivity.INDEX"
        const val TYPE = "app.spidy.memecreator.ImageSliderActivity.TYPE"
    }

    private lateinit var memeTitleView: TextView
    private lateinit var memeDownBtn: ImageView
    private lateinit var memeShareBtn: ImageView
    private lateinit var memeEditBtn: ImageView
    private lateinit var bgImageView: ImageView
    private lateinit var memeDeleteBtn: ImageView

    private val images = ArrayList<String>()
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_slider)

        val tinyDB = TinyDB(this)
        val database = Room.databaseBuilder(this, MemeDatabase::class.java, "MemeDatabase")
            .fallbackToDestructiveMigration().build()

        val urls = tinyDB.getListString(URLS)
        val captions = tinyDB.getListString(CAPTIONS)
        val memeType = tinyDB.getString(TYPE)
        currentIndex = tinyDB.getInt(INDEX)
        for (url in urls) images.add(url)

        memeTitleView = findViewById(R.id.memeTitleView)
        memeDownBtn = findViewById(R.id.memeDownBtn)
        memeShareBtn = findViewById(R.id.memeShareBtn)
        memeEditBtn = findViewById(R.id.memeEditBtn)
        bgImageView = findViewById(R.id.bgImageView)
        memeDeleteBtn = findViewById(R.id.memeDeleteBtn)
        val pager: ViewPager = findViewById(R.id.viewPager)
        val adapter = ImageSliderAdapter(this, images)
        pager.adapter = adapter
        pager.currentItem = currentIndex
        memeTitleView.text = captions[currentIndex]

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        title = ""

        if (!urls[currentIndex].startsWith("http://") && !urls[currentIndex].startsWith("https://")) {
            memeDeleteBtn.visibility = View.VISIBLE
        }

        Glide.with(this@ImageSliderActivity)
            .load(urls[currentIndex])
            .centerCrop()
            .apply(bitmapTransform(BlurTransformation(25, 3)))
            .into(bgImageView)

        pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                currentIndex = position
                memeTitleView.text = captions[currentIndex]

                Glide.with(this@ImageSliderActivity)
                    .load(urls[currentIndex])
                    .centerCrop()
                    .apply(bitmapTransform(BlurTransformation(25, 3)))
                    .into(bgImageView)
            }
        })

        val fileIO = FileIO(this)
        memeEditBtn.setOnClickListener {
            val dialog = showLoading(getString(R.string.fetching_meme))
            ignore {
                fileIO.deleteFile("__data__", "meme.dat")
            }
            ignore { fileIO.deleteDir("__frames__") }
            if (memeType == "meme") {
                Glide.with(this)
                    .asBitmap()
                    .load(urls[currentIndex])
                    .into(object : CustomTarget<Bitmap>(){
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            val stream = ByteArrayOutputStream()
                            resource.compress(Bitmap.CompressFormat.PNG, 100, stream)
                            val byteArray: ByteArray = stream.toByteArray()
                            fileIO.saveBytes("__data__", "meme.dat", byteArray)
                            dialog.dismiss()
                            val intent = Intent(this@ImageSliderActivity, EditorActivity::class.java)
                            intent.putExtra("isLocal", true)
                            intent.putExtra("isMeme", true)
                            startActivity(intent)
                        }
                        override fun onLoadCleared(placeholder: Drawable?) {}
                    })
            } else if (memeType == "gif") {
                fileIO.saveGif(urls[currentIndex]) {
                    dialog.dismiss()
                    tinyDB.putListString("frame_names", it)
                    val intent = Intent(this, GifEditorActivity::class.java)
                    intent.putExtra("isLocal", true)
                    startActivity(intent)
                }
            }
        }

        memeShareBtn.setOnClickListener {
            val dialog = showLoading(getString(R.string.fetching_meme))
            if (memeType == "meme") {
                Glide.with(this)
                    .asBitmap()
                    .load(urls[currentIndex])
                    .into(object : CustomTarget<Bitmap>(){
                        override fun onResourceReady(bitmap: Bitmap, transition: Transition<in Bitmap>?) {
                            val fileDir = getExternalFilesDir("__share__")
                            assert(fileDir != null)
                            if (!fileDir!!.exists()) {
                                fileDir.mkdirs()
                            }

                            val file = File(fileDir, "share.png")

                            val fOut = FileOutputStream(file)
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fOut)

                            fOut.flush()
                            fOut.close()

                            dialog.dismiss()

                            val imageUri = FileProvider.getUriForFile(this@ImageSliderActivity,
                                applicationContext.packageName + ".provider", file)
                            val sharingIntent = Intent(Intent.ACTION_SEND)
                            sharingIntent.type = "image/*"
                            sharingIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            sharingIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            sharingIntent.putExtra(Intent.EXTRA_STREAM, imageUri)
                            startActivity(Intent.createChooser(sharingIntent, "Share via"))
                        }
                        override fun onLoadCleared(placeholder: Drawable?) {}
                    })
            } else if (memeType == "gif") {
                Glide.with(this)
                    .asGif()
                    .load(urls[currentIndex])
                    .into(object : CustomTarget<GifDrawable>() {
                        override fun onLoadCleared(placeholder: Drawable?) {}
                        override fun onResourceReady(resource: GifDrawable, transition: Transition<in GifDrawable>?) {
                            dialog.dismiss()
                            saveGifAndShare(resource)
                        }
                    })
            }
        }

        memeDownBtn.setOnClickListener {
            PermissionHandler.requestStorage(this, getString(R.string.template_download_storage_permission)) {
                Ads.showInterstitial()
                Ads.loadInterstitial()
                if (urls[currentIndex].startsWith("http://") || urls[currentIndex].startsWith("https://")) {
                    toast(getString(R.string.downloading_started))
                    val ext = getFileName(Uri.parse(urls[currentIndex])).split(".").last()
                    val request = DownloadManager.Request(Uri.parse(urls[currentIndex]))
                        .setTitle(captions[currentIndex])
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                        .setAllowedOverMetered(true)
                        .setAllowedOverRoaming(true)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,  "meme_${UUID.randomUUID()}.$ext")

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        request.setRequiresCharging(false)
                    }

                    val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                    downloadManager.enqueue(request)
                } else {
                    val file = File(urls[currentIndex])
                    saveGif(file.readBytes())
                    toast(getString(R.string.your_meme_saved_to_dcim))
                }
            }
        }

        memeDeleteBtn.setOnClickListener {
            thread {
                database.memeDao().removeSavedMeme(urls[currentIndex])
            }
            ignore {
                File(urls[currentIndex]).delete()
                toast(getString(R.string.delete_success))
                finish()
            }
        }
    }

    private fun saveGif(gif: ByteArray) {
        val fos: OutputStream?
        val fileName = "meme_${UUID.randomUUID()}.gif"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = contentResolver;
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/gif")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { //this one
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            fos = resolver.openOutputStream(imageUri!!)
        } else {
            val imagesDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM).toString()
            val file = File(imagesDir)
            if (!file.exists()) {
                file.mkdir()
            }
            val image = File(imagesDir, fileName);
            fos = FileOutputStream(image)

//            sendBroadcast(Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"
//                    + Environment.getExternalStorageDirectory())))
        }
        fos?.write(gif)
        fos?.flush()
        fos?.close()
    }

    private fun saveGifAndShare(gif: GifDrawable) {
        val fileDir = getExternalFilesDir("__share__")
        assert(fileDir != null)
        if (!fileDir!!.exists()) {
            fileDir.mkdirs()
        }

        val file = File(fileDir, "share.gif")
        gifDrawableToFile(gif, file)
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", file);
        shareFile(uri);
    }

    private fun shareFile(uri: Uri) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "image/gif"

        shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    private fun gifDrawableToFile(gifDrawable: GifDrawable, gifFile: File) {
        val byteBuffer = gifDrawable.buffer
        val output = FileOutputStream(gifFile)
        val bytes = ByteArray(byteBuffer.capacity())
        (byteBuffer.duplicate().clear() as ByteBuffer).get(bytes)
        output.write(bytes, 0, bytes.size)
        output.close()
    }

    private fun getFileName(uri: Uri): String {
        val file = File(uri.path!!);
        return file.name
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (PermissionHandler.isInRequestCode(requestCode)) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                PermissionHandler.execute()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
