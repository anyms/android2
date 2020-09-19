package app.spidy.freeproxylist.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import app.spidy.freeproxylist.R
import app.spidy.freeproxylist.data.Proxy
import app.spidy.freeproxylist.utils.Ads
import app.spidy.freeproxylist.utils.copyToClipboard
import app.spidy.kotlinutils.toast

class ProxyAdapter(
    private val context: Context,
    private val proxies: ArrayList<Proxy>
): RecyclerView.Adapter<ProxyAdapter.ViewHolder>() {

    private lateinit var ipAddressDialog: TextView
    private lateinit var anonymityDialog: TextView
    private lateinit var googlePassedDialog: TextView
    private lateinit var sslSupportDialog: TextView
    private lateinit var countryNameDialog: TextView
    private lateinit var countryCodeDialog: TextView
    private lateinit var portDialog: TextView

    private lateinit var proxyDialog: AlertDialog

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.proxy_item, parent, false)
        proxyDialog = createDialog()
        return ViewHolder(v)
    }

    override fun getItemCount(): Int {
        return proxies.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val proxy = proxies[position]
//        val density = context.resources.displayMetrics.density
//        val padding20 = (20 * density).toInt()
//        val padding10 = (10 * density).toInt()
//        if (proxy.isFirst && position == 0) {
//            val paddingTop = (85 * density).toInt()
//            holder.rootView.setPadding(padding20, paddingTop, padding20, padding10)
//        } else {
//            holder.rootView.setPadding(padding20, padding10, padding20, padding10)
//        }
        holder.ipAddress.text = proxy.ip
        holder.anonymity.text = proxy.anonymity
        holder.sslSupport.text = if (proxy.sslSupport) "Yes" else "No"
        holder.googlePassed.text = if (proxy.googlePassed) "Yes" else "No"

        holder.rootView.setOnClickListener {
            showDialog(proxy)
            Ads.showInterstitial()
            Ads.loadInterstitial()
        }
    }

    private fun showDialog(proxy: Proxy) {
        ipAddressDialog.text = proxy.ip
        anonymityDialog.text = proxy.anonymity
        googlePassedDialog.text = if (proxy.googlePassed) "Yes" else "No"
        sslSupportDialog.text = if (proxy.sslSupport) "Yes" else "No"
        countryNameDialog.text = proxy.countryName
        countryCodeDialog.text = proxy.countryCode
        portDialog.text = proxy.port

        proxyDialog.show()

        proxyDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            context.copyToClipboard(proxy.ip)
            context.toast("IP copied!")
            Ads.showInterstitial()
            Ads.loadInterstitial()
        }
        proxyDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
            context.copyToClipboard(proxy.port)
            context.toast("Port copied!")
            Ads.showInterstitial()
            Ads.loadInterstitial()
        }
        proxyDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
            proxyDialog.dismiss()
        }
    }

    private fun createDialog(): AlertDialog {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setCancelable(false)
        val viewGroup: ViewGroup? = null
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_proxy, viewGroup, false)

        ipAddressDialog = view.findViewById(R.id.ipAddress)
        anonymityDialog = view.findViewById(R.id.anonymity)
        googlePassedDialog = view.findViewById(R.id.googlePassed)
        sslSupportDialog = view.findViewById(R.id.sslSupport)
        countryNameDialog = view.findViewById(R.id.countryName)
        countryCodeDialog = view.findViewById(R.id.countryCode)
        portDialog = view.findViewById(R.id.port)

        builder.setView(view)
        builder.setPositiveButton("Copy IP", null)
        builder.setNegativeButton("Copy Port", null)
        builder.setNeutralButton("Close", null)
        return builder.create()
    }


    inner class ViewHolder(v: View): RecyclerView.ViewHolder(v) {
        val rootView: LinearLayout = v.findViewById(R.id.rootView)
        val ipAddress: TextView = v.findViewById(R.id.ipAddress)
        val anonymity: TextView = v.findViewById(R.id.anonymity)
        val googlePassed: TextView = v.findViewById(R.id.googlePassed)
        val sslSupport: TextView = v.findViewById(R.id.sslSupport)
    }
}