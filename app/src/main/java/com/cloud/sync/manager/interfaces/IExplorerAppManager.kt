package com.cloud.sync.manager.interfaces

interface IExplorerAppManager {
    fun isExplorerInstalled(): Boolean
    fun openExplorer()
    fun openExplorerDownloadPage()
}
