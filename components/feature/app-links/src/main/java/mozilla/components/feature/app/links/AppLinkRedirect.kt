/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package mozilla.components.feature.app.links

import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * Data class for the external Intent or fallback URL a given URL encodes for.
 */
data class AppLinkRedirect(
    val appIntent: Intent?,
    val webUrl: String?,
    val isFallback: Boolean,
    @StringRes
    val appName: Int? = null,
    @DrawableRes
    val appIconResource: Int? = null
) {
    fun hasExternalApp() = appIntent != null

    fun hasFallback() = webUrl != null && isFallback

    fun isRedirect() = hasExternalApp() || hasFallback()

    fun isInstall() = appIntent?.data?.scheme == "market"
}
