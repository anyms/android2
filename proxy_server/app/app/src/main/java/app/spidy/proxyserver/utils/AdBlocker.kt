package app.spidy.proxyserver.utils

import java.util.*

object AdBlocker {
    private val startsHosts = listOf<Any>(
        "ad.",
        "pagead2.",
        "ade."
    )
    private val containsHosts = listOf<Any>(
        "googleads.",
        "googleadservices.com",
        "adsco.re",

        "amazon-adsystem.com",
        "moatads.com",
        "addthis.com",
        "displayvertising.com",
        "adskeeper.co",

        "steepto.com",
        "ueuodgnrhb.com",
        "mgid.com",

        "ratlyepin.club",
        "wastesshimssat.world",
        "outtunova.com",
        "nunhoefey.com",
        "nibzitgas.com",
        "inpagepush.com"
    )

    private fun checkList(innerHosts: List<String>, url: String): Boolean {
        for (h in innerHosts) {
            if (!url.contains(h)) {
                return false
            }
        }
        return true
    }

    private fun checkContainsHosts(url: String, isAlreadyValidated: Boolean): Boolean {
        if (isAlreadyValidated) {
            for (h in containsHosts) {
                if (h::class.simpleName.toString().toLowerCase(Locale.ROOT).contains("list")) {
                    if (checkList(
                            h as List<String>,
                            url
                        )
                    ) {
                        return true
                    }
                } else if (url.contains(h.toString())) {
                    return true
                }
            }
            return false
        }

        return false
    }

    private fun checkStartsHosts(url: String, isAlreadyValidated: Boolean): Boolean {
        if (isAlreadyValidated) {
            for (h in containsHosts) {
                if (h::class.simpleName.toString().toLowerCase(Locale.ROOT).contains("list")) {
                    if (checkList(
                            h as List<String>,
                            url
                        )
                    ) {
                        return true
                    }
                } else if (url.startsWith(h.toString())) {
                    return true
                }
            }
            return false
        }

        return false
    }

    fun isAd(url: String, isAlreadyValidated: Boolean = false): Boolean {
        if (checkStartsHosts(
                url,
                isAlreadyValidated
            ) || checkContainsHosts(
                url,
                isAlreadyValidated
            )
        ) {
            return true
        }
        return false
    }
}