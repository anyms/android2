package app.spidy.freeproxylist.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import app.spidy.freeproxylist.R
import app.spidy.freeproxylist.data.Country
import java.util.*

class CountryAdapter(
    private val ctx: Context,
    private val countries: List<Country>
): ArrayAdapter<Country>(ctx, 0, countries) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return initView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return initView(position, convertView, parent)
    }

    private fun initView(position: Int, convertView: View?, parent: ViewGroup): View {
        var conView = convertView
        if (conView == null) {
            conView = LayoutInflater.from(context).inflate(R.layout.spinner_country_item, parent, false)
        }

        val flagImageView: ImageView = conView!!.findViewById(R.id.flagImage)
        val countryNameView: TextView = conView.findViewById(R.id.countryName)
        val country = getItem(position)

        country?.also {
            val id = context.resources.getIdentifier(
                "flag_${it.countryCode.toLowerCase(Locale.getDefault())}",
                "drawable", context.packageName)
            if (id == 0) {
                flagImageView.setImageResource(R.drawable.globe_icon)
            } else {
                flagImageView.setImageResource(id)
            }
            countryNameView.text = it.countryName
        }

        return conView
    }
}