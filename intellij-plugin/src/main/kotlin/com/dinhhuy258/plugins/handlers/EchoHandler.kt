package com.dinhhuy258.plugins.handlers

import com.google.gson.JsonElement

class EchoHandler: BaseHandler<JsonElement, String>() {
    override fun requestClass(): Class<JsonElement> {
        return JsonElement::class.java
    }

    override fun handleInternal(request: JsonElement): String {
        return request.toString()
    }
}
