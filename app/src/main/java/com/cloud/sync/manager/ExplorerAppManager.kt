package com.cloud.sync.manager

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.cloud.sync.manager.interfaces.IExplorerAppManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

@ViewModelScoped
class ExplorerAppManager @Inject constructor(
    @ApplicationContext private val appContext: Context
) : IExplorerAppManager {

    override fun isExplorerInstalled(): Boolean {
        return try {
            appContext.packageManager.getPackageInfo(EXPLORER_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    override fun openExplorer() {
        val packageManager = appContext.packageManager
        val deepLinkIntent = Intent(Intent.ACTION_VIEW, Uri.parse(EXPLORER_DEEPLINK)).apply {
            setPackage(EXPLORER_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            appContext.startActivity(deepLinkIntent)
            return
        } catch (e: ActivityNotFoundException) {
            val launchIntent = packageManager.getLaunchIntentForPackage(EXPLORER_PACKAGE)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                appContext.startActivity(launchIntent)
                return
            }
        }
    }

    override fun openExplorerDownloadPage() {
        val downloadIntent = Intent(Intent.ACTION_VIEW, Uri.parse(EXPLORER_DOWNLOAD_URL)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        appContext.startActivity(downloadIntent)
    }

    private companion object {
        const val EXPLORER_PACKAGE = "com.khayym.cloudstorage"
        const val EXPLORER_DEEPLINK = "com.cloudapp://"
        const val EXPLORER_DOWNLOAD_URL =
            "https://github.com/ramazan199/graphene-cloud-explorer-react-native-app/releases/tag/v1.0.0"
    }
}
