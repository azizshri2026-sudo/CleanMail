package cleanmail.models

import kotlinx.serialization.Serializable

@Serializable
data class NotificationSettings(
    val accountId: String,
    val enabled: Boolean = true,
    val onlyInbox: Boolean = true,
    val onlyUnread: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: Int = 22,
    val quietHoursEnd: Int = 8,
    val batchIntervalMinutes: Int = 0
)
