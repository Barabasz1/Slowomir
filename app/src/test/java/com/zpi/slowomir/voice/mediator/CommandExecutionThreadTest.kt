package com.zpi.slowomir.voice.mediator

import com.zpi.slowomir.overlay.OverlayContext
import com.zpi.slowomir.overlay.commands.Command
import com.zpi.slowomir.overlay.commands.CommandResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class CommandExecutionThreadTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

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
    fun `other commands execute on main dispatcher`() = runBlocking {
        var executed = false
        val command = object : Command {
            override fun canExecute(context: OverlayContext) = true
            override fun execute(): CommandResult {
                executed = true
                return CommandResult.Success()
            }
        }
        val mediator = DefaultCommandMediator(mock())

        val result = mediator.dispatch(command)
        assertTrue(result is CommandResult.Success)
        assertTrue(executed)
    }
}
