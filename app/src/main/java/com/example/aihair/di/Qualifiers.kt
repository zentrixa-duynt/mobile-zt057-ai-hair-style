package com.example.aihair.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OpenAIOkHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UploadOkHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OpenAIRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class UploadRetrofit
