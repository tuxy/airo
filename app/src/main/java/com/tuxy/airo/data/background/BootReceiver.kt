package com.tuxy.airo.data.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Re-creates the reminder schedule after a reboot or an app update.
 *
 * The periodic refresh itself is owned by WorkManager and survives reboots, so
 * this receiver only has to restore exact alarms (which the system clears) and
 * enqueue a prompt refresh.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in HANDLED_ACTIONS) return

        Log.d(LOG_TAG, "Received $action; restoring reminders and enqueueing refresh")
        FlightWorkScheduler.enqueueRescheduleReminders(context)
        FlightWorkScheduler.enqueueOneTime(context, delaySeconds = BOOT_REFRESH_DELAY_SECONDS)
    }

    private companion object {
        const val LOG_TAG = "BootReceiver"
        const val BOOT_REFRESH_DELAY_SECONDS = 60L
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
        )
    }
}
