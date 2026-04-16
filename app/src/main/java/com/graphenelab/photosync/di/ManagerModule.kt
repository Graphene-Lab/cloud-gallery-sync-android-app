package com.graphenelab.photosync.di

import com.graphenelab.photosync.manager.ExplorerAppManager
import com.graphenelab.photosync.manager.interfaces.IOAuthManager
import com.graphenelab.photosync.manager.interfaces.IExplorerAppManager
import com.graphenelab.photosync.manager.interfaces.IPermissionsManager
import com.graphenelab.photosync.manager.interfaces.IQRScanner
import com.graphenelab.photosync.manager.OAuthManager
import com.graphenelab.photosync.manager.PermissionsManager
import com.graphenelab.photosync.manager.QRScanner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped


@Module
@InstallIn(ViewModelComponent::class)
abstract class ManagerModule {

    @Binds
    @ViewModelScoped
    abstract fun bindPermissionsManager(
        impl: PermissionsManager
    ): IPermissionsManager

    @Binds
    @ViewModelScoped
    //TODO: change to class level binding
    abstract fun bindQRScanner(
        impl: QRScanner
    ): IQRScanner

    @Binds
    @ViewModelScoped
    abstract fun bindAuthManager(
        impl: OAuthManager
    ): IOAuthManager

    @Binds
    @ViewModelScoped
    abstract fun bindExplorerAppManager(
        impl: ExplorerAppManager
    ): IExplorerAppManager
}
