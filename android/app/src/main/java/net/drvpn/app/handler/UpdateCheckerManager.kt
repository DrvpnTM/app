package net.drvpn.app.handler

import android.os.Build
import net.drvpn.app.AppConfig
import net.drvpn.app.BuildConfig
import net.drvpn.app.dto.CheckUpdateResult
import net.drvpn.app.dto.GitHubRelease
import net.drvpn.app.dto.UrlContentRequest
import net.drvpn.app.extension.concatUrl
import net.drvpn.app.util.HttpUtil
import net.drvpn.app.util.JsonUtil
import net.drvpn.app.util.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object UpdateCheckerManager {
    suspend fun checkForUpdate(includePreRelease: Boolean = false): CheckUpdateResult = withContext(Dispatchers.IO) {
        // Always read the release list: the repo also holds "countries-v*" releases for the
        // per-country editions, so "releases/latest" could point at the wrong channel.
        val url = AppConfig.APP_API_URL

        val proxyUsername = SettingsManager.getSocksUsername()
        val proxyPassword = SettingsManager.getSocksPassword()

        var response = HttpUtil.getUrlContent(
            UrlContentRequest(
                url = url,
                timeout = 5000
            )
        )
        if (response.isNullOrEmpty()) {
            val httpPort = SettingsManager.getHttpPort()
            response = HttpUtil.getUrlContent(
                UrlContentRequest(
                    url = url,
                    timeout = 5000,
                    httpPort = httpPort,
                    proxyUsername = proxyUsername,
                    proxyPassword = proxyPassword
                )
            )
                ?: throw IllegalStateException("Failed to get response")
        }

        val tagPrefix = if (isCountryEdition()) COUNTRY_TAG_PREFIX else "v"
        val latestRelease = JsonUtil.fromJsonSafe(response, Array<GitHubRelease>::class.java)
            ?.firstOrNull { release ->
                release.tagName.startsWith(tagPrefix) &&
                    (isCountryEdition() || !release.tagName.startsWith(COUNTRY_TAG_PREFIX)) &&
                    (includePreRelease || !release.prerelease)
            }
            ?: return@withContext CheckUpdateResult(hasUpdate = false)

        val latestVersion = latestRelease.tagName.removePrefix(tagPrefix)
        LogUtil.i(
            AppConfig.TAG,
            "Found new version: $latestVersion (current: ${BuildConfig.VERSION_NAME})"
        )

        return@withContext if (compareVersions(latestVersion, BuildConfig.VERSION_NAME) > 0) {
            val downloadUrl = getDownloadUrl(latestRelease, Build.SUPPORTED_ABIS[0])
            CheckUpdateResult(
                hasUpdate = true,
                latestVersion = latestVersion,
                releaseNotes = latestRelease.body,
                downloadUrl = downloadUrl,
                isPreRelease = latestRelease.prerelease
            )
        } else {
            CheckUpdateResult(hasUpdate = false)
        }
    }

    private fun compareVersions(version1: String, version2: String): Int {
        val v1 = version1.split(".")
        val v2 = version2.split(".")

        for (i in 0 until maxOf(v1.size, v2.size)) {
            val num1 = v1.getOrNull(i)?.toIntOrNull() ?: 0
            val num2 = v2.getOrNull(i)?.toIntOrNull() ?: 0
            if (num1 != num2) return num1 - num2
        }
        return 0
    }

    private const val COUNTRY_TAG_PREFIX = "countries-v"

    private fun isCountryEdition(): Boolean = BuildConfig.COUNTRY_CODE.isNotEmpty()

    private fun getDownloadUrl(release: GitHubRelease, abi: String): String {
        if (isCountryEdition()) {
            // Country editions only ship a universal APK named DrVPN_<code>_<version>_universal.apk,
            // so the update keeps the same app name, id and the user's chosen language.
            val prefix = "DrVPN_${BuildConfig.COUNTRY_CODE}_"
            return release.assets.firstOrNull { it.name.startsWith(prefix) }?.browserDownloadUrl
                ?: throw IllegalStateException("No APK found for this country edition")
        }

        val fDroid = "fdroid"

        val assetsByAbi = release.assets.filter {
            (it.name.contains(abi, true))
        }

        val asset = if (BuildConfig.APPLICATION_ID.contains(fDroid, ignoreCase = true)) {
            assetsByAbi.firstOrNull { it.name.contains(fDroid) }
        } else {
            assetsByAbi.firstOrNull { !it.name.contains(fDroid) }
        }

        return asset?.browserDownloadUrl
            ?: throw IllegalStateException("No compatible APK found")
    }
}
