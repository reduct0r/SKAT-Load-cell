package com.h2grow.skat_load_cell.di

import android.content.Context
import com.h2grow.skat_load_cell.data.ble.BleScanner
import com.h2grow.skat_load_cell.data.ble.SkatLoadCellManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BleModule {

    @Provides
    @Singleton
    fun provideBleScanner(
        @ApplicationContext context: Context,
    ): BleScanner = BleScanner(context)

    @Provides
    @Singleton
    fun provideSkatLoadCellManager(
        @ApplicationContext context: Context,
    ): SkatLoadCellManager = SkatLoadCellManager(context)
}
