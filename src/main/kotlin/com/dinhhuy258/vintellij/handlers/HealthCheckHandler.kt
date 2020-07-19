package com.dinhhuy258.vintellij.handlers

import com.google.gson.JsonElement
import com.google.gson.JsonObject

class HealthCheckHandler: VIHandler {
    override fun handle(data: JsonElement): JsonElement {
       return JsonObject()
    }
}
