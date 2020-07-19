package com.dinhhuy258.vintellij.connections

import com.google.gson.JsonElement

data class ServerRequest(val handler: String, val data: JsonElement)
