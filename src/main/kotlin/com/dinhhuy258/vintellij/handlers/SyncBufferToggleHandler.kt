package com.dinhhuy258.vintellij.handlers

import com.dinhhuy258.vintellij.comrade.core.NvimInstance

class SyncBufferToggleHandler(private val nvimInstance: NvimInstance)
    : BaseHandler<SyncBufferToggleHandler.Request, SyncBufferToggleHandler.Response>() {

    data class Request(val enable: Boolean)

    data class Response(val enable: Boolean)

    override fun requestClass(): Class<Request> {
        return Request::class.java
    }

    override fun handleInternal(request: Request): Response {
        nvimInstance.updateSyncBuffer(request.enable)
        return Response(request.enable)
    }
}


