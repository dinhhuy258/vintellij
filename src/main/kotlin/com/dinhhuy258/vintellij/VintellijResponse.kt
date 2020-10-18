package com.dinhhuy258.vintellij

import com.google.gson.JsonElement

class VintellijResponse private constructor(val success: Boolean, val handler: String?, val data: JsonElement?, val message: String?) {
    companion object {
        fun success(data: JsonElement, handler: String): VintellijResponse {
            return VintellijResponse(true, handler, data, null)
        }

        fun fail(message: String): VintellijResponse {
            return VintellijResponse(false, null, null, message)
        }
    }
}
