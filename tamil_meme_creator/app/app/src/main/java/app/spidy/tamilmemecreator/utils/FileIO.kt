package app.spidy.tamilmemecreator.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.Environment
import app.spidy.tamilmemecreator.BuildConfig
import com.bumptech.glide.Glide
import com.bumptech.glide.gifdecoder.StandardGifDecoder
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import java.io.*
import java.nio.ByteBuffer


class FileIO(private val context: Context) {

    fun saveTextFile(fileName: String, content: String) {
        val fileDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (BuildConfig.DEBUG && fileDir == null) {
            error("Assertion failed")
        }
        if (!fileDir!!.exists()) {
            fileDir.mkdirs()
        }

        val file = File(fileDir, fileName)
        try {
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    throw IOException("Unable to create file")
                }
            }
            val fos = FileOutputStream(file)
            val data = content.toByteArray()
            fos.write(data)
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun saveBytes(folderName: String, fileName: String, content: ByteArray): String {
        val fileDir = context.getExternalFilesDir(folderName)
        if (BuildConfig.DEBUG && fileDir == null) {
            error("Assertion failed")
        }
        if (!fileDir!!.exists()) {
            fileDir.mkdirs()
        }

        val file = File(fileDir, fileName)
        try {
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    throw IOException("Unable to create file")
                }
            }
            val fos = FileOutputStream(file)
            fos.write(content)
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return file.absolutePath
    }

    fun getUri(folderName: String, fileName: String): String {
        val fileDir = context.getExternalFilesDir(folderName)
        val file = File(fileDir, fileName)
        return file.absolutePath
    }

    fun saveBitmap(folderName: String, fileName: String, bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val byteArray = stream.toByteArray()

        return saveBytes(folderName, fileName, byteArray)
    }

    fun readBitmap(folderName: String, fileName: String): Bitmap? {
        val bytes = readBytes(folderName, fileName)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        return bitmap
    }

    fun saveBytes(folderName: String, fileName: String, content: ByteBuffer): String {
        val fileDir = context.getExternalFilesDir(folderName)
        assert(fileDir != null)
        if (!fileDir!!.exists()) {
            fileDir.mkdirs()
        }

        val file = File(fileDir, fileName)
        try {
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    throw IOException("Unable to create file")
                }
            }
            val fos = FileOutputStream(file).channel
            fos.write(content)
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return file.absolutePath
    }

    fun readBytes(folderName: String, fileName: String): ByteArray {
        val fileDir = context.getExternalFilesDir(folderName)
        val file = File(fileDir, fileName)
        val fos = FileInputStream(file)
        val data = fos.readBytes()
        fos.close()
        return data
    }

    fun deleteFile(folderName: String, fileName: String) {
        val fileDir = context.getExternalFilesDir(folderName)
        val file = File(fileDir, fileName)
        if (file.exists()) {
            file.delete()
        }
    }

    fun deleteDir(folderName: String) {
        val fileDir = context.getExternalFilesDir(folderName)
        if (fileDir != null && fileDir.isDirectory) {
            val childs = fileDir.list()
            if (childs != null) {
                for (child in childs) {
                    File(fileDir, child).delete()
                }
            }
        }
    }


    fun saveGif(url: String, callback: (frameNames: List<String>) -> Unit) {
        Glide.with(context)
            .asGif()
            .load(url)
            .into(object : CustomTarget<GifDrawable>() {
                override fun onLoadCleared(placeholder: Drawable?) {}
                override fun onResourceReady(
                    resource: GifDrawable,
                    transition: Transition<in GifDrawable>?
                ) {
                    val bitmaps = ArrayList<Bitmap>()
                    val frameNames = ArrayList<String>()

                    val gifState = resource.constantState!!
                    val frameLoader = gifState::class.java.getDeclaredField("frameLoader")
                    frameLoader.isAccessible = true;
                    val gifFrameLoader = frameLoader.get(gifState)

                    val gifDecoder = gifFrameLoader::class.java.getDeclaredField("gifDecoder")
                    gifDecoder.isAccessible = true
                    val standardGifDecoder = gifDecoder.get(gifFrameLoader) as StandardGifDecoder
                    for (i in 0 until standardGifDecoder.frameCount) {
                        standardGifDecoder.advance();
                        bitmaps.add(standardGifDecoder.nextFrame!!)
                    }

                    bitmaps.forEachIndexed { index, bitmap ->
                        val frameName = "frame_$index.jpg"
                        saveBitmap("__frames__", frameName, bitmap)
                        frameNames.add(frameName)
                    }

                    callback(frameNames)
                }
            })
    }
}