package cleanmail.notifications

import android.content.Context
import androidx.work.*
import cleanmail.CleanMailApp
import cleanmail.db.toDomain
import cleanmail.imap.ImapSyncManager
import cleanmail.models.Account
import cleanmail.models.AuthType
import cleanmail.models.SecurityType
import java.util.concurrent.TimeUnit

class SyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val db = CleanMailApp.instance.database
        val accounts = db.accountQueries.selectAll().executeAsList()

        accounts.forEach { row ->
            val account = Account(
                id = row.id,
                displayName = row.display_name,
                emailAddress = row.email_address,
                authType = AuthType.valueOf(row.auth_type),
                imapHost = row.imap_host,
                imapPort = row.imap_port.toInt(),
                imapSecurity = SecurityType.valueOf(row.imap_security),
                smtpHost = row.smtp_host,
                smtpPort = row.smtp_port.toInt(),
                smtpSecurity = SecurityType.valueOf(row.smtp_security),
                passwordEncrypted = row.password_encrypted,
                syncIntervalMinutes = row.sync_interval_minutes.toInt()
            )

            runCatching {
                val syncManager = ImapSyncManager(account, db)
                val summary = syncManager.fullSync()
                syncManager.disconnect()

                val newEmails = summary.folders
                    .filter { it.fetched > 0 }
                    .flatMap { folderResult ->
                        val folderId = "${account.id}/${folderResult.folderPath}"
                        db.emailQueries
                            .selectUnreadByFolder(folderId)
                            .executeAsList()
                            .map { it.toDomain() }
                            .take(folderResult.fetched)
                    }

                val notifSettings = db.notificationSettingsQueries
                    .selectByAccount(account.id)
                    .executeAsOneOrNull()

                if (notifSettings?.enabled == 1L && newEmails.isNotEmpty()) {
                    MailNotificationManager.showNewMailNotification(context, newEmails, account.emailAddress)
                }
            }
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "cleanmail_sync"

        fun schedule(context: Context, intervalMinutes: Long = 15) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                intervalMinutes, TimeUnit.MINUTES,
                5, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
