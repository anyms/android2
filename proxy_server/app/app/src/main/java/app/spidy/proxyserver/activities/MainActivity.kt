package app.spidy.proxyserver.activities

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import app.spidy.chaiui.ChaiEditText
import app.spidy.hiper.Hiper
import app.spidy.kotlinutils.Permission
import app.spidy.kotlinutils.TinyDB
import app.spidy.kotlinutils.ignore
import app.spidy.kotlinutils.toast
import app.spidy.proxyserver.R
import app.spidy.proxyserver.databases.ProxyDatabase
import app.spidy.proxyserver.services.InappropDownloadService
import app.spidy.proxyserver.services.ProxyServer
import app.spidy.proxyserver.services.TrackingDownloadService
import app.spidy.proxyserver.utils.Ads
import app.spidy.proxyserver.utils.C
import app.spidy.proxyserver.utils.html
import app.spidy.proxyserver.utils.newDialog
import com.anjlab.android.iab.v3.BillingProcessor
import com.anjlab.android.iab.v3.TransactionDetails
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import kotlinx.android.synthetic.main.activity_main.*
import java.net.Inet4Address
import java.net.NetworkInterface


class MainActivity : AppCompatActivity(), BillingProcessor.IBillingHandler {
    private lateinit var tinyDB: TinyDB
    private lateinit var database: ProxyDatabase
    private lateinit var billingProcessor: BillingProcessor
    private lateinit var permission: Permission

    private val hiper = Hiper.getAsyncInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tinyDB = TinyDB(this)
        database = Room.databaseBuilder(this, ProxyDatabase::class.java, "ProxyDatabase")
            .fallbackToDestructiveMigration().build()
        permission = Permission(this)

        billingProcessor = BillingProcessor(this,
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkqz95QM+5Yd4Vwd/kjQGJJjp4Molpa3DJ9eLd4heQIORZBsFTC5yCdDYbhgD3dQzzxUuWguEWTrUbHRdSSVIqEXMQkez51WcCLOONHcK1PLjF5euELIJOTe4hWHCg9cFpeb17Nu5L0Bub2T+5lAaZh8zvTUVdMT31YyDFtWdKoW90ab1uJcc7eBXiZpsHxksU4MQHpDnpCoSG5it1kVrKdNd7W6QjIDO4LnLxsDctI2d8ebmgR1UgpwqWaWLUoE5YWmcVgLhYh67yW3wFAdloQSjlHhmc79JaBCbJrxgZhps5iPVulAPpx7lpZhK22eAodib4mFrHTeHECaP0gUTbwIDAQAB",
            this)
        billingProcessor.initialize()


        if (!tinyDB.getBoolean("isAlreadyOpened")) {
            tinyDB.putInt(C.TAG_PROXY_PORT, 8128)
            tinyDB.putBoolean("isAlreadyOpened", true)
        }
        if (tinyDB.getBoolean("isPro")) {
            adView1.visibility = View.GONE
            adView2.visibility = View.GONE
        } else {
            Ads.initInterstitial(this)
            Ads.initReward(this)
            Ads.loadInterstitial()
            Ads.loadReward()
            adView1.loadAd(AdRequest.Builder().build())
            adView2.loadAd(AdRequest.Builder().build())
        }

        serverSwitch.isChecked = ProxyServer.isRunning
        serverSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                startService(Intent(this, ProxyServer::class.java))
            } else {
                ProxyServer.serverKill?.invoke()
            }

            if (!tinyDB.getBoolean("isPro")) {
                Ads.showInterstitial {  }
                Ads.loadInterstitial()
            }
        }

        setSupportActionBar(toolbar)
        title = ""
        toolbar.setNavigationIcon(R.drawable.ic_console)
        toolbar.setNavigationOnClickListener {
            val v = LayoutInflater.from(this).inflate(R.layout.layout_console_dialog, null)
            val outputWindow: TextView = v.findViewById(R.id.outputWindow)
            val scrollView: ScrollView = v.findViewById(R.id.scrollView)
            var output = "<font color=#00AB64>* Console initiated</font><br><br>"
            if (ProxyServer.isRunning) {
                ProxyServer.requestCallback = { request, isBlocked ->
                    runOnUiThread {
                        output += if (isBlocked) {
                            "<font color=#E74C3C>- $request</font><br>"
                        } else {
                            "+ $request<br><br>"
                        }
                        outputWindow.html(output)
                        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
                    }
                }
            }
            newDialog().withTitle("Network Console")
                .withCustomView(v)
                .withCancelable(false)
                .withPositiveButton("Cancel") {dialog, _ ->
                    ProxyServer.requestCallback = null
                    dialog.dismiss()
                }
                .show()
        }

        setupSettings()
    }

    fun purchase() {
        billingProcessor.purchase(this, "app.spidy.proxyserver")
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        permission.execute(requestCode, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (!billingProcessor.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onDestroy() {
        billingProcessor.release()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuConfigure -> {
                val v = LayoutInflater.from(this).inflate(R.layout.layout_configure_dialog, null)
                val localIpView: TextView = v.findViewById(R.id.localIpView)
                val remoteIpView: TextView = v.findViewById(R.id.remoteIpView)
                val portField: ChaiEditText = v.findViewById(R.id.portField)

                localIpView.html("Local IP: <strong>${getIpv4HostAddress()}</strong>")
                hiper.get("http://checkip.amazonaws.com/").then {
                    remoteIpView.html("Remote IP: <strong>${it.text}</strong>")
                }.catch {
                    remoteIpView.html("Remote IP: <strong>Unknown</strong>")
                }
                portField.setText(tinyDB.getInt(C.TAG_PROXY_PORT).toString())

                newDialog().withTitle("Configure")
                    .withCustomView(v)
                    .withPositiveButton("Apply") {_, _ ->
                        val port = portField.text.toString()
                        if (port.trim() == "") {
                            toast("Port number can not be empty")
                        } else {
                            tinyDB.putInt(C.TAG_PROXY_PORT, port.toInt())
                            if (ProxyServer.isRunning) {
                                ProxyServer.serverKill?.invoke()
                                startService(Intent(this, ProxyServer::class.java))
                            }
                        }
                    }
                    .withNegativeButton("Cancel") {d, _ -> d.dismiss() }
                    .show()
            }

            R.id.menuShare -> {
                ignore {
                    val shareIntent = Intent(Intent.ACTION_SEND);
                    shareIntent.type = "text/plain";
                    shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Proxy Server");
                    var shareMessage = "\nTurn your smartphone into a proxy server\n\n";
                    shareMessage = shareMessage + "https://play.google.com/store/apps/details?id=" + packageName +"\n\n";
                    shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                    startActivity(Intent.createChooser(shareIntent, "Share with"));
                }
            }

            R.id.menuFeedback -> {
                val uri = Uri.parse("market://details?id=$packageName");
                val goToMarket = Intent(Intent.ACTION_VIEW, uri)
                goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                try {
                    startActivity(goToMarket)
                } catch (e: ActivityNotFoundException) {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("http://play.google.com/store/apps/details?id=$packageName"))
                    )
                }
            }

            R.id.menuUpgrade -> purchase()
        }

        return true
    }


    private fun getIpv4HostAddress(): String {
        NetworkInterface.getNetworkInterfaces()?.toList()?.map { networkInterface ->
            networkInterface.inetAddresses?.toList()?.find {
                !it.isLoopbackAddress && it is Inet4Address
            }?.let { return it.hostAddress }
        }
        return "127.0.0.1"
    }



    private fun canEnableInapprop(): Boolean {
        when {
            InappropDownloadService.isRunning -> {
                toast("Downloading in progress...")
                return false
            }
            tinyDB.getBoolean(InappropDownloadService.TAG_IS_DOWNLOADED) -> {
                return true
            }
            else -> {
                newDialog().withTitle("Require Downloading")
                    .withCancelable(false)
                    .withMessage("The inappropriate list has to be downloaded to enable this feature, would you like to download?")
                    .withPositiveButton("Download") {dialog, which ->
                        val intent = Intent(this, InappropDownloadService::class.java)
                        intent.putExtra("type", "inappropriate")
                        startService(intent)
                    }
                    .withNegativeButton("Cancel") {dialog, i -> dialog.dismiss() }
                    .show()

                return false
            }
        }
    }

    private fun canEnablePrivacy(): Boolean {
        when {
            TrackingDownloadService.isRunning -> {
                toast("Downloading in progress...")
                return false
            }
            tinyDB.getBoolean(TrackingDownloadService.TAG_IS_DOWNLOADED) -> {
                return true
            }
            else -> {
                newDialog().withTitle("Require Downloading")
                    .withCancelable(false)
                    .withMessage("The privacy list has to be downloaded to enable this feature, would you like to download?")
                    .withPositiveButton("Download") {dialog, which ->
                        val intent = Intent(this, TrackingDownloadService::class.java)
                        intent.putExtra("type", "tracking")
                        startService(intent)
                        dialog.dismiss()
                    }
                    .withNegativeButton("Cancel") {dialog, i -> dialog.dismiss() }
                    .show()

                return false
            }
        }
    }

    private fun setupSettings() {
        if (tinyDB.getBoolean(C.SETTINGS_INAPPROP_WEBSITES)) {
            inappropEnableBtn.visibility = View.GONE
            inappropDisableBtn.visibility = View.VISIBLE
        }
        inappropEnableBtn.setOnClickListener {
            if (tinyDB.getBoolean("isPro")) {
                permission.request(Manifest.permission.WRITE_EXTERNAL_STORAGE, null, object : Permission.Listener {
                    override fun onGranted() {
                        val canEnable = canEnableInapprop()

                        if (canEnable) {
                            inappropEnableBtn.visibility = View.GONE
                            inappropDisableBtn.visibility = View.VISIBLE
                            tinyDB.putBoolean(C.SETTINGS_INAPPROP_WEBSITES, true)
                        }
                    }
                    override fun onRejected() {}
                })
            } else {
                newDialog().withTitle("Upgrade")
                    .withMessage("This feature is only available for premium users, would you like to upgrade?")
                    .withPositiveButton("Upgrade") {_, _ ->
                        purchase()
                    }
                    .withNegativeButton("Cancel") {d, _ -> d.dismiss() }
                    .show()
            }
        }
        inappropDisableBtn.setOnClickListener {
            inappropEnableBtn.visibility = View.VISIBLE
            inappropDisableBtn.visibility = View.GONE
            tinyDB.putBoolean(C.SETTINGS_INAPPROP_WEBSITES, false)
        }


        if (tinyDB.getBoolean(C.SETTINGS_INSECURE_WEBSITES)) {
            insecureEnableBtn.visibility = View.GONE
            insecureDisableBtn.visibility = View.VISIBLE
        }
        insecureEnableBtn.setOnClickListener {
            if (tinyDB.getBoolean("isPro")) {
                insecureEnableBtn.visibility = View.GONE
                insecureDisableBtn.visibility = View.VISIBLE
                tinyDB.putBoolean(C.SETTINGS_INSECURE_WEBSITES, true)
            } else {
                newDialog().withTitle("Upgrade")
                    .withMessage("This feature is only available for premium users, would you like to upgrade?")
                    .withPositiveButton("Upgrade") {_, _ ->
                        purchase()
                    }
                    .withNegativeButton("Cancel") {d, _ -> d.dismiss() }
                    .show()
            }
        }
        insecureDisableBtn.setOnClickListener {
            insecureEnableBtn.visibility = View.VISIBLE
            insecureDisableBtn.visibility = View.GONE
            tinyDB.putBoolean(C.SETTINGS_INSECURE_WEBSITES, false)
        }


        if (tinyDB.getBoolean(C.SETTINGS_PRIVACY_PROTECTION)) {
            privacyEnableBtn.visibility = View.GONE
            privacyDisableBtn.visibility = View.VISIBLE
        }
        privacyEnableBtn.setOnClickListener {
            if (tinyDB.getBoolean("isPro")) {
                permission.request(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    null,
                    object : Permission.Listener {
                        override fun onGranted() {
                            val canEnable = canEnablePrivacy()
                            if (canEnable) {
                                privacyEnableBtn.visibility = View.GONE
                                privacyDisableBtn.visibility = View.VISIBLE
                                tinyDB.putBoolean(C.SETTINGS_PRIVACY_PROTECTION, true)
                            }
                        }

                        override fun onRejected() {}
                    })
            } else {
                newDialog().withTitle("Upgrade")
                    .withMessage("This feature is only available for premium users, would you like to upgrade?")
                    .withPositiveButton("Upgrade") {_, _ ->
                        purchase()
                    }
                    .withNegativeButton("Cancel") {d, _ -> d.dismiss() }
                    .show()
            }
        }
        privacyDisableBtn.setOnClickListener {
            privacyEnableBtn.visibility = View.VISIBLE
            privacyDisableBtn.visibility = View.GONE
            tinyDB.putBoolean(C.SETTINGS_PRIVACY_PROTECTION, false)
        }


        if (!tinyDB.getBoolean(C.SETTINGS_DATA_SERVER)) {
            dataSaverEnableBtn.visibility = View.VISIBLE
            dataSaverDisableBtn.visibility = View.GONE
        }
        dataSaverEnableBtn.setOnClickListener {
            if (tinyDB.getBoolean("isPro")) {
                dataSaverEnableBtn.visibility = View.GONE
                dataSaverDisableBtn.visibility = View.VISIBLE
                tinyDB.putBoolean(C.SETTINGS_DATA_SERVER, true)
            } else {
                newDialog().withTitle("Upgrade")
                    .withMessage("This feature is only available for premium users, would you like to upgrade?")
                    .withPositiveButton("Upgrade") {_, _ ->
                        purchase()
                    }
                    .withNegativeButton("Cancel") {d, _ -> d.dismiss() }
                    .show()
            }
        }
        dataSaverDisableBtn.setOnClickListener {
            dataSaverEnableBtn.visibility = View.VISIBLE
            dataSaverDisableBtn.visibility = View.GONE
            tinyDB.putBoolean(C.SETTINGS_DATA_SERVER, false)
        }


        userConfigBtn.setOnClickListener {
            val intent = Intent(this, UserConfigActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onBillingInitialized() {

    }

    override fun onPurchaseHistoryRestored() {

    }

    override fun onProductPurchased(productId: String, details: TransactionDetails?) {
        tinyDB.putBoolean("isPro", true)
        toast("Great! you've purchased pro version, restart the app to enable the changes.")
    }

    override fun onBillingError(errorCode: Int, error: Throwable?) {
        toast("billing failed")
    }
}