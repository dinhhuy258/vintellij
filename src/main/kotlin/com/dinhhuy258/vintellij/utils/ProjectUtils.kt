package com.dinhhuy258.vintellij.utils

import com.intellij.psi.PsiFile
import java.net.URLDecoder
import java.nio.file.Paths

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
