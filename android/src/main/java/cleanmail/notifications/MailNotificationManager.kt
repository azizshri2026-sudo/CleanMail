package cleanmail.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import cleanmail.MainActivity
import cleanmail.models.Email

object MailNotificationManager {

    private const val CHANNEL_ID   = "cleanmail_new_mail"
    private const val CHANNEL_NAME = "New mail"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for new incoming emails"
            // No sound/vibration override — system defaults
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun showNewMailNotification(context: Context, emails: List<Email>, accountEmail: String) {
        if (emails.isEmpty()) return

        val nm = context.getSystemService(NotificationManager::class.java)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = if (emails.size == 1) {
            val email = emails.first()
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle(email.from.name.ifBlank { email.from.address })
                .setContentText(email.subject)
                .setStyle(NotificationCompat.BigTextStyle().bigText(email.bodyText.take(200)))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
        } else {
            val inbox = NotificationCompat.InboxStyle()
            emails.take(5).forEach { inbox.addLine("${it.from.name.ifBlank { it.from.address }}: ${it.subject}") }
            if (emails.size > 5) inbox.setSummaryText("+${emails.size - 5} more")

            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentTitle("$accountEmail — ${emails.size} new messages")
                .setStyle(inbox)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
        }

        nm.notify(accountEmail.hashCode(), notification)
    }
}
