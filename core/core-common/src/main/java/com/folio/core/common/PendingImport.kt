package com.folio.core.common

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingImport @Inject constructor() {

    private val _uri = MutableStateFlow<Uri?>(null)
    val uri: StateFlow<Uri?> = _uri.asStateFlow()

    fun consume(): Uri? {
        val value = _uri.value
        _uri.value = null
        return value
    }

    fun set(uri: Uri?) {
        _uri.value = uri
    }
}
