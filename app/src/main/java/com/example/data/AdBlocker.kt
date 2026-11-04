package com.example.data

import android.net.Uri
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

object AdBlocker {
    private val AD_DOMAINS = hashSetOf(
        "doubleclick.net",
        "googlesyndication.com",
        "googleads.g.doubleclick.net",
        "adservice.google.com",
        "pagead2.googlesyndication.com",
        "popads.net",
        "popcash.net",
        "propellerads.com",
        "adcash.com",
        "adsterra.com",
        "exoclick.com",
        "adnxs.com",
        "outbrain.com",
        "taboola.com",
        "scorecardresearch.com",
        "moatads.com",
        "criteo.com",
        "rubiconproject.com",
        "amazon-adsystem.com",
        "pubmatic.com",
        "advertising.com",
        "revcontent.com",
        "zergnet.com",
        "adcolony.com",
        "ad-delivery.net",
        "trafficjunky.net",
        "clickadu.com",
        "hilltopads.net",
        "adtrue.com"
    )

    private val AD_KEYWORDS = listOf(
        "/ads.js",
        "/pagead/",
        "/advertisement",
        "/popunder",
        "/popup-ad",
        "banner_ad",
        "adskeeper"
    )

    fun isAd(url: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase() ?: return false

            // Check exact or subdomain match
            for (domain in AD_DOMAINS) {
                if (host == domain || host.endsWith(".$domain")) {
                    return true
                }
            }

            // Check suspicious url patterns
            val path = uri.path?.lowercase() ?: ""
            for (keyword in AD_KEYWORDS) {
                if (path.contains(keyword)) {
                    return true
                }
            }

            false
        } catch (_: Exception) {
            false
        }
    }

    fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "utf-8",
            ByteArrayInputStream(ByteArray(0))
        )
    }
}
