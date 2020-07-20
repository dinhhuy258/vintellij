package com.dinhhuy258.vintellij.components

import com.dinhhuy258.vintellij.connections.VIServer
import com.intellij.openapi.components.ApplicationComponent

class VIApplicationComponent: ApplicationComponent {
    private val server = VIServer(getPort())

    override fun initComponent() {
        server.start()
    }

    private fun getPort(): Int {
        return System.getProperty("vi.server.port")?.toInt() ?: return 6969
    }
}
