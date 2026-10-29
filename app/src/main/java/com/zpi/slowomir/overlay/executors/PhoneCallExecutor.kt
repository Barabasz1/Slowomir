package com.zpi.slowomir.overlay.executors

import android.telecom.InCallService

/**
 * Handles phone calls using the Accessibility API.
 *
 * @param service The accessibility service to use for injection.
 */
class PhoneCallExecutor(private val service: InCallService) {
    /**
     * Picks up a phone call.
     */
    fun pickUp(): Boolean {
        return true //TODO: implementation needed
    }

    /**
     * Makes a phone call.
     */
    fun call(): Boolean {
        return true //TODO: implementation needed
    }

    /**
     * Declines a phone call.
     */
    fun decline(): Boolean {
        return true // TODO: implementation needed
    }

    /**
     * Ends a phone call.
     */
    fun end(): Boolean {
        return true // TODO: implementation needed
    }

    /**
     * Turns on the speaker.
     */
    fun speakerOn(): Boolean {
        return true // TODO: implementation needed
    }

    /**
     * Writes the SMS.
     */
    fun writeSMS(): Boolean {
        return true // TODO: implementation needed
    }
}