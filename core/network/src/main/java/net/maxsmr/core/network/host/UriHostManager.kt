package net.maxsmr.core.network.host

import android.net.Uri
import net.maxsmr.core.network.URL_SCHEME_HTTP
import net.maxsmr.core.network.URL_SCHEME_HTTPS

open class UriHostManager(initialUrl: String) : HostManager {

    val uri: Uri = with(Uri.parse(initialUrl)) {
        // не включаем query в целевой Uri
        Uri.Builder()
            .scheme(
                if (URL_SCHEME_HTTPS.equals(scheme, true)) {
                    URL_SCHEME_HTTPS
                } else {
                    URL_SCHEME_HTTP
                }
            )
            .encodedAuthority(authority)
            .encodedPath(path)
            .build()
    }

    override val baseUrl: String = uri.buildUpon().path(
        // retrofit не принимает path в baseUrl
        uri.path.takeIf { it == "/" }.orEmpty()
    ).toString()
}