package com.dinhhuy258.vintellij.lsp.navigation

import com.dinhhuy258.vintellij.comrade.buffer.SyncBuffer
import com.dinhhuy258.vintellij.utils.PathUtils
import com.intellij.codeInsight.navigation.GotoImplementationHandler
import com.intellij.openapi.application.ApplicationManager
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range

@Synchronized
fun goToDefinition(buffer: SyncBuffer?, position: Position): List<Location> {
    val locations = ArrayList<Location>()

    if (buffer != null) {
        ApplicationManager.getApplication().invokeAndWait {
            buffer.moveCaretToPosition(position.line, position.character)
            val editor = buffer.editor
            val gotoImplementationHandler = GotoImplementationHandler()
            val gotoData = gotoImplementationHandler.getSourceAndTargetElements(editor.editor, buffer.psiFile)
            gotoData?.targets?.forEach { target ->
                val pathWithOffset = PathUtils.getPathWithOffsetFromVirtualFileAndPsiElement(target.containingFile.virtualFile, target)
                if (pathWithOffset != null) {
                    val targetPosition = Position(0, 0)
                    val location = Location(pathWithOffset.first, Range(targetPosition, targetPosition))
                    locations.add(location)
                }
            }
        }
    }

    return locations
}
