package cleanmail.smtp

import cleanmail.imap.ProxySocketHelper
import cleanmail.models.Account
import cleanmail.models.SecurityType
import jakarta.mail.*
import jakarta.mail.internet.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties

actual class SmtpClient actual constructor(private val account: Account) {

    private var transport: Transport? = null
    private var session: Session? = null

    actual val isConnected: Boolean
        get() = transport?.isConnected == true

    actual suspend fun connect(): SmtpResult<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val props = buildProperties()
            val s = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication() =
                    PasswordAuthentication(account.emailAddress, decryptPassword())
            })
            session = s
            val t = s.getTransport(smtpProtocol())
            t.connect(account.smtpHost, account.smtpPort, account.emailAddress, decryptPassword())
            transport = t
        }.toSmtpResult()
    }

    actual suspend fun disconnect() = withContext(Dispatchers.IO) {
        runCatching { transport?.close() }
        transport = null
        session = null
    }

    actual suspend fun sendMessage(message: OutgoingMessage): SmtpResult<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val s = session ?: run {
                connect()
                session ?: error("Failed to create session")
            }
            if (!isConnected) {
                transport?.connect(account.smtpHost, account.smtpPort, account.emailAddress, decryptPassword())
            }
            val mime = buildMimeMessage(s, message)
            val recipients = (message.to + message.cc + message.bcc)
                .map { InternetAddress(it.address, it.name, "UTF-8") }
                .toTypedArray()
            transport!!.sendMessage(mime, recipients)
        }.toSmtpResult()
    }

    actual suspend fun testConnection(): SmtpResult<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            connect().let { if (it is SmtpResult.Failure) throw Exception(it.error.toString()) }
            disconnect()
        }.toSmtpResult()
    }

    private fun smtpProtocol() = if (account.smtpSecurity == SecurityType.SSL_TLS) "smtps" else "smtp"
    private fun decryptPassword() = account.passwordEncrypted

    private fun buildProperties(): Properties = Properties().apply {
        val proto = smtpProtocol()
        put("mail.$proto.host", account.smtpHost)
        put("mail.$proto.port", account.smtpPort.toString())
        put("mail.$proto.auth", "true")
        put("mail.$proto.ssl.enable", (account.smtpSecurity == SecurityType.SSL_TLS).toString())
        put("mail.$proto.starttls.enable", (account.smtpSecurity == SecurityType.STARTTLS).toString())
        put("mail.$proto.starttls.required", (account.smtpSecurity == SecurityType.STARTTLS).toString())
        put("mail.$proto.connectiontimeout", "15000")
        put("mail.$proto.timeout", "15000")
        put("mail.$proto.writetimeout", "15000")
        put("mail.$proto.ssl.protocols", "TLSv1.2 TLSv1.3")
        ProxySocketHelper.proxyProperties(account.proxy).forEach { (k, v) ->
            put(k.replace("imap", proto), v)
        }
    }

    private fun buildMimeMessage(session: Session, msg: OutgoingMessage): MimeMessage {
        val mime = MimeMessage(session)
        mime.setFrom(InternetAddress(msg.from.address, msg.from.name, "UTF-8"))

        if (msg.replyTo.isNotEmpty()) {
            mime.replyTo = msg.replyTo.map { InternetAddress(it.address, it.name, "UTF-8") }.toTypedArray()
        }

        msg.to.forEach { mime.addRecipient(Message.RecipientType.TO, InternetAddress(it.address, it.name, "UTF-8")) }
        msg.cc.forEach { mime.addRecipient(Message.RecipientType.CC, InternetAddress(it.address, it.name, "UTF-8")) }
        msg.bcc.forEach { mime.addRecipient(Message.RecipientType.BCC, InternetAddress(it.address, it.name, "UTF-8")) }

        mime.setSubject(msg.subject, "UTF-8")

        if (msg.inReplyTo.isNotEmpty()) {
            mime.setHeader("In-Reply-To", msg.inReplyTo)
            mime.setHeader("References", (msg.references + msg.inReplyTo).joinToString(" "))
        }

        if (msg.attachments.isEmpty()) {
            mime.setBodyContent(msg)
        } else {
            val multipart = MimeMultipart("mixed")
            val bodyPart = MimeBodyPart().also { it.setBodyContent(msg) }
            multipart.addBodyPart(bodyPart)
            msg.attachments.forEach { att ->
                val part = MimeBodyPart()
                part.setContent(att.data, att.mimeType)
                part.fileName = MimeUtility.encodeText(att.filename, "UTF-8", "B")
                part.disposition = Part.ATTACHMENT
                multipart.addBodyPart(part)
            }
            mime.setContent(multipart)
        }

        mime.saveChanges()
        return mime
    }

    private fun MimePart.setBodyContent(msg: OutgoingMessage) {
        when {
            msg.bodyHtml.isNotEmpty() && msg.bodyText.isNotEmpty() -> {
                val alt = MimeMultipart("alternative")
                alt.addBodyPart(MimeBodyPart().apply { setText(msg.bodyText, "UTF-8", "plain") })
                alt.addBodyPart(MimeBodyPart().apply { setText(msg.bodyHtml, "UTF-8", "html") })
                setContent(alt)
            }
            msg.bodyHtml.isNotEmpty() -> setText(msg.bodyHtml, "UTF-8", "html")
            else -> setText(msg.bodyText, "UTF-8", "plain")
        }
    }
}

private fun <T> Result<T>.toSmtpResult(): SmtpResult<T> = fold(
    onSuccess = { SmtpResult.Success(it) },
    onFailure = { e ->
        val msg = e.message ?: ""
        SmtpResult.Failure(when {
            msg.contains("authentication", ignoreCase = true) ||
            msg.contains("535") -> SmtpError.AuthFailed(msg)
            msg.contains("SSL", ignoreCase = true) ||
            msg.contains("TLS", ignoreCase = true) -> SmtpError.SslError(msg)
            msg.contains("timeout", ignoreCase = true) -> SmtpError.Timeout(msg)
            msg.contains("552") ||
            msg.contains("message size", ignoreCase = true) -> SmtpError.MessageTooLarge(msg)
            msg.contains("550") || msg.contains("551") -> SmtpError.RecipientRejected("", msg)
            msg.contains("connect", ignoreCase = true) -> SmtpError.ConnectionFailed(msg)
            else -> SmtpError.Unknown(msg)
        })
    }
)
