package com.zpi.slowomir.voice.mediator

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

class CommandExecutionThreadTest {
    @Test
    fun `commands requiring background verification execute away from caller thread`() = runBlocking {
        val callerThread = Thread.currentThread()
        var executionThread: Thread? = null
        val command = object : Command {
            override val requiresBackgroundExecution = true
            override fun canExecute(context: OverlayContext) = true
            override fun execute(): CommandResult {
                executionThread = Thread.currentThread()
                return CommandResult.Success()
            }
        }
        val mediator = DefaultCommandMediator(mock())

        assertTrue(mediator.dispatch(command) is CommandResult.Success)
        assertNotSame(callerThread, executionThread)
    }

    @Test
    fun `other commands keep executing on caller thread`() = runBlocking {
        val callerThread = Thread.currentThread()
        var executionThread: Thread? = null
        val command = object : Command {
            override fun canExecute(context: OverlayContext) = true
            override fun execute(): CommandResult {
                executionThread = Thread.currentThread()
                return CommandResult.Success()
            }
        }
        val mediator = DefaultCommandMediator(mock())

        assertTrue(mediator.dispatch(command) is CommandResult.Success)
        assertSame(callerThread, executionThread)
    }
}
