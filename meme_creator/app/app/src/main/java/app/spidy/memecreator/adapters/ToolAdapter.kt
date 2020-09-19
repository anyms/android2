package app.spidy.memecreator.adapters

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import app.spidy.kotlinutils.toast
import app.spidy.memecreator.R
import app.spidy.memecreator.activities.EditorActivity
import app.spidy.memecreator.data.Tool
import app.spidy.memecreator.fragments.*
import app.spidy.memecreator.utils.FileChooser
import app.spidy.photoeditor2.CurrentView
import app.spidy.photoeditor2.core.PhotoEditor
import app.spidy.photoeditor2.core.PhotoEditorView
import app.spidy.photoeditor2.core.TextStyleBuilder
import yuku.ambilwarna.AmbilWarnaDialog

class ToolAdapter(
    private val context: Context,
    private val tools: List<Tool>,
    private val editor: PhotoEditor,
    private val editorViewHolder: LinearLayout,
    private val fileChooser: FileChooser,
    private val editorView: PhotoEditorView,
    private val getLastView: (() -> CurrentView?)? = null
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var usedTool: MainHolder? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.layout_tool_item, parent, false)
        return MainHolder(v)
    }

    override fun getItemCount(): Int {
        return tools.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mainHolder = holder as MainHolder

        mainHolder.toolImageView.setImageDrawable(ContextCompat.getDrawable(context, tools[position].drawable))
        mainHolder.toolTitleView.text = tools[position].title

        if (editor.isRotationEnabled && tools[position].tag == Tool.IMAGE_ROTATE) {
            activate(mainHolder)
        } else {
            deactivate(mainHolder)
        }

        mainHolder.toolRootView.setOnClickListener {
            when (tools[position].tag) {
                Tool.EDITOR_BACKGROUND_COLOR -> {
                    var color = Color.TRANSPARENT
                    val background = editorViewHolder.background
                    if (background is ColorDrawable) {
                        color = background.color
                    }
                    val colorPicker = AmbilWarnaDialog(context, color, object : AmbilWarnaDialog.OnAmbilWarnaListener {
                        override fun onCancel(dialog: AmbilWarnaDialog?) {}
                        override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                            editorViewHolder.setBackgroundColor(color)
                        }
                    })
                    colorPicker.show()
                }
                Tool.EDITOR_IMAGE -> {
                    fileChooser.open(FileChooser.RESULT_CODE_UPDATE_EDITOR_IMAGE)
                }

                Tool.TEXT_EDIT -> {
                    if (editor.currentView.view == null) {
                        context.toast(context.getString(R.string.view_not_inside_canvas))
                    } else {
                        val bundle = Bundle()
                        bundle.putString(
                            "text",
                            (editor.currentView.view as TextView).text.toString()
                        )
                        val sheet = EditTextBottomSheetFragment.newInstance()
                        sheet.arguments = bundle
                        sheet.listener = object : EditTextBottomSheetFragment.Listener {
                            override fun onApply(text: String) {
                                editor.editText(
                                    editor.currentView.rootView!!,
                                    text,
                                    editor.currentView.textStyle
                                )
                            }
                        }
                        sheet.show(
                            (context as AppCompatActivity).supportFragmentManager,
                            EditorActivity.TAG_BOTTOM_SHEET
                        )
                    }
                }
                Tool.TEXT_COLOR -> {
                    if (editor.currentView.textStyle == null) {
                        context.toast(context.getString(R.string.view_not_inside_canvas))
                    } else {
                        val bundle = Bundle()
                        bundle.putInt(
                            "fgColor",
                            (editor.currentView.view as TextView).currentTextColor
                        )
                        bundle.putInt(
                            "bgColor",
                            (editor.currentView.view!!.background as ColorDrawable).color
                        )
                        val sheet = TextColorBottomSheetFragment.newInstance()
                        sheet.arguments = bundle
                        sheet.listener = object : TextColorBottomSheetFragment.Listener {
                            override fun onApply(fgColor: Int, bgColor: Int) {
                                editor.currentView.textStyle?.withTextColor(fgColor)
                                    ?.withBackgroundColor(bgColor)
                                editor.editText(
                                    editor.currentView.rootView!!,
                                    (editor.currentView.view as TextView).text.toString(),
                                    editor.currentView.textStyle
                                )
                            }
                        }
                        sheet.show(
                            (context as AppCompatActivity).supportFragmentManager,
                            EditorActivity.TAG_BOTTOM_SHEET
                        )
                    }
                }
                Tool.TEXT_SIZE -> {
                    if (editor.currentView.textStyle == null) {
                        context.toast(context.getString(R.string.view_not_inside_canvas))
                    } else {
                        val bundle = Bundle()
                        bundle.putInt(
                            "text_size",
                            editor.currentView.textStyle?.values?.get(TextStyleBuilder.TextStyle.SIZE)
                                ?.toString()!!.toFloat().toInt()
                        )
                        val sheet = TextSizeBottomSheetFragment.newInstance()
                        sheet.arguments = bundle
                        sheet.listener = object : TextSizeBottomSheetFragment.Listener {
                            override fun onApply(textSize: Int) {
                                editor.currentView.textStyle?.withTextSize(textSize.toFloat())
                                editor.editText(
                                    editor.currentView.rootView!!,
                                    (editor.currentView.view as TextView).text.toString(),
                                    editor.currentView.textStyle
                                )
                            }
                        }
                        sheet.show(
                            (context as AppCompatActivity).supportFragmentManager,
                            EditorActivity.TAG_BOTTOM_SHEET
                        )
                    }
                }
                Tool.TEXT_FONT -> {
                    if (editor.currentView.textStyle == null) {
                        context.toast(context.getString(R.string.view_not_inside_canvas))
                    } else {
                        val sheet = TextFontBottomSheetFragment.newInstance()
                        sheet.listener = object : TextFontBottomSheetFragment.Listener {
                            override fun onApply(typeface: Typeface) {
                                editor.currentView.textStyle?.withTextFont(typeface)
                                editor.editText(
                                    editor.currentView.rootView!!,
                                    (editor.currentView.view as TextView).text.toString(),
                                    editor.currentView.textStyle
                                )
                            }
                        }
                        sheet.show(
                            (context as AppCompatActivity).supportFragmentManager,
                            EditorActivity.TAG_BOTTOM_SHEET
                        )
                    }
                }
                Tool.TEXT_OUTLINE -> {
                    if (editor.currentView.textStyle == null) {
                        context.toast(context.getString(R.string.view_not_inside_canvas))
                    } else {
                        val bundle = Bundle()
                        val vals = editor.currentView.textStyle?.values!![TextStyleBuilder.TextStyle.OUTLINE_COLOR] as List<*>
                        bundle.putInt("outline_color", vals[0] as Int)
                        bundle.putInt("outline_size", vals[1] as Int)
                        val sheet = TextOutlineBottomSheetFragment.newInstance()
                        sheet.arguments = bundle
                        sheet.listener = object : TextOutlineBottomSheetFragment.Listener {
                            override fun onApply(outlineColor: Int, outlineSize: Int) {
                                editor.currentView.textStyle?.withTextOutline(
                                    outlineColor,
                                    outlineSize
                                )
                                editor.editText(
                                    editor.currentView.rootView!!,
                                    (editor.currentView.view as TextView).text.toString(),
                                    editor.currentView.textStyle
                                )
                            }
                        }
                        sheet.show(
                            (context as AppCompatActivity).supportFragmentManager,
                            EditorActivity.TAG_BOTTOM_SHEET
                        )
                    }
                }
                Tool.TEXT_OPACITY -> {
                    if (editor.currentView.textStyle == null) {
                        context.toast(context.getString(R.string.view_not_inside_canvas))
                    } else {
                        val bundle = Bundle()
                        val opacity =
                            editor.currentView.textStyle?.values!![TextStyleBuilder.TextStyle.OPACITY] as Float
                        bundle.putFloat("opacity", opacity)
                        val sheet = TextOpacityBottomSheetFragment.newInstance()
                        sheet.arguments = bundle
                        sheet.listener = object : TextOpacityBottomSheetFragment.Listener {
                            override fun onApply(textOpacity: Int) {
                                editor.currentView.textStyle?.withOpacity(textOpacity / 10f)
                                editor.editText(
                                    editor.currentView.rootView!!,
                                    (editor.currentView.view as TextView).text.toString(),
                                    editor.currentView.textStyle
                                )
                            }
                        }
                        sheet.show(
                            (context as AppCompatActivity).supportFragmentManager,
                            EditorActivity.TAG_BOTTOM_SHEET
                        )
                    }
                }

                Tool.IMAGE_CHANGE -> {
                    fileChooser.open(FileChooser.RESULT_CODE_UPDATE)
                }
                Tool.IMAGE_OPACITY -> {
                    if (editor.currentView.view == null) {
                        context.toast(context.getString(R.string.view_not_inside_canvas))
                    } else {
                        val bundle = Bundle()
                        val view = editor.currentView.view as ImageView
                        bundle.putFloat("opacity", view.alpha)
                        val sheet = TextOpacityBottomSheetFragment.newInstance()
                        sheet.arguments = bundle
                        sheet.listener = object : TextOpacityBottomSheetFragment.Listener {
                            override fun onApply(textOpacity: Int) {
                                view.alpha = textOpacity / 10f
                            }
                        }
                        sheet.show(
                            (context as AppCompatActivity).supportFragmentManager,
                            EditorActivity.TAG_BOTTOM_SHEET
                        )
                    }
                }
                Tool.IMAGE_ROTATE -> {
                    if (editor.isRotationEnabled) {
                        editor.isRotationEnabled = false
                        deactivate(mainHolder)
                    } else {
                        editor.isRotationEnabled = true
                        activate(mainHolder)
                    }
                }

                Tool.EMOJI_OPACITY -> {
                    if (editor.currentView.view == null) {
                        context.toast(context.getString(R.string.view_not_inside_canvas))
                    } else {
                        val bundle = Bundle()
                        val view = editor.currentView.view as TextView
                        bundle.putFloat("opacity", view.alpha)
                        val sheet = TextOpacityBottomSheetFragment.newInstance()
                        sheet.arguments = bundle
                        sheet.listener = object : TextOpacityBottomSheetFragment.Listener {
                            override fun onApply(textOpacity: Int) {
                                view.alpha = textOpacity / 10f
                            }
                        }
                        sheet.show(
                            (context as AppCompatActivity).supportFragmentManager,
                            EditorActivity.TAG_BOTTOM_SHEET
                        )
                    }
                }
            }
        }
    }

    private fun activate(mainHolder: MainHolder) {
        val color = ContextCompat.getColor(context, R.color.colorAccent)
        mainHolder.toolTitleView.setTextColor(color)
        mainHolder.toolImageView.setColorFilter(color)
        usedTool = mainHolder
    }

    private fun deactivate(mainHolder: MainHolder) {
        val color = ContextCompat.getColor(context, R.color.colorSemiWhite)
        mainHolder.toolTitleView.setTextColor(color)
        mainHolder.toolImageView.setColorFilter(color)
    }


    inner class MainHolder(v: View): RecyclerView.ViewHolder(v) {
        val toolRootView: LinearLayout = v.findViewById(R.id.toolRootView)
        val toolImageView: ImageView = v.findViewById(R.id.toolImageView)
        val toolTitleView: TextView = v.findViewById(R.id.toolTitleView)
    }
}