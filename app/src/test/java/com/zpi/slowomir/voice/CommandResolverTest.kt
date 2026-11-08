package com.zpi.slowomir.voice

import com.zpi.slowomir.overlay.commands.CommandArgument
import com.zpi.slowomir.overlay.commands.CommandIdentifier
import com.zpi.slowomir.overlay.commands.TapCommand
import com.zpi.slowomir.overlay.commands.media.VolumeDownCommand
import com.zpi.slowomir.overlay.commands.media.VolumeUpCommand
import com.zpi.slowomir.overlay.commands.navigation.*
import com.zpi.slowomir.overlay.commands.phoneCalls.*
import com.zpi.slowomir.overlay.commands.textEdit.KeyboardHideCommand
import com.zpi.slowomir.overlay.commands.textEdit.KeyboardShowCommand
import com.zpi.slowomir.utils.ScreenCoordinatesProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class CommandResolverTest {

    private val tapCommandFactory = mock<TapCommand.Factory>()
    private val notificationsCommandFactory = mock<NotificationsCommand.Factory>()
    private val quickSettingsCommandFactory = mock<QuickSettingsCommand.Factory>()
    private val lockScreenCommandFactory = mock<LockScreenCommand.Factory>()
    private val screenshotCommandFactory = mock<ScreenshotCommand.Factory>()
    private val navigateHomeCommandFactory = mock<NavigateHomeCommand.Factory>()
    private val navigateRecentCommandFactory = mock<NavigateRecentCommand.Factory>()
    private val navigateBackCommandFactory = mock<NavigateBackCommand.Factory>()
    private val keyboardHideCommandFactory = mock<KeyboardHideCommand.Factory>()
    private val keyboardShowCommandFactory = mock<KeyboardShowCommand.Factory>()
    private val pickUpCommandFactory = mock<PickUpCommand.Factory>()
    private val declineCommandFactory = mock<DeclineCommand.Factory>()
    private val endCommandFactory = mock<EndCommand.Factory>()
    private val speakerCommandFactory = mock<SpeakerCommand.Factory>()
    private val callCommandFactory = mock<CallCommand.Factory>()
    private val writeSMSCommandFactory = mock<WriteSMSCommand.Factory>()
    private val swipeUpCommandFactory = mock<SwipeUpCommand.Factory>()
    private val swipeDownCommandFactory = mock<SwipeDownCommand.Factory>()
    private val swipeLeftCommandFactory = mock<SwipeLeftCommand.Factory>()
    private val swipeRightCommandFactory = mock<SwipeRightCommand.Factory>()
    private val zoomInCommandFactory = mock<ZoomInCommand.Factory>()
    private val zoomOutCommandFactory = mock<ZoomOutCommand.Factory>()
    private val volumeUpCommandFactory = mock<VolumeUpCommand.Factory>()
    private val volumeDownCommandFactory = mock<VolumeDownCommand.Factory>()
    private val screenCoordinatesProvider = ScreenCoordinatesProvider()

    private val resolver = CommandResolver(
        tapCommandFactory = tapCommandFactory,
        notificationsCommandFactory = notificationsCommandFactory,
        quickSettingsCommandFactory = quickSettingsCommandFactory,
        lockScreenCommandFactory = lockScreenCommandFactory,
        screenshotCommandFactory = screenshotCommandFactory,
        navigateHomeCommandFactory = navigateHomeCommandFactory,
        navigateRecentCommandFactory = navigateRecentCommandFactory,
        navigateBackCommandFactory = navigateBackCommandFactory,
        keyboardHideCommandFactory = keyboardHideCommandFactory,
        keyboardShowCommandFactory = keyboardShowCommandFactory,
        pickUpCommandFactory = pickUpCommandFactory,
        declineCommandFactory = declineCommandFactory,
        endCommandFactory = endCommandFactory,
        speakerCommandFactory = speakerCommandFactory,
        callCommandFactory = callCommandFactory,
        writeSMSCommandFactory = writeSMSCommandFactory,
        swipeUpCommandFactory = swipeUpCommandFactory,
        swipeDownCommandFactory = swipeDownCommandFactory,
        swipeLeftCommandFactory = swipeLeftCommandFactory,
        swipeRightCommandFactory = swipeRightCommandFactory,
        zoomInCommandFactory = zoomInCommandFactory,
        zoomOutCommandFactory = zoomOutCommandFactory,
        volumeUpCommandFactory = volumeUpCommandFactory,
        volumeDownCommandFactory = volumeDownCommandFactory,
        screenCoordinatesProvider = screenCoordinatesProvider,
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
            rawText = "Napisz do Mama"
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
    fun `createCall delegates directly to callCommandFactory`() {
        val mockCommand = mock<CallCommand>()
        whenever(callCommandFactory.create(recipient = "123456789")).thenReturn(mockCommand)

        val result = resolver.createCall("123456789")

        assertEquals(mockCommand, result)
        verify(callCommandFactory).create(recipient = "123456789")
    }

    @Test
    fun `createWriteSMS delegates directly to writeSMSCommandFactory`() {
        val mockCommand = mock<WriteSMSCommand>()
        whenever(writeSMSCommandFactory.create(recipient = "987654321")).thenReturn(mockCommand)

        val result = resolver.createWriteSMS("987654321")

        assertEquals(mockCommand, result)
        verify(writeSMSCommandFactory).create(recipient = "987654321")
    }

    @Test
    fun `resolve CELL_PRESS with valid number calculates coordinates and calls tapCommandFactory`() {
        val mockCommand = mock<TapCommand>()
        whenever(tapCommandFactory.create(any(), any())).thenReturn(mockCommand)

        val intent = VoiceIntent(
            identifier = CommandIdentifier.CELL_PRESS,
            argument = CommandArgument.Number(5),
            rawText = "Kliknij 5"
        )

        val result = resolver.resolve(intent)

        assertEquals(mockCommand, result)
        verify(tapCommandFactory).create(any(), any())
    }
}
