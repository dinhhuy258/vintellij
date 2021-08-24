package com.dinhhuy258.vintellij.lsp.navigation

import com.dinhhuy258.vintellij.lsp.Buffer
import com.dinhhuy258.vintellij.lsp.utils.getLocation
import com.intellij.codeInsight.navigation.GotoImplementationHandler
import com.intellij.openapi.application.ApplicationManager
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position

fun goToImplementation(buffer: Buffer?, position: Position): List<Location> {
    val locations = ArrayList<Location>()

    if (buffer != null) {
        ApplicationManager.getApplication().invokeAndWait {
            buffer.moveCaretToPosition(position.line, position.character)
            val editor = buffer.editor
            val gotoImplementationHandler = GotoImplementationHandler()
            val gotoData = gotoImplementationHandler.getSourceAndTargetElements(editor.editor, buffer.psiFile)
            gotoData?.targets?.forEach { target ->
                val location = getLocation(target)
                if (location != null) {
                    locations.add(location)
                }
            }
        }
    }

    return locations
}
