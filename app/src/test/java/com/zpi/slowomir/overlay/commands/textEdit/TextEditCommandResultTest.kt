package com.zpi.slowomir.overlay.commands.textEdit

import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import com.zpi.slowomir.overlay.executors.TextEditExecutor
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import javax.inject.Provider

class TextEditCommandResultTest {
    private data class Case(
        val command: Command,
        val action: (TextEditExecutor) -> Boolean,
    )

    private fun cases(provider: Provider<TextEditExecutor?>): List<Case> = listOf(
        Case(CursorForwardCommand(provider), TextEditExecutor::cursorForward),
        Case(CursorBackwardCommand(provider), TextEditExecutor::cursorBackward),
        Case(CursorWordNextCommand(provider), TextEditExecutor::cursorWordNext),
        Case(CursorWordPreviousCommand(provider), TextEditExecutor::cursorWordPrevious),
        Case(DeleteCharCommand(provider), TextEditExecutor::deleteChar),
        Case(DeleteWordCommand(provider), TextEditExecutor::deleteWord),
        Case(ConfirmCommand(provider), TextEditExecutor::confirm),
        Case(CancelCommand(provider), TextEditExecutor::cancel),
    )

    @Test
    fun `all verified editing commands request background execution`() {
        cases { null }.forEach { assertTrue(it.command.requiresBackgroundExecution) }
    }

    @Test
    fun `disabled accessibility service produces failure for every editing command`() {
        cases { null }.forEach { assertTrue(it.command.execute() is CommandResult.Failure) }
    }

    @Test
    fun `every editing command reports the result of its corresponding executor action`() {
        val executor = mock<TextEditExecutor>()
        for (case in cases { executor }) {
            for (succeeded in listOf(true, false)) {
                reset(executor)
                whenever(case.action(executor)).thenReturn(succeeded)
                val result = case.command.execute()
                if (succeeded) {
                    assertTrue(result is CommandResult.Success)
                } else {
                    assertTrue(result is CommandResult.Failure)
                }
                case.action(verify(executor))
                verifyNoMoreInteractions(executor)
            }
        }
    }
}
