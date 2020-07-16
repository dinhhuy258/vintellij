package com.dinhhuy258.plugins.handlers

import com.dinhhuy258.plugins.idea.IdeaUtils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor

class OpenFileHandler: BaseHandler<OpenFileHandler.Request, Unit>() {
    data class Request(val file: String, val offset: Int)

    override fun requestClass(): Class<Request> {
        return Request::class.java
    }

    override fun handle(request: Request) {
        val project = IdeaUtils.getProject()
        val virtualFile = IdeaUtils.getVirtualFile(request.file)

        ApplicationManager.getApplication().invokeLater {
            FileEditorManager.getInstance(project).openTextEditor(OpenFileDescriptor(project, virtualFile, request.offset), true)
        }
    }
}
