package app.spidy.freeproxylist.utils

import app.spidy.freeproxylist.data.Country
import app.spidy.freeproxylist.data.Proxy
import app.spidy.kotlinutils.toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import org.json.JSONObject

object ProxyFetcher {
    private val db = FirebaseFirestore.getInstance()
    private val gson = Gson()

    fun getCountries(listener: CountryListener) {
        db.collection("proxy").document("meta").get().addOnSuccessListener { countries ->
            if (countries.data != null) {
                val data = JSONObject(countries.data!!).getJSONArray("countries")
                val output = ArrayList<Country>()
                for (i in 0 until data.length()) {
                    val country = data.getJSONObject(i)
                    output.add(Country(
                        countryName = country.getString("countryName"),
                        countryCode = country.getString("countryCode")
                    ))
                }
                listener.onSuccess(output)
            }
        }.addOnFailureListener {
            listener.onFail()
        }
    }


    fun getProxies(countryName: String, listener: ProxyListener) {
        val output = ArrayList<Proxy>()

        db.collection("proxy").document(countryName).get()
            .addOnSuccessListener {
                if (it.data != null) {
                    val data = JSONObject(it.data!!)
                    val proxies = data.getJSONArray("proxies")
                    for (i in 0 until proxies.length()) {
                        val proxy = proxies.getJSONObject(i)
                        output.add(Proxy(
                            ip = proxy.getString("ip"),
                            port = proxy.getString("port"),
                            countryCode = proxy.getString("countryCode"),
                            countryName = proxy.getString("countryName"),
                            anonymity = proxy.getString("anonymity"),
                            googlePassed = proxy.getBoolean("googlePassed"),
                            sslSupport = proxy.getBoolean("sslSupport")
                        ))
                    }
                    listener.onSuccess(output)
                } else {
                    listener.onFail()
                }
            }
            .addOnFailureListener {
                listener.onFail()
            }
    }


    interface CountryListener {
        fun onFail()
        fun onSuccess(countries: List<Country>)
    }

    interface ProxyListener {
        fun onFail()
        fun onSuccess(proxies: List<Proxy>)
    }
}