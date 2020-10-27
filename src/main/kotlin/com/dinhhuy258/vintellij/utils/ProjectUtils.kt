package com.dinhhuy258.vintellij.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiFile
import java.io.File
import java.net.URLDecoder
import java.nio.file.Paths

fun getProject(projectUri: String): Project? {
    val newUri = normalizeUri(projectUri)
    val directory = File(uriToPath(newUri))
    if (!directory.isDirectory) {
        return null
    }

    val projectPath = directory.absolutePath
    if (!File(projectPath).exists()) {
        return null
    }

    val projectManager = ProjectManagerEx.getInstanceEx()
    val projectRef = Ref<Project>()
    ApplicationManager.getApplication().runWriteAction {
        try {
            val isProjectOpenned = projectManager.openProjects.find {
                uriToPath(it.baseDir.path).equals(projectPath.replace("\\", "/"), true)
            }

            val project = isProjectOpenned ?: projectManager.loadAndOpenProject(projectPath)
            projectRef.set(project)
        } catch (e: Throwable) {
            projectRef.set(null)
        }
    }

    val project = projectRef.get()
    if (project == null || project.isDisposed) {
        return null
    }

    while (!project.isInitialized) {
        Thread.sleep(1000)
    }
    return project
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

private fun normalizeUri(uri: String): String {
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
