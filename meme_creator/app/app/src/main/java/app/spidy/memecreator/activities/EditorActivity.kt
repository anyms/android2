package app.spidy.memecreator.activities

import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import app.spidy.kotlinutils.toast
import app.spidy.memecreator.R
import app.spidy.memecreator.adapters.ToolAdapter
import app.spidy.memecreator.data.SavedMeme
import app.spidy.memecreator.data.Tool
import app.spidy.memecreator.databases.MemeDatabase
import app.spidy.memecreator.fragments.EmojiesBottomSheetFragment
import app.spidy.memecreator.interfaces.EditorListener
import app.spidy.memecreator.utils.*
import app.spidy.photoeditor2.CurrentView
import app.spidy.photoeditor2.core.*
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


class EditorActivity : AppCompatActivity() {
    companion object {
        const val TAG_BOTTOM_SHEET = "app.spidy.mime.TAG_BOTTOM_SHEET"
    }

    private lateinit var editor: PhotoEditor
    private lateinit var fileChooser: FileChooser
    private lateinit var editorView: PhotoEditorView
    private lateinit var toolRecyclerView: RecyclerView
    private lateinit var toolAdapter: ToolAdapter
    private lateinit var toolbar: Toolbar
    private lateinit var addItemDialog: Dialog
    private lateinit var fontFace: FontFace
    private lateinit var editorViewHolder: LinearLayout
    private lateinit var database: MemeDatabase

    private lateinit var tinyDB: TinyDB
    private lateinit var fileIO: FileIO

    private val tools = ArrayList<Tool>()

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
        setContentView(R.layout.activity_editor)

        Ads.showInterstitial()
        Ads.loadInterstitial()

        findViewById<AdView>(R.id.adView).loadAd(AdRequest.Builder().build())

        defaultTools.forEach { tools.add(it) }

        val isLocal = intent.getBooleanExtra("isLocal", false)

        editorView = findViewById(R.id.editorView)
        editor = PhotoEditor.Builder(this, editorView).build()
        fileChooser = FileChooser(this)
        toolRecyclerView = findViewById(R.id.toolRecyclerView)
        toolbar = findViewById(R.id.toolbar)
        editorViewHolder = findViewById(R.id.editorViewHolder)

        toolAdapter = ToolAdapter(this, tools, editor, editorViewHolder, fileChooser, editorView)
        toolRecyclerView.adapter = toolAdapter
        toolRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // resize the layout
//        val bitmap = getDrawable(R.drawable.plain_template)!!.toBitmap()
//        editorView.source?.setImageBitmap(Bitmap.createScaledBitmap(bitmap, 1024, 720, false))

        editorView.source?.setImageDrawable(getDrawable(R.drawable.placeholder))

        addEditorListener(editor)
        setSupportActionBar(toolbar)
        title = ""
        addItemDialog = createAddItemDialog()
        fontFace = FontFace(this)

        toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        editor.addImage(getDrawable(R.drawable.logo)!!.toBitmap())
        editor.currentView.view?.apply {
            alpha = 0.3f
            requestLayout()
            layoutParams.height = 60
        }

        tinyDB = TinyDB(this)
        fileIO = FileIO(this)

        if (isLocal) {
            val bytes = fileIO.readBytes("__data__", "meme.dat")
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            editorView.source?.setImageBitmap(bitmap)
        }

        database = Room.databaseBuilder(this, MemeDatabase::class.java, "MemeDatabase")
            .fallbackToDestructiveMigration().build()
    }

    private fun addEditorListener(editor: PhotoEditor) {
        editor.setOnPhotoEditorListener(object : OnPhotoEditorListener {
            override fun onRemoveViewListener(viewType: ViewType?, numberOfAddedViews: Int, view: View?) {
                updateToolbar(editor.currentView.viewType)
            }
            override fun onViewSelected(currentView: CurrentView) {
                updateToolbar(currentView.viewType)
            }
            override fun onAddViewListener(viewType: ViewType?, numberOfAddedViews: Int, view: View?) {
                updateToolbar(viewType)
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

    private fun createMeme(listener: EditorListener) {
        editor.clearHelperBox()
        thread {
            try {
                Thread.sleep(1000)
                val bitmap = editorViewHolder.getBitmap()
                runOnUiThread {
                    listener.onSuccess(bitmap)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    listener.onFail()
                }
            }
        }
    }


    private fun saveImage(bitmap: Bitmap): Boolean {
        val isSaved: Boolean
        val fos: OutputStream?
        val fileName = "meme_${UUID.randomUUID()}"
        val fileUri = fileIO.saveBitmap("memes", fileName, bitmap)

        thread {
            database.memeDao().putSavedMeme(SavedMeme(
                fileUri,
                "meme"
            ))
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = contentResolver;
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
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
            val image = File(imagesDir, "$fileName.png");
            fos = FileOutputStream(image)

//            sendBroadcast(Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"
//                    + Environment.getExternalStorageDirectory())))
        }
        isSaved = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        fos?.flush()
        fos?.close()
        return isSaved
    }


    // override methods

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_editor, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menuUndo -> editor.undo()
            R.id.menuRedo -> editor.redo()
            R.id.menuCamera -> {
                PermissionHandler.requestCamera(this, getString(R.string.camera_permission_message)) {
                    val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, PermissionHandler.RESULT_CAMERA_CODE);
                    toast(getString(R.string.opening_the_camera))
                }
            }
            R.id.menuAddItem -> {
                addItemDialog.show()
            }
            R.id.menuSave -> {
                PermissionHandler.requestStorage(this, getString(R.string.storage_permission_message)) {
                    val dialog = showLoading(getString(R.string.generating_meme))
                    toast(getString(R.string.watch_a_vieo_while_we_generate), isLong = true)
                    Ads.showReward()
                    Ads.loadReward()
                    createMeme(object : EditorListener {
                        override fun onSuccess(bitmap: Bitmap) {
                            saveImage(bitmap)
                            dialog.dismiss()
                            toast(getString(R.string.your_meme_saved_to_dcim))
                        }
                        override fun onFail() {
                            dialog.dismiss()
                            toast(getString(R.string.unable_to_save_meme))
                        }
                    })
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
                    val dialog = showLoading("Generating your meme...")
                    createMeme(object : EditorListener {
                        override fun onFail() {
                            dialog.dismiss()
                            toast(getString(R.string.unable_to_save_meme))
                        }
                        override fun onSuccess(bitmap: Bitmap) {
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

                            val imageUri = FileProvider.getUriForFile(this@EditorActivity,
                                applicationContext.packageName + ".provider", file)
                            val sharingIntent = Intent(Intent.ACTION_SEND)
                            sharingIntent.type = "image/*"
                            sharingIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            sharingIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            sharingIntent.putExtra(Intent.EXTRA_STREAM, imageUri)
                            startActivity(Intent.createChooser(sharingIntent, "Share via"))
                        }
                    });
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
