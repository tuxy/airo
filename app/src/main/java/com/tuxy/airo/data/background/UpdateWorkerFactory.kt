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
                // Return an instance of your worker
                UpdateWorker(workerParameters, appContext)
            }
            // Add other workers here if needed
            else -> null // Fallback to the default WorkerFactory
        }
    }
}