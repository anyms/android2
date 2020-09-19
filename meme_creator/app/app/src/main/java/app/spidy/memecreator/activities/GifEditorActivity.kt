package app.spidy.memecreator.activities

import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import app.spidy.kotlinutils.ignore
import app.spidy.kotlinutils.toast
import app.spidy.memecreator.R
import app.spidy.memecreator.adapters.FrameAdapter
import app.spidy.memecreator.adapters.ToolAdapter
import app.spidy.memecreator.data.*
import app.spidy.memecreator.databases.MemeDatabase
import app.spidy.memecreator.fragments.EmojiesBottomSheetFragment
import app.spidy.memecreator.interfaces.EditorListener
import app.spidy.memecreator.interfaces.FrameListener
import app.spidy.memecreator.utils.*
import app.spidy.photoeditor2.CurrentView
import app.spidy.photoeditor2.core.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.load.resource.transcode.GifDrawableBytesTranscoder
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.github.florent37.tutoshowcase.TutoShowcase
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

class GifEditorActivity : AppCompatActivity() {
    companion object {
        const val TAG_BOTTOM_SHEET = "app.spidy.mime.TAG_BOTTOM_SHEET"
    }

    private lateinit var editor: PhotoEditor
    private lateinit var fileChooser: FileChooser
    private lateinit var editorView: PhotoEditorView
    private lateinit var toolRecyclerView: RecyclerView
    private lateinit var frameRecyclerView: RecyclerView
    private lateinit var toolAdapter: ToolAdapter
    private lateinit var frameAdapter: FrameAdapter
    private lateinit var toolbar: Toolbar
    private lateinit var addItemDialog: Dialog
    private lateinit var fontFace: FontFace
    private lateinit var editorViewHolder: LinearLayout
    private lateinit var frame: Frame
    private lateinit var database: MemeDatabase

    private lateinit var tinyDB: TinyDB
    private lateinit var fileIO: FileIO
    private var isLocal: Boolean = false
    private var menu: Menu? = null

    private val tools = ArrayList<Tool>()
    private val frames = ArrayList<Frame>()

    private var uId = ""

    private val defaultTools = listOf(
        Tool(Tool.EDITOR_IMAGE, "Image", R.drawable.ic_background_image),
        Tool(Tool.EDITOR_BACKGROUND_COLOR, "Background Color", R.drawable.ic_background),
        Tool(Tool.IMAGE_ROTATE, "Rotation", R.drawable.ic_rotate)
    )
    private val textTools = listOf(
        Tool(Tool.EDITOR_IMAGE, "Image", R.drawable.ic_background_image),
        Tool(Tool.EDITOR_BACKGROUND_COLOR, "Background Color", R.drawable.ic_background),
        Tool(Tool.IMAGE_ROTATE, "Rotation", R.drawable.ic_rotate),
        Tool(Tool.TEXT_EDIT, "Edit", R.drawable.ic_text_edit),
        Tool(Tool.TEXT_COLOR, "Color", R.drawable.ic_color),
        Tool(Tool.TEXT_SIZE, "Size", R.drawable.ic_font_size),
        Tool(Tool.TEXT_FONT, "Font", R.drawable.ic_font),
        Tool(Tool.TEXT_OUTLINE, "Outline", R.drawable.ic_outline),
        Tool(Tool.TEXT_OPACITY, "Opacity", R.drawable.ic_opacity)
    )
    private val imageTools = listOf(
        Tool(Tool.EDITOR_IMAGE, "Image", R.drawable.ic_background_image),
        Tool(Tool.EDITOR_BACKGROUND_COLOR, "Background Color", R.drawable.ic_background),
        Tool(Tool.IMAGE_ROTATE, "Rotation", R.drawable.ic_rotate),
        Tool(Tool.IMAGE_CHANGE, "Change", R.drawable.ic_image),
        Tool(Tool.IMAGE_OPACITY, "Opacity", R.drawable.ic_opacity)
    )
    private val emojiTools = listOf(
        Tool(Tool.EDITOR_IMAGE, "Image", R.drawable.ic_background_image),
        Tool(Tool.EDITOR_BACKGROUND_COLOR, "Background Color", R.drawable.ic_background),
        Tool(Tool.IMAGE_ROTATE, "Rotation", R.drawable.ic_rotate),
        Tool(Tool.EMOJI_OPACITY, "Opacity", R.drawable.ic_opacity)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gif_editor)

        Ads.showInterstitial()
        Ads.loadInterstitial()

        findViewById<AdView>(R.id.adView).loadAd(AdRequest.Builder().build())

        defaultTools.forEach { tools.add(it) }

        tinyDB = TinyDB(this)
        fileIO = FileIO(this)
        isLocal = intent.getBooleanExtra("isLocal", false)

        if (isLocal) {
            val frameNames = tinyDB.getListString("frame_names")
            for (frameName in frameNames) {
                frames.add( Frame(fileIO.readBitmap("__frames__", frameName)!!) )
            }
        }

        editorView = findViewById(R.id.editorView)
        editor = PhotoEditor.Builder(this, editorView).build()
        fileChooser = FileChooser(this)
        toolRecyclerView = findViewById(R.id.toolRecyclerView)
        frameRecyclerView = findViewById(R.id.frameRecyclerView)
        toolbar = findViewById(R.id.toolbar)
        editorViewHolder = findViewById(R.id.editorViewHolder)

        toolAdapter = ToolAdapter(this, tools, editor, editorViewHolder, fileChooser, editorView)
        toolRecyclerView.adapter = toolAdapter
        toolRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        editorView.source?.setImageDrawable(getDrawable(R.drawable.placeholder))
        switchFrame(0)

        frameAdapter = FrameAdapter(this, frames, object : FrameAdapter.Listener {
            override fun onExitSelectMode() {
                toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
                menu?.getItem(3)?.icon = getDrawable(R.drawable.ic_save)
            }
            override fun onFrameSelected(position: Int) {
                if (frameAdapter.isSelectMode) {
                    menu?.getItem(3)?.icon = getDrawable(R.drawable.ic_select_all)
                    toolbar.setNavigationIcon(R.drawable.ic_done)
                } else {
                    frameListener.onFrameSelected(position)
                }
            }
        })
        frameRecyclerView.adapter = frameAdapter
        frameRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        addEditorListener(editor)
        setSupportActionBar(toolbar)
        title = ""
        addItemDialog = createAddItemDialog()
        fontFace = FontFace(this)

        toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        toolbar.setNavigationOnClickListener {
            if (frameAdapter.isSelectMode) {
                frameAdapter.exitSelectMode()
            } else {
                onBackPressed()
            }
        }

        editor.addImage(getDrawable(R.drawable.logo)!!.toBitmap())
        editor.currentView.view?.apply {
            alpha = 0.3f
            requestLayout()
            layoutParams.height = 60
        }

        database = Room.databaseBuilder(this, MemeDatabase::class.java, "MemeDatabase")
            .fallbackToDestructiveMigration().build()

        TutoShowcase.from(this)
            .setContentView(R.layout.tuto_gif_frame_select)
            .on(frameRecyclerView)
            .addRoundRect()
            .withBorder()
            .showOnce("tuto_gif_frame_select");
    }

    private fun switchFrame(i: Int) {
        if (::frame.isInitialized) {
            Log.d("hello2", frame.toString())
            for (view in frame.views) {
                view.visibility = View.INVISIBLE
            }
        }

        frame = frames[i]
        editorView.source?.setImageBitmap(frame.bitmap)

        for (view in frame.views) {
            view.visibility = View.VISIBLE
        }
    }

    private val frameListener = object : FrameListener {
        override fun onViewAdded(view: View) {
            if (frameAdapter.isSelectMode) {
                for (position in frameAdapter.selectedPositions) {
                    frames[position].views.add(view)
                }
            } else {
                frame.views.add(view)
            }
        }

        override fun onRemoveView(view: View?) {
            var i = 0
            for (v in frame.views) {
                if (v == view) {
                    break
                }
                i++
            }
            ignore {
                frame.views.removeAt(i)
            }
        }

        override fun onFrameSelected(position: Int) {
            switchFrame(position)
        }
    }

    private fun addEditorListener(editor: PhotoEditor) {
        editor.setOnPhotoEditorListener(object : OnPhotoEditorListener {
            override fun onRemoveViewListener(viewType: ViewType?, numberOfAddedViews: Int, view: View?) {
                updateToolbar(editor.currentView.viewType)
                frameListener.onRemoveView(view)
            }
            override fun onViewSelected(currentView: CurrentView) {
                updateToolbar(currentView.viewType)
            }
            override fun onAddViewListener(viewType: ViewType?, numberOfAddedViews: Int, view: View?) {
                updateToolbar(viewType)
                view?.also { frameListener.onViewAdded(view) }
            }
        })
    }

    private fun updateToolbar(viewType: ViewType?) {
        when (viewType) {
            ViewType.TEXT -> {
                tools.clear()
                textTools.forEach { tools.add(it) }
                toolAdapter.notifyDataSetChanged()
            }
            ViewType.IMAGE -> {
                tools.clear()
                imageTools.forEach { tools.add(it) }
                toolAdapter.notifyDataSetChanged()
            }
            ViewType.EMOJI -> {
                tools.clear()
                emojiTools.forEach { tools.add(it) }
                toolAdapter.notifyDataSetChanged()
            }
            else -> {
                tools.clear()
                defaultTools.forEach { tools.add(it) }
                toolAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun createAddItemDialog(): AlertDialog {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setCancelable(true)
        val viewGroup: ViewGroup? = null
        val view = LayoutInflater.from(this).inflate(R.layout.layout_add_item, viewGroup, false)
        builder.setView(view)
        val dialog = builder.create()

        val addImageView: LinearLayout = view.findViewById(R.id.addImageView)
        val addEmojiView: LinearLayout = view.findViewById(R.id.addEmojiView)
        val addTextView: LinearLayout = view.findViewById(R.id.addTextView)

        addImageView.setOnClickListener {
            dialog.dismiss()
            fileChooser.open()
        }

        addEmojiView.setOnClickListener {
            toast(getString(R.string.opening_emojies))
            thread {
                val sheet = EmojiesBottomSheetFragment.newInstance()
                runOnUiThread {
                    sheet.listener = object : EmojiesBottomSheetFragment.Listener {
                        override fun onAdd(emoji: String) {
                            editor.addEmoji(emoji)
                            sheet.dismiss()
                        }
                    }
                    sheet.show(supportFragmentManager, TAG_BOTTOM_SHEET)
                    dialog.dismiss()
                }
            }
        }

        addTextView.setOnClickListener {
            val textStyleBuilder = TextStyleBuilder()
                .withTextSize(18f) // always add the text size or you will get null pointer exception in ToolAdapter
                .withBackgroundColor(Color.TRANSPARENT)
                .withTextColor(Color.WHITE)
                .withTextOutline(Color.BLACK, 2)
                .withTextFont(fontFace.generateFont("Calistoga")!!.typeface)
                .withOpacity(1f)
            editor.addText(getString(R.string.your_text), textStyleBuilder)
            (editor.currentView.view as TextView).textAlignment = View.TEXT_ALIGNMENT_CENTER
            dialog.dismiss()
        }

        return dialog
    }

    private fun generateGif(): ByteArray {
        runOnUiThread {
            editor.clearHelperBox()
        }
        val bos = ByteArrayOutputStream()
        val encoder = GifEncoder()
        encoder.setRepeat(0)
        encoder.start(bos)
        for (i in frames.indices) {
            runOnUiThread {
                switchFrame(i)
            }
            Thread.sleep(100)
            val bitmap = editorViewHolder.getBitmap()
            val nh = ( bitmap.height * (500.0 / bitmap.width) ).toInt()
            val bmp = Bitmap.createScaledBitmap(bitmap, 500, nh, true)
            encoder.addFrame(bmp)
        }
        encoder.finish()
        return bos.toByteArray()
    }

    private fun saveGif(gif: ByteArray) {
        val fos: OutputStream?
        val fileName = "meme_${UUID.randomUUID()}.gif"
        val fileUri: String = fileIO.saveBytes("gifs", fileName, gif)

        thread {
            database.memeDao().putSavedMeme(SavedMeme(
                fileUri,
                "gif"
            ))
        }

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

    // override methods

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)
        this.menu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menuUndo -> editor.undo()
            R.id.menuRedo -> editor.redo()
            R.id.menuCamera -> {
                PermissionHandler.requestCamera(this, getString(R.string.camera_permission_message)) {
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, PermissionHandler.RESULT_CAMERA_CODE);
                    toast(getString(R.string.opening_the_camera))
                }
            }
            R.id.menuAddItem -> {
                addItemDialog.show()
            }
            R.id.menuSave -> {
                if (frameAdapter.isSelectMode) {
                    frameAdapter.selectAll()
                } else {
                    PermissionHandler.requestStorage(this, getString(R.string.storage_permission_message)) {
                        toast(getString(R.string.watch_a_vieo_while_we_generate), isLong = true)
                        Ads.showReward()
                        Ads.loadReward()
                        val dialog = showLoading(getString(R.string.generating_meme))
                        thread {
                            try {
                                val gif = generateGif()
                                saveGif(gif)
                                runOnUiThread {
                                    toast(getString(R.string.your_meme_saved_to_dcim))
                                }
                            } catch (e: Exception) {
                                Log.e("hello2", e.toString())
                                runOnUiThread {
                                    toast(getString(R.string.unable_to_save_meme))
                                }
                            }
                            runOnUiThread {
                                dialog.dismiss()
                            }
                        }
                    }
                }
            }
            R.id.menuDuplicate -> {
                if (editor.currentView.view == null) {
                    return false
                }

                when (editor.currentView.viewType) {
                    ViewType.IMAGE -> {
                        val imageView: ImageView = editor.currentView.view as ImageView
                        val bitmap = imageView.drawable.toBitmap()
                        editor.addImage(bitmap)

                        editor.currentView.view?.apply {
                            alpha = imageView.alpha
                            requestLayout()
                            layoutParams.height = imageView.height
                        }
                    }
                    ViewType.EMOJI -> {
                        val emojiTextView: TextView = editor.currentView.view as TextView
                        editor.addEmoji(emojiTextView.text.toString())
                        (editor.currentView.view as? TextView)?.apply {
                            alpha = emojiTextView.alpha
                            scaleX = emojiTextView.scaleX
                            scaleY = emojiTextView.scaleY
                        }
                    }
                    ViewType.TEXT -> {
                        val textView: TextView = editor.currentView.view as TextView
                        editor.addText(textView.text.toString(), editor.currentView.textStyle)
                        (editor.currentView.view as TextView).textAlignment = View.TEXT_ALIGNMENT_CENTER
                    }
                    else -> {
                        toast(getString(R.string.select_a_view_to_duplicate))
                    }
                }
            }
            R.id.menuShare -> {
                PermissionHandler.requestStorage(this, getString(R.string.storage_permission_message)) {
                    val dialog = showLoading(getString(R.string.generating_meme))
                    thread {
                        try {
                            val gif = generateGif()
                            runOnUiThread {
                                val fileDir = getExternalFilesDir("__share__")
                                assert(fileDir != null)
                                if (!fileDir!!.exists()) {
                                    fileDir.mkdirs()
                                }

                                val file = File(fileDir, "share.gif")

                                val fOut = FileOutputStream(file)
                                fOut.write(gif)
                                fOut.flush()
                                fOut.close()

                                dialog.dismiss()

                                val imageUri = FileProvider.getUriForFile(this@GifEditorActivity,
                                    applicationContext.packageName + ".provider", file)
                                val sharingIntent = Intent(Intent.ACTION_SEND)
                                sharingIntent.type = "image/gif"
                                sharingIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                sharingIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                                sharingIntent.putExtra(Intent.EXTRA_STREAM, imageUri)
                                startActivity(Intent.createChooser(sharingIntent, "Share via"))
                            }
                        } catch (e: Exception) {
                            runOnUiThread {
                                toast(getString(R.string.unable_to_save_meme))
                            }
                        }
                        runOnUiThread {
                            dialog.dismiss()
                        }
                    }
                }
            }
        }

        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            if (requestCode == FileChooser.RESULT_CODE_NEW) {
                data?.data?.also {
                    val bytes = fileChooser.read(it)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    editor.addImage(bitmap)
                }
            } else if (requestCode == FileChooser.RESULT_CODE_UPDATE) {
                data?.data?.also {
                    val bytes = fileChooser.read(it)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    (editor.currentView.view as ImageView).setImageBitmap(bitmap)
                }
            } else if (requestCode == PermissionHandler.RESULT_CAMERA_CODE) {
                val photo = data?.extras?.get("data") as? Bitmap
                if (photo != null) {
                    editor.addImage(photo)
                }
            } else if (requestCode == FileChooser.RESULT_CODE_UPDATE_EDITOR_IMAGE) {
                data?.data?.also {
                    val bytes = fileChooser.read(it)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    editorView.source?.setImageBitmap(bitmap)
                }
            }
        }
        return super.onActivityResult(requestCode, resultCode, data)
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
