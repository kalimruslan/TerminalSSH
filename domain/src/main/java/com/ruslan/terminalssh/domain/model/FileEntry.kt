package com.ruslan.terminalssh.domain.model

data class FileEntry(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val permissions: String,
    val modifiedTime: Long,
    val owner: String = ""
)
