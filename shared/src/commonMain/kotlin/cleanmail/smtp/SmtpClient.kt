package cleanmail.smtp

import cleanmail.models.Account

expect class SmtpClient(account: Account) {

    suspend fun connect(): SmtpResult<Unit>

    suspend fun disconnect()

    suspend fun sendMessage(message: OutgoingMessage): SmtpResult<Unit>

    /**
     * Verify credentials without sending — connects and authenticates only.
     */
    suspend fun testConnection(): SmtpResult<Unit>

    val isConnected: Boolean
}
