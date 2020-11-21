package org.hildan.github.secrets.wizard.http

import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import org.hildan.github.secrets.wizard.browseIfSupported

object OAuth {

    fun authorize(url: String, callbackUriParamName: String = "redirect_uri"): String {
        val server = LocalServerReceiver()
        val redirectUri = server.redirectUri
        browseIfSupported("$url&$callbackUriParamName=$redirectUri")

        return server.waitForCode()
    }
}
