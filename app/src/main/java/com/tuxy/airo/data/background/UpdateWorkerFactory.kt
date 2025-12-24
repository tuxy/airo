package com.tuxy.airo.data.background

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters

class UpdateWorkerFactory : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            UpdateWorker::class.java.name -> {
                UpdateWorker(
                    workerParameters, appContext,
                )
            }

            else -> null
        }
    }
}