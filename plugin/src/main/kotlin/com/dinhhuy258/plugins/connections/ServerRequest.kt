package com.dinhhuy258.plugins.connections

import com.google.gson.JsonElement

data class ServerRequest(val handler: String, val data: JsonElement)
