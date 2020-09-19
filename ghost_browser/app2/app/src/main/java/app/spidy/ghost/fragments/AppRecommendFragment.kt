package app.spidy.ghost.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

import app.spidy.ghost.R

//

class AppRecommendFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_app_recommend, container, false)
        val getItBtn: Button = v.findViewById(R.id.getItBtn)

        getItBtn.setOnClickListener {
            val appId = "app.spidy.ghostvpn"
            try {
                this.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appId")))
            } catch (anfe: ActivityNotFoundException) {
                this.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=$appId")
                    )
                )
            }
        }
        return v
    }
}
