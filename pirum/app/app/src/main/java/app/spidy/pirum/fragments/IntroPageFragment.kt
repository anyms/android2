package app.spidy.pirum.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import app.spidy.pirum.R
import app.spidy.pirum.utils.setHtml


class IntroPageFragment : Fragment() {
    companion object {
        fun getInstance(pageNum: Int): IntroPageFragment {
            val page = IntroPageFragment()
            page.pageNum = pageNum
            return page
        }
    }

    var pageNum = 1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_intro_page, container, false)
        val messageView: TextView = v.findViewById(R.id.messageView)
        val titleView: TextView = v.findViewById(R.id.titleView)
        val imageView: ImageView = v.findViewById(R.id.imageView)

        when (pageNum) {
            1 -> {
                imageView.visibility = View.GONE
                titleView.text = getString(R.string.getting_started)
                messageView.setHtml("""
                    Welcome to <strong>Pirum</strong>. The idea is to connect your smartphone with your desktop browser, so we created an extension that can communicate with app.
                """.trimIndent())
            }
            2 -> {
                imageView.visibility = View.VISIBLE
                titleView.text = getString(R.string.installation)
                messageView.setHtml("""
                    Find the <strong>Pirum</strong> extension in your browser extension store. Just search for <strong>Pirum</strong> and add the extension <strong>Offered by: Spidy, Inc.</strong>
                """.trimIndent())
            }
            3 -> {
                imageView.visibility = View.VISIBLE
                imageView.setImageResource(R.drawable.img_extension_connect)
                titleView.text = getString(R.string.connect_your_smartphone)
                messageView.setHtml("""
                    Click the <strong>Connect</strong> button from your extension and enter the IP address of your smartphone, you can find the IP address in <strong>Pirum</strong> server notification. Only enter the IP address not the port number.
                """.trimIndent())
            }
        }

        return v
    }
}