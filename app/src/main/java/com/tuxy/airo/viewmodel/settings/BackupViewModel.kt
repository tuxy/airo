package com.tuxy.airo.viewmodel.settings

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tuxy.airo.MainActivity
import com.tuxy.airo.R
import com.tuxy.airo.data.flightdata.FlightDataBase
import de.raphaelebner.roomdatabasebackup.core.RoomBackup

@Suppress("UNCHECKED_CAST")
class BackupViewModel(
    context: Context,
    val backup: RoomBackup
) : ViewModel() {
    val database = FlightDataBase.getDatabase(context)

    fun roomBackup(context: Context) {
        backup
            .database(database)
            .enableLogDebug(true)
            .backupIsEncrypted(false)
            .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_DIALOG)
            .customRestoreDialogTitle(context.getString(R.string.backup_now))
            .maxFileCount(10)
            .apply {
                onCompleteListener { success, message, exitCode ->
                    Log.d("Backup", "success: $success, message: $message, exitCode: $exitCode")
                    if (success) restartApp(
                        Intent(
                            context,
                            MainActivity::class.java
                        )
                    )
                }
            }
            .backup()
    }

    fun roomRestore(context: Context) {
        backup
            .database(database)
            .enableLogDebug(true)
            .backupIsEncrypted(false)
            .backupLocation(RoomBackup.BACKUP_FILE_LOCATION_CUSTOM_DIALOG)
            .customRestoreDialogTitle(context.getString(R.string.restore_now))
            .maxFileCount(10)
            .apply {
                onCompleteListener { success, message, exitCode ->
                    Log.d("Backup", "success: $success, message: $message, exitCode: $exitCode")
                    if (success) restartApp(
                        Intent(
                            context,
                            MainActivity::class.java
                        )
                    )
                }
            }
            .restore()
    }

    class Factory(
        private val context: Context,
        private val backup: RoomBackup,
    ) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BackupViewModel(context, backup) as T
        }
    }
}