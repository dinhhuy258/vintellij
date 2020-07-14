package com.dinhhuy258.plugins.components

import com.dinhhuy258.plugins.connections.VIServer
import com.intellij.openapi.components.ApplicationComponent

class VIApplicationComponent: ApplicationComponent {
    private val server = VIServer(getPort())

    override fun initComponent() {
        println("Application start")
        server.start()
    }

    private fun getPort(): Int {
        return System.getProperty("vi.server.port")?.toInt() ?: return 6969
    }
}
