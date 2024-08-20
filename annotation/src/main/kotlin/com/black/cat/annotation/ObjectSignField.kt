package com.black.cat.annotation

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FIELD)
annotation class ObjectSignField(val ignore: Boolean = false)