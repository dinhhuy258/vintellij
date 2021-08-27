package com.dinhhuy258.vintellij.navigation

import com.dinhhuy258.vintellij.buffer.Buffer
import com.dinhhuy258.vintellij.utils.getLocation
import com.dinhhuy258.vintellij.utils.invokeAndWait
import com.dinhhuy258.vintellij.utils.runReadAction
import com.intellij.codeInsight.navigation.GotoImplementationHandler
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position

fun goToImplementation(buffer: Buffer?, position: Position): List<Location> {
    val locations = ArrayList<Location>()

    if (buffer != null) {
        invokeAndWait {
            runReadAction {
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
    }

    return locations
}
