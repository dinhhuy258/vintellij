package com.dinhhuy258.plugins.handlers

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject

abstract class BaseHandler<RequestType, ResponseType>: VIHandler {
    private val gson = Gson()

    protected abstract fun requestClass(): Class<RequestType>

    protected abstract fun handleInternal(request: RequestType): ResponseType

    private fun validate(request: RequestType) {
    }

    override fun handle(data: JsonElement): JsonElement {
        val request = gson.fromJson(data, requestClass())
        validate(request)

        return gson.toJsonTree(handleInternal(request))
    }
}
