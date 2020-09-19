package app.spidy.tamilmemecreator.utils

import android.content.Context
import android.graphics.Typeface
import app.spidy.tamilmemecreator.data.Font

class FontFace(private val context: Context) {
    private val fonts = arrayListOf(
        hashMapOf("name" to "Be Vietnam", "typeface" to "BeVietnam_Regular.ttf"),
        hashMapOf("name" to "Grundschrift", "typeface" to "Grundschrift_Regular.otf"),
        hashMapOf("name" to "Gavabon", "typeface" to "Gavabon.otf"),
        hashMapOf("name" to "Calistoga", "typeface" to "Calistoga.ttf"),
        hashMapOf("name" to "Gupter", "typeface" to "Gupter_Regular.ttf"),
        hashMapOf("name" to "League Gothic", "typeface" to "LeagueGothic_Regular.otf"),
        hashMapOf("name" to "Memes Font", "typeface" to "Memes.ttf"),
        hashMapOf("name" to "Road Way", "typeface" to "RoadWay.ttf"),
        hashMapOf("name" to "Gnuolane", "typeface" to "Gnuolane.ttf"),
        hashMapOf("name" to "Arima Madurai", "typeface" to "ArimaMadurai.ttf"),
        hashMapOf("name" to "Baloo Thambi", "typeface" to "BalooThambi.ttf"),
        hashMapOf("name" to "Coiny", "typeface" to "Coiny.ttf")
    )

    private val tamilFonts = arrayListOf(
        hashMapOf("name" to "Arima Madurai", "typeface" to "ArimaMadurai.ttf"),
        hashMapOf("name" to "Baloo Thambi", "typeface" to "BalooThambi.ttf"),
        hashMapOf("name" to "Coiny", "typeface" to "Coiny.ttf")
    )

    fun getFontNames(): List<String> {
        val fs = ArrayList<String>()
        for (font in fonts) {
            fs.add(font["name"]!!)
        }
        return fs
    }

    fun generateFont(name: String): Font? {
        var font: HashMap<String, String>? = null
        for (f in fonts) {
            if (f["name"] == name) {
                font = f
                break
            }
        }
        if (font == null) return null
        return Font(
            name = font["name"]!!,
            typeface = Typeface.createFromAsset(context.assets, font["typeface"])
        )
    }

    fun getTamilFontNames(): List<String> {
        val fs = ArrayList<String>()
        for (font in tamilFonts) {
            fs.add(font["name"]!!)
        }
        return fs
    }

    fun generateTamilFont(name: String): Font? {
        var font: HashMap<String, String>? = null
        for (f in tamilFonts) {
            if (f["name"] == name) {
                font = f
                break
            }
        }
        if (font == null) return null
        return Font(
            name = font["name"]!!,
            typeface = Typeface.createFromAsset(context.assets, font["typeface"])
        )
    }
}