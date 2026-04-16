package com.graphenelab.photosync.manager.interfaces

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import com.graphenelab.photosync.manager.PermissionSet

interface IPermissionsManager {
    fun setLauncher(launcher: ActivityResultLauncher<Array<String>>)
    fun requestPermissions(permissionSet: PermissionSet)
    fun hasPermissions(context: Context, permissionSet: PermissionSet): Boolean
}