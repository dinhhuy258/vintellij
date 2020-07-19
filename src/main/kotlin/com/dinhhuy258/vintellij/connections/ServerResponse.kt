package com.dinhhuy258.vintellij.connections

import com.google.gson.JsonElement

class ServerResponse private constructor(val success: Boolean, val handler: String?, val data: JsonElement?, val message: String?) {
    companion object {
        fun success(data: JsonElement, handler: String): ServerResponse {
            return ServerResponse(true, handler, data, null)
        }

        fun fail(message: String): ServerResponse {
            return ServerResponse(false, null, null, message)
        }
    }
}
