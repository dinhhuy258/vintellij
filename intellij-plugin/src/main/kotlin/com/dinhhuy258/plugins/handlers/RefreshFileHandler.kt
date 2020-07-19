package com.dinhhuy258.plugins.handlers

import com.dinhhuy258.plugins.idea.IdeaUtils

class RefreshFileHandler : BaseHandler<RefreshFileHandler.Request, RefreshFileHandler.Response>() {
    data class Request(val file: String)

    data class Response(val file: String)

    override fun requestClass(): Class<Request> {
        return Request::class.java
    }

    override fun handleInternal(request: Request): Response {
        val virtualFile = IdeaUtils.getVirtualFile(request.file)
        virtualFile.refresh(true, false)

        return Response(virtualFile.name)
    }
}
