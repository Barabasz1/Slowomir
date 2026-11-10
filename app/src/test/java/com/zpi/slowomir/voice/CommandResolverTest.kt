package com.zpi.slowomir.voice

import com.zpi.slowomir.overlay.commands.CommandArgument
import com.zpi.slowomir.overlay.commands.CommandIdentifier
import com.zpi.slowomir.overlay.commands.grid.DivideCellCommand
import com.zpi.slowomir.overlay.commands.grid.GridEnableCommand
import com.zpi.slowomir.overlay.commands.grid.GridResetCommand
import com.zpi.slowomir.overlay.commands.grid.GridToggleCommand
import com.zpi.slowomir.overlay.commands.grid.LongTapCellCommand
import com.zpi.slowomir.overlay.commands.grid.SelectCellCommand
import com.zpi.slowomir.overlay.commands.grid.TapCellCommand
import com.zpi.slowomir.overlay.commands.grid.UndivideCellCommand
import com.zpi.slowomir.overlay.commands.media.VolumeDownCommand
import com.zpi.slowomir.overlay.commands.media.VolumeUpCommand
import com.zpi.slowomir.overlay.commands.navigation.*
import com.zpi.slowomir.overlay.commands.phoneCalls.*
import com.zpi.slowomir.overlay.commands.textEdit.KeyboardHideCommand
import com.zpi.slowomir.overlay.commands.textEdit.KeyboardShowCommand
import com.zpi.slowomir.overlay.commands.textEdit.KeyboardToggleCommand
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class CommandResolverTest {

    private val notificationsCommandFactory = mock<NotificationsCommand.Factory>()
    private val quickSettingsCommandFactory = mock<QuickSettingsCommand.Factory>()
    private val lockScreenCommandFactory = mock<LockScreenCommand.Factory>()
    private val screenshotCommandFactory = mock<ScreenshotCommand.Factory>()
    private val navigateHomeCommandFactory = mock<NavigateHomeCommand.Factory>()
    private val navigateRecentCommandFactory = mock<NavigateRecentCommand.Factory>()
    private val navigateBackCommandFactory = mock<NavigateBackCommand.Factory>()
    private val keyboardHideCommandFactory = mock<KeyboardHideCommand.Factory>()
    private val keyboardShowCommandFactory = mock<KeyboardShowCommand.Factory>()
    private val keyboardToggleCommandFactory = mock<KeyboardToggleCommand.Factory>()
    private val pickUpCommandFactory = mock<PickUpCommand.Factory>()
    private val declineCommandFactory = mock<DeclineCommand.Factory>()
    private val endCommandFactory = mock<EndCommand.Factory>()
    private val speakerCommandFactory = mock<SpeakerCommand.Factory>()
    private val callCommandFactory = mock<CallCommand.Factory>()
    private val writeSMSCommandFactory = mock<WriteSMSCommand.Factory>()
    private val swipeUpNavCommandFactory = mock<SwipeUpNavCommand.Factory>()
    private val swipeDownNavCommandFactory = mock<SwipeDownNavCommand.Factory>()
    private val swipeLeftNavCommandFactory = mock<SwipeLeftNavCommand.Factory>()
    private val swipeRightNavCommandFactory = mock<SwipeRightNavCommand.Factory>()
    private val zoomInNavCommandFactory = mock<ZoomInNavCommand.Factory>()
    private val zoomOutNavCommandFactory = mock<ZoomOutNavCommand.Factory>()
    private val volumeUpCommandFactory = mock<VolumeUpCommand.Factory>()
    private val volumeDownCommandFactory = mock<VolumeDownCommand.Factory>()
    private val openAppCommandFactory = mock<OpenAppCommand.Factory>()
    private val gridEnableCommandFactory = mock<GridEnableCommand.Factory>()
    private val disableGridCommandFactory = mock<GridEnableCommand.Factory>()
    private val gridToggleCommandFactory = mock<GridToggleCommand.Factory>()
    private val gridResetCommandFactory = mock<GridResetCommand.Factory>()
    private val selectCellCommandFactory = mock<SelectCellCommand.Factory>()
    private val divideCellCommandFactory = mock<DivideCellCommand.Factory>()
    private val undivideCellCommandFactory = mock<UndivideCellCommand.Factory>()
    private val tapCellCommandFactory = mock<TapCellCommand.Factory>()
    private val longTapCellCommandFactory = mock<LongTapCellCommand.Factory>()

    private val resolver = CommandResolver(
        notificationsCommandFactory = notificationsCommandFactory,
        quickSettingsCommandFactory = quickSettingsCommandFactory,
        lockScreenCommandFactory = lockScreenCommandFactory,
        screenshotCommandFactory = screenshotCommandFactory,
        navigateHomeCommandFactory = navigateHomeCommandFactory,
        navigateRecentCommandFactory = navigateRecentCommandFactory,
        navigateBackCommandFactory = navigateBackCommandFactory,
        keyboardHideCommandFactory = keyboardHideCommandFactory,
        keyboardShowCommandFactory = keyboardShowCommandFactory,
        keyboardToggleCommandFactory = keyboardToggleCommandFactory,
        pickUpCommandFactory = pickUpCommandFactory,
        declineCommandFactory = declineCommandFactory,
        endCommandFactory = endCommandFactory,
        speakerCommandFactory = speakerCommandFactory,
        callCommandFactory = callCommandFactory,
        writeSMSCommandFactory = writeSMSCommandFactory,
        swipeUpNavCommandFactory = swipeUpNavCommandFactory,
        swipeDownNavCommandFactory = swipeDownNavCommandFactory,
        swipeLeftNavCommandFactory = swipeLeftNavCommandFactory,
        swipeRightNavCommandFactory = swipeRightNavCommandFactory,
        zoomInNavCommandFactory = zoomInNavCommandFactory,
        zoomOutNavCommandFactory = zoomOutNavCommandFactory,
        volumeUpCommandFactory = volumeUpCommandFactory,
        volumeDownCommandFactory = volumeDownCommandFactory,
        openAppCommandFactory = openAppCommandFactory,
        gridEnableCommandFactory = gridEnableCommandFactory,
        disableGridCommandFactory = disableGridCommandFactory,
        gridToggleCommandFactory = gridToggleCommandFactory,
        gridResetCommandFactory = gridResetCommandFactory,
        selectCellCommandFactory = selectCellCommandFactory,
        divideCellCommandFactory = divideCellCommandFactory,
        undivideCellCommandFactory = undivideCellCommandFactory,
        tapCellCommandFactory = tapCellCommandFactory,
        longTapCellCommandFactory = longTapCellCommandFactory,
    )

    @Test
    fun `resolve PHONE_CALL delegates to callCommandFactory with recipient text argument`() {
        val mockCommand = mock<CallCommand>()
        whenever(callCommandFactory.create(recipient = "Jan Kowalski")).thenReturn(mockCommand)

        val intent = VoiceIntent(
            identifier = CommandIdentifier.PHONE_CALL,
            argument = CommandArgument.Text("Jan Kowalski"),
            rawText = "Zadzwoń do Jan Kowalski",
        )

        val result = resolver.resolve(intent)

        assertEquals(mockCommand, result)
        verify(callCommandFactory).create(recipient = "Jan Kowalski")
    }

    @Test
    fun `resolve SMS_WRITE delegates to writeSMSCommandFactory with recipient text argument`() {
        val mockCommand = mock<WriteSMSCommand>()
        whenever(writeSMSCommandFactory.create(recipient = "Mama")).thenReturn(mockCommand)

        val intent = VoiceIntent(
            identifier = CommandIdentifier.SMS_WRITE,
            argument = CommandArgument.Text("Mama"),
            rawText = "Napisz do Mama",
        )

        val result = resolver.resolve(intent)

        assertEquals(mockCommand, result)
        verify(writeSMSCommandFactory).create(recipient = "Mama")
    }

    @Test
    fun `resolve PHONE_PICK_UP delegates to pickUpCommandFactory`() {
        val mockCommand = mock<PickUpCommand>()
        whenever(pickUpCommandFactory.create()).thenReturn(mockCommand)

        val intent = VoiceIntent(identifier = CommandIdentifier.PHONE_PICK_UP, rawText = "odbierz")
        val result = resolver.resolve(intent)

        assertEquals(mockCommand, result)
        verify(pickUpCommandFactory).create()
    }

    @Test
    fun `resolve PHONE_DECLINE delegates to declineCommandFactory`() {
        val mockCommand = mock<DeclineCommand>()
        whenever(declineCommandFactory.create()).thenReturn(mockCommand)

        val intent = VoiceIntent(identifier = CommandIdentifier.PHONE_DECLINE, rawText = "odrzuć")
        val result = resolver.resolve(intent)

        assertEquals(mockCommand, result)
        verify(declineCommandFactory).create()
    }

    @Test
    fun `resolve PHONE_END_CALL delegates to endCommandFactory`() {
        val mockCommand = mock<EndCommand>()
        whenever(endCommandFactory.create()).thenReturn(mockCommand)

        val intent = VoiceIntent(identifier = CommandIdentifier.PHONE_END_CALL, rawText = "rozłącz")
        val result = resolver.resolve(intent)

        assertEquals(mockCommand, result)
        verify(endCommandFactory).create()
    }

    @Test
    fun `resolve PHONE_SPEAKER delegates to speakerCommandFactory`() {
        val mockCommand = mock<SpeakerCommand>()
        whenever(speakerCommandFactory.create()).thenReturn(mockCommand)

        val intent = VoiceIntent(identifier = CommandIdentifier.PHONE_SPEAKER, rawText = "głośnik")
        val result = resolver.resolve(intent)

        assertEquals(mockCommand, result)
        verify(speakerCommandFactory).create()
    }

    @Test
    fun `resolve CELL_PRESS calls tapCellCommandFactory`() {
        val mockCommand = mock<TapCellCommand>()
        whenever(tapCellCommandFactory.create()).thenReturn(mockCommand)

        val intent = VoiceIntent(
            identifier = CommandIdentifier.CELL_PRESS,
            rawText = "Kliknij"
        )

        val result = resolver.resolve(intent)

        assertEquals(mockCommand, result)
        verify(tapCellCommandFactory).create()
    }

    @Test
    fun `resolve KEYBOARD_TOGGLE delegates to keyboardToggleCommandFactory`() {
        val mockCommand = mock<KeyboardToggleCommand>()
        whenever(keyboardToggleCommandFactory.create()).thenReturn(mockCommand)

        val intent = VoiceIntent(identifier = CommandIdentifier.KEYBOARD_TOGGLE, rawText = "klawiatura")
        val result = resolver.resolve(intent)

        assertEquals(mockCommand, result)
        verify(keyboardToggleCommandFactory).create()
    }

    @Test
    fun `resolve KEYBOARD_HIDE delegates to keyboardHideCommandFactory`() {
        val mockCommand = mock<KeyboardHideCommand>()
        whenever(keyboardHideCommandFactory.create()).thenReturn(mockCommand)

        val intent = VoiceIntent(identifier = CommandIdentifier.KEYBOARD_HIDE, rawText = "schowaj klawiaturę")
        val result = resolver.resolve(intent)

        assertEquals(mockCommand, result)
        verify(keyboardHideCommandFactory).create()
    }

    @Test
    fun `resolve KEYBOARD_SHOW delegates to keyboardShowCommandFactory`() {
        val mockCommand = mock<KeyboardShowCommand>()
        whenever(keyboardShowCommandFactory.create()).thenReturn(mockCommand)

        val intent = VoiceIntent(identifier = CommandIdentifier.KEYBOARD_SHOW, rawText = "pokaż klawiaturę")
        val result = resolver.resolve(intent)

        assertEquals(mockCommand, result)
        verify(keyboardShowCommandFactory).create()
    }

    @Test
    fun `resolve GRID_TOGGLE delegates to gridToggleCommandFactory`() {
        val mockCommand = mock<GridToggleCommand>()
        whenever(gridToggleCommandFactory.create()).thenReturn(mockCommand)

        val intent = VoiceIntent(identifier = CommandIdentifier.GRID_TOGGLE, rawText = "siatka")
        val result = resolver.resolve(intent)

        assertEquals(mockCommand, result)
        verify(gridToggleCommandFactory).create()
    }

    @Test
    fun `resolve GRID_SHOW delegates to gridEnableCommandFactory`() {
        val mockCommand = mock<GridEnableCommand>()
        whenever(gridEnableCommandFactory.create()).thenReturn(mockCommand)

        val intent = VoiceIntent(identifier = CommandIdentifier.GRID_SHOW, rawText = "pokaż siatkę")
        val result = resolver.resolve(intent)

        assertEquals(mockCommand, result)
        verify(gridEnableCommandFactory).create()
    }

    @Test
    fun `resolve GRID_RESET delegates to gridResetCommandFactory`() {
        val mockCommand = mock<GridResetCommand>()
        whenever(gridResetCommandFactory.create()).thenReturn(mockCommand)

        val intent = VoiceIntent(identifier = CommandIdentifier.GRID_RESET, rawText = "zresetuj siatkę")
        val result = resolver.resolve(intent)

        assertEquals(mockCommand, result)
        verify(gridResetCommandFactory).create()
    }
}
