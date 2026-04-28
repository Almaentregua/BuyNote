package com.martinjm.buynote.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// El provider de AppDatabase se agrega en la historia 1.1 junto con las primeras entidades.
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule
