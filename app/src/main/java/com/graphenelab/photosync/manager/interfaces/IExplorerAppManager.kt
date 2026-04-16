package com.graphenelab.photosync.manager.interfaces

interface IExplorerAppManager {
    fun isExplorerInstalled(): Boolean
    fun openExplorer()
    fun openExplorerDownloadPage()
}
