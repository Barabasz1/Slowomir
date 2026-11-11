package com.zpi.slowomir.voice.intent

import org.junit.Assert.assertEquals
import org.junit.Test

class CommandIntentLabelsTest {

    @Test
    fun `every model label maps to a CommandIdentifier`() {
        // Throws with the list of unmatched labels if the enum and the model drift apart.
        assertEquals(51, CommandIntentLabels.INTENTS.size)
    }
}
