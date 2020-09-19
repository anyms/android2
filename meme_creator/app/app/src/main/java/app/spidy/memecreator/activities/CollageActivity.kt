package app.spidy.memecreator.activities

import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.*
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.spidy.kotlinutils.toast
import app.spidy.memecreator.R
import app.spidy.memecreator.adapters.ToolAdapter
import app.spidy.memecreator.data.Tool
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

class CollageActivity : AppCompatActivity() {
    private lateinit var currentEditor: PhotoEditor
    private lateinit var currentEditorView: PhotoEditorView
    private lateinit var currentViewHolder: FrameLayout
    private lateinit var fileChooser: FileChooser
    private lateinit var parentView: LinearLayout
    private lateinit var toolRecyclerView: RecyclerView
    private lateinit var toolAdapter: ToolAdapter
    private lateinit var toolbar: Toolbar
    private lateinit var addItemDialog: Dialog
    private lateinit var fontFace: FontFace

    private var lastSelectedView: CurrentView? = null

    private val editors = ArrayList<PhotoEditor>()
    private val editorViews  = ArrayList<PhotoEditorView>()
    private val editorViewHolders = ArrayList<FrameLayout>()

    private lateinit var storageReference: StorageReference
    private lateinit var firestore: FirebaseFirestore
    private lateinit var tinyDB: TinyDB

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

        val layout = intent.getIntExtra("layout", 1)
        val id = resources.getIdentifier("layout_collage${layout}", "layout", packageName)
        setContentView(id)

        when(layout) {
            1 -> for (i in 1..2) { initViews(i) }
            2, 3, 4 -> for (i in 1..3) { initViews(i) }
            5 -> for (i in 1..4) { initViews(i) }
        }

        Ads.showInterstitial()
        Ads.loadInterstitial()

        findViewById<AdView>(R.id.adView).loadAd(AdRequest.Builder().build())

        parentView = findViewById(R.id.parentView)

        fileChooser = FileChooser(this)
        toolRecyclerView = findViewById(R.id.toolRecyclerView)
        toolbar = findViewById(R.id.toolbar)

        setSupportActionBar(toolbar)
        title = ""
        addItemDialog = createAddItemDialog()
        fontFace = FontFace(this)

        toolbar.setNavigationIcon(R.drawable.ic_back_arrow)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        setCurrentEditor(1)

        currentEditor.addImage(getDrawable(R.drawable.logo)!!.toBitmap())
        currentEditor.currentView.view?.apply {
            alpha = 0.3f
            requestLayout()
            layoutParams.height = 60
        }

        editorViews.forEachIndexed { index, editorView ->
            editorView.source?.setImageDrawable(getDrawable(R.drawable.placeholder))
            editorView.setOnClickListener {
                setCurrentEditor(index+1)
            }
        }

        storageReference = FirebaseStorage.getInstance().getReference("showcase")
        firestore = FirebaseFirestore.getInstance()
        tinyDB = TinyDB(this)
    }

    private fun initViews(i: Int) {
        val editorViewId = resources.getIdentifier("editorView${i}", "id", packageName)
        val editorViewHolderId = resources.getIdentifier("editorView${i}Holder", "id", packageName)
        val editorView: PhotoEditorView = findViewById(editorViewId)
        val editorViewHolder: FrameLayout = findViewById(editorViewHolderId)
        val editor = PhotoEditor.Builder(this, editorView).build()
        editorView.source?.setImageDrawable(getDrawable(R.drawable.placeholder))
        editorViews.add(editorView)
        editors.add(editor)
        editorViewHolders.add(editorViewHolder)
    }

    private fun setCurrentEditor(num: Int) {
        currentEditor = editors[num-1]
        currentEditorView = editorViews[num-1]
        currentViewHolder = editorViewHolders[num-1]
        for (editorViewHolder in editorViewHolders) {
            editorViewHolder.setBackgroundResource(0)
        }
        editorViewHolders[num-1].setBackgroundResource(R.drawable.border_accent)

        toolAdapter = ToolAdapter(this, tools, currentEditor, parentView, fileChooser, currentEditorView) {
            return@ToolAdapter lastSelectedView
        }
        toolRecyclerView.adapter = toolAdapter
        toolRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        addEditorListener(currentEditor)
    }

    private fun addEditorListener(editor: PhotoEditor) {
        editor.setOnPhotoEditorListener(object : OnPhotoEditorListener {
            override fun onRemoveViewListener(viewType: ViewType?, numberOfAddedViews: Int, view: View?) {
                updateToolbar(currentEditor.currentView.viewType)
            }
            override fun onViewSelected(currentView: CurrentView) {
                updateToolbar(currentView.viewType)
            }
            override fun onAddViewListener(viewType: ViewType?, numberOfAddedViews: Int, view: View?) {
                lastSelectedView = currentEditor.currentView
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
                            currentEditor.addEmoji(emoji)
                            sheet.dismiss()
                        }
                    }
                    sheet.show(supportFragmentManager,
                        EditorActivity.TAG_BOTTOM_SHEET
                    )
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
            currentEditor.addText(getString(R.string.your_text), textStyleBuilder)
            (currentEditor.currentView.view as TextView).textAlignment = View.TEXT_ALIGNMENT_CENTER
            dialog.dismiss()
        }

        return dialog
    }

    private fun clearHelperBox() {
        for (editor in editors) {
            editor.clearHelperBox()
        }
        for (editorViewHolder in editorViewHolders) {
            editorViewHolder.setBackgroundResource(0)
        }
    }

    private fun createMeme(listener: EditorListener) {
        clearHelperBox()
        thread {
            try {
                Thread.sleep(1000)
                val bitmap = parentView.getBitmap()
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = contentResolver;
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "meme_${UUID.randomUUID()}")
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
            val image = File(imagesDir, "meme_${UUID.randomUUID()}.png");
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
            R.id.menuUndo -> currentEditor.undo()
            R.id.menuRedo -> currentEditor.redo()
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
                PermissionHandler.requestStorage(this, getString(R.string.storage_permission_message)) {
                    toast(getString(R.string.watch_a_vieo_while_we_generate), isLong = true)
                    Ads.showReward()
                    Ads.loadReward()
                    val dialog = showLoading("Generating your meme...")
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
                when (lastSelectedView?.viewType) {
                    ViewType.IMAGE -> {
                        val imageView: ImageView = lastSelectedView!!.view as ImageView
                        val bitmap = imageView.drawable.toBitmap()
                        currentEditor.addImage(bitmap)
                        lastSelectedView?.view?.apply {
                            alpha = imageView.alpha
                            requestLayout()
                            layoutParams.height = imageView.height
                        }
                    }
                    ViewType.EMOJI -> {
                        val emojiTextView: TextView = lastSelectedView!!.view as TextView
                        currentEditor.addEmoji(emojiTextView.text.toString())
                        (lastSelectedView?.view as? TextView)?.apply {
                            alpha = emojiTextView.alpha
                            scaleX = emojiTextView.scaleX
                            scaleY = emojiTextView.scaleY
                        }
                    }
                    ViewType.TEXT -> {
                        val textView: TextView = lastSelectedView!!.view as TextView
                        currentEditor.addText(textView.text.toString(), lastSelectedView?.textStyle)
                        (currentEditor.currentView.view as TextView).textAlignment = View.TEXT_ALIGNMENT_CENTER
                    }
                    else -> {
                        toast(getString(R.string.select_a_view_to_duplicate))
                    }
                }
            }
            R.id.menuShare -> {
                PermissionHandler.requestStorage(this, getString(R.string.storage_permission_message)) {
                    val dialog = showLoading(getString(R.string.generating_meme))
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

                            val imageUri = FileProvider.getUriForFile(this@CollageActivity,
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
                    currentEditor.addImage(bitmap)
                }
            } else if (requestCode == FileChooser.RESULT_CODE_UPDATE) {
                data?.data?.also {
                    val bytes = fileChooser.read(it)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    val imageView = (currentEditor.currentView.view as? ImageView)

                    if (imageView == null) {
                        toast(getString(R.string.view_not_inside_canvas))
                    } else {
                        imageView.setImageBitmap(bitmap)
                    }
                }
            } else if (requestCode == PermissionHandler.RESULT_CAMERA_CODE) {
                val photo = data?.extras?.get("data") as? Bitmap
                if (photo != null) {
                    currentEditor.addImage(photo)
                }
            } else if (requestCode == FileChooser.RESULT_CODE_UPDATE_EDITOR_IMAGE) {
                data?.data?.also {
                    val bytes = fileChooser.read(it)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    currentEditorView.source?.setImageBitmap(bitmap)
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
