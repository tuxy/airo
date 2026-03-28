package com.tuxy.airo.data.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver that reschedules flight refresh alarms after device boot.
 *
 * When the device restarts, all scheduled alarms are cleared. This receiver
 * listens for [Intent.ACTION_BOOT_COMPLETED] and reschedules the periodic
 * flight refresh using [AlarmSchedulerHelper].
 *
 * @see AlarmSchedulerHelper for the scheduling logic
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val LOG_TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(LOG_TAG, "Boot completed, rescheduling flight refresh alarms")
            AlarmSchedulerHelper.schedulePeriodicRefresh(context)
        }
    }
}