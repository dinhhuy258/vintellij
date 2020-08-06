package com.dinhhuy258.vintellij.handlers

import com.dinhhuy258.vintellij.idea.IdeaUtils
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.idea.core.util.toPsiFile

class RefreshFileHandler : BaseHandler<RefreshFileHandler.Request, RefreshFileHandler.Response>() {
    data class Request(val file: String)

    data class Response(val file: String)

    override fun requestClass(): Class<Request> {
        return Request::class.java
    }

    override fun handleInternal(request: Request): Response {
        val virtualFile = IdeaUtils.getVirtualFile(request.file)
        val application = ApplicationManager.getApplication()
        val project = IdeaUtils.getProject()
        application.invokeAndWait {
            application.runWriteAction {
                virtualFile.refresh(false, false)
            }
        }

        val psiFileRef = Ref<PsiFile>()
        application.runReadAction {
            psiFileRef.set(virtualFile.toPsiFile(project))
        }
        val psiFile = psiFileRef.get()

        application.invokeAndWait {
            application.runWriteAction {
                PsiManager.getInstance(project).reloadFromDisk(psiFile)
            }
        }

        return Response(virtualFile.name)
    }
}
