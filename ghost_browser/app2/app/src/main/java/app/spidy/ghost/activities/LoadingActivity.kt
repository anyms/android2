package app.spidy.ghost.activities

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import app.spidy.ghost.R
import app.spidy.ghost.utils.TinyDB
import app.spidy.hiper.controllers.Hiper
import xyz.oboloi.openvpn.OboloiVPN
import xyz.oboloi.openvpn.OnVPNStatusChangeListener

class LoadingActivity : AppCompatActivity() {
    companion object {
        lateinit var vpnController: OboloiVPN
    }
    private val vpnLinks = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)
        TinyDB(applicationContext).putBoolean(IntroActivity.IS_SHOWN, true)

        vpnController = OboloiVPN(this, applicationContext)

//        val hiper = Hiper()
//        hiper.get("https://www.freeopenvpn.org/en/cf/korea.php")
//            .ifFailedOrException {
//                runOnUiThread { launch() }
//            }
//            .finally {
//                getVpnLinks(it.text)
//
//                hiper.get("https://www.freeopenvpn.org/en/cf/usa.php")
//                    .ifFailedOrException {
//                        runOnUiThread { launch() }
//                    }
//                    .finally { res ->
//                        getVpnLinks(res.text)
//                        runOnUiThread { launch() }
//                    }
//            }

        launch()

    }

    private fun getVpnLinks(text: String?) {
        if (text != null) {
            val regex = "https://www.freeopenvpn.org(.*?).ovpn".toRegex()
            val match = regex.findAll(text)
            for (m in match) {
                vpnLinks.add(m.value)
            }
        }
    }

    fun vpnDisconnect() {
        vpnController.cleanup()
        vpnController.init()
    }

    private fun launch() {
        vpnLinks.add("https://firebasestorage.googleapis.com/v0/b/ghost-96b75.appspot.com/o/vpns%2Fus1_tcp.ovpn?alt=media&token=83fe1944-4a2a-4b5e-b097-c73a9c06baea")
        vpnLinks.add("https://firebasestorage.googleapis.com/v0/b/ghost-96b75.appspot.com/o/vpns%2Fus1_udp.ovpn?alt=media&token=9bd3a5c5-d37b-474d-82db-49b4d1a85aaa")

        val link = vpnLinks.shuffled().take(1)[0]
        Log.d("hello", link)
        vpnController.launchVPN(link)
        vpnController.setOnVPNStatusChangeListener(object : OnVPNStatusChangeListener {
            override fun onProfileLoaded(isProfileLoaded: Boolean) {
                if (isProfileLoaded) vpnController.init()
            }

            @SuppressLint("SetTextI18n")
            override fun onVPNStatusChanged(isVpnActivated: Boolean) {
                if (isVpnActivated) {
                    startActivity(Intent(this@LoadingActivity, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@LoadingActivity, getString(R.string.auth_error_message), Toast.LENGTH_LONG).show()
                }
            }
        })
    }

    override fun onResume() {
        LoadingActivity.vpnController.onResume()
        super.onResume()
    }

    override fun onPause() {
        LoadingActivity.vpnController.onPause()
        super.onPause()
    }
}
