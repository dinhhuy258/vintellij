package com.dinhhuy258.vintellij.utils

import com.intellij.psi.PsiFile
import java.net.URLDecoder
import java.nio.file.Paths

const val INTELLIJ_PATH_PREFIX = "jar://"
private const val VIM_FILE_PATH_PREFIX = "file://"
private const val VIM_ZIPFILE_PATH_PREFIX = "zipfile://"
private const val INTELLIJ_JAR_SEPARATOR = ".jar!/"
private const val VIM_JAR_SEPARATOR = ".jar::"
private const val INTELLIJ_ZIP_SEPARATOR = ".zip!/"
private const val VIM_ZIP_SEPARATOR = ".zip::"

fun getFilePath(name: String): String {
    return if (isVimJarFilePath(name)) {
        toIntellijJarFilePath(name).removePrefix(INTELLIJ_PATH_PREFIX)
    } else {
        name
    }
}

fun isIntellijJarFile(filePath: String) =
    filePath.contains(INTELLIJ_JAR_SEPARATOR) || filePath.contains(INTELLIJ_ZIP_SEPARATOR)

fun toVimJarFilePath(filePath: String): String {
    val path = if (filePath.contains(INTELLIJ_JAR_SEPARATOR)) {
        filePath.replaceFirst(INTELLIJ_JAR_SEPARATOR, VIM_JAR_SEPARATOR)
    } else {
        filePath.replaceFirst(INTELLIJ_ZIP_SEPARATOR, VIM_ZIP_SEPARATOR)
    }

    return "$VIM_ZIPFILE_PATH_PREFIX$path"
}

fun toVimFilePath(filePath: String): String {
    return "$VIM_FILE_PATH_PREFIX$filePath"
}

private fun isVimJarFilePath(filePath: String): Boolean = filePath.startsWith(VIM_ZIPFILE_PATH_PREFIX)

private fun toIntellijJarFilePath(filePath: String): String {
    val path = filePath.replaceFirst(VIM_ZIPFILE_PATH_PREFIX, INTELLIJ_PATH_PREFIX)

    return if (path.contains(VIM_JAR_SEPARATOR)) {
        path.replaceFirst(VIM_JAR_SEPARATOR, INTELLIJ_JAR_SEPARATOR)
    } else {
        path.replaceFirst(VIM_ZIP_SEPARATOR, INTELLIJ_ZIP_SEPARATOR)
    }
}

fun uriToPath(uri: String): String {
    val newUri = normalizeUri(URLDecoder.decode(uri, "UTF-8"))
    val isWindowsPath = """^file:/+\w:""".toRegex().containsMatchIn(newUri)
    return if (isWindowsPath) {
        Paths.get("^file:/+".toRegex().replace(newUri, "")).toString().replace("\\", "/")
    } else {
        "^file:/+".toRegex().replace(newUri, "/")
    }
}

fun normalizeUri(uri: String): String {
    val protocolRegex = "^file:/+".toRegex()
    val trailingSlashRegex = "/$".toRegex()
    var decodedUri = URLDecoder.decode(uri, "UTF-8")
    decodedUri = trailingSlashRegex.replace(decodedUri, "")
    decodedUri = protocolRegex.replace(decodedUri, "file:///")
    decodedUri = decodedUri.replace("\\", "/")

    val driveLetterRegex = """file:///([a-zA-Z]:)/.*""".toRegex()
    val match = driveLetterRegex.matchEntire(decodedUri)?.groups?.get(1)
    match?.let { decodedUri = decodedUri.replaceRange(it.range, it.value.toLowerCase()) }

    return decodedUri
}

fun getURIForFile(file: PsiFile) = normalizeUri(file.virtualFile.url)

