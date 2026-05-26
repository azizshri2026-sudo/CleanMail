package cleanmail.models

import kotlinx.serialization.Serializable

@Serializable
data class EmailTemplate(
    val id: String,
    val name: String,
    val subject: String,
    val bodyText: String = "",
    val bodyHtml: String = "",
    val toDefaults: List<EmailAddress> = emptyList(),
    val ccDefaults: List<EmailAddress> = emptyList()
)
