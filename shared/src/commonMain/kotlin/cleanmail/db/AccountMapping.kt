package cleanmail.db

import cleanmail.models.Account as DomainAccount
import cleanmail.models.ProxyConfig
import cleanmail.models.ProxyType

fun DomainAccount.toDbEntity(): cleanmail.db.Account = cleanmail.db.Account(
    id = id,
    display_name = displayName,
    email_address = emailAddress,
    auth_type = authType.name,
    imap_host = imapHost,
    imap_port = imapPort.toLong(),
    imap_security = imapSecurity.name,
    smtp_host = smtpHost,
    smtp_port = smtpPort.toLong(),
    smtp_security = smtpSecurity.name,
    username_encrypted = usernameEncrypted,
    password_encrypted = passwordEncrypted,
    oauth_token_encrypted = oauthTokenEncrypted,
    oauth_refresh_token_encrypted = oauthRefreshTokenEncrypted,
    proxy_type = proxy.type.name,
    proxy_host = proxy.host,
    proxy_port = proxy.port.toLong(),
    proxy_username = proxy.username,
    proxy_password_encrypted = proxy.passwordEncrypted,
    proxy_force_dns = if (proxy.forceDnsThroughProxy) 1L else 0L,
    sync_interval_minutes = syncIntervalMinutes.toLong(),
    is_active = if (isActive) 1L else 0L
)

fun cleanmail.db.Account.toDomain(): DomainAccount = DomainAccount(
    id = id,
    displayName = display_name,
    emailAddress = email_address,
    authType = cleanmail.models.AuthType.valueOf(auth_type),
    imapHost = imap_host,
    imapPort = imap_port.toInt(),
    imapSecurity = cleanmail.models.SecurityType.valueOf(imap_security),
    smtpHost = smtp_host,
    smtpPort = smtp_port.toInt(),
    smtpSecurity = cleanmail.models.SecurityType.valueOf(smtp_security),
    usernameEncrypted = username_encrypted,
    passwordEncrypted = password_encrypted,
    oauthTokenEncrypted = oauth_token_encrypted,
    oauthRefreshTokenEncrypted = oauth_refresh_token_encrypted,
    proxy = ProxyConfig(
        type = ProxyType.valueOf(proxy_type),
        host = proxy_host,
        port = proxy_port.toInt(),
        username = proxy_username,
        passwordEncrypted = proxy_password_encrypted,
        forceDnsThroughProxy = proxy_force_dns == 1L
    ),
    syncIntervalMinutes = sync_interval_minutes.toInt(),
    isActive = is_active == 1L
)
