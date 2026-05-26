package cleanmail.models

import kotlinx.serialization.Serializable

enum class FilterConditionField { FROM, TO, CC, SUBJECT, BODY, HAS_ATTACHMENT }
enum class FilterConditionOp { CONTAINS, NOT_CONTAINS, EQUALS, STARTS_WITH, ENDS_WITH, REGEX }
enum class FilterAction { MOVE_TO_FOLDER, MARK_READ, MARK_STARRED, DELETE, LABEL }

@Serializable
data class FilterCondition(
    val field: FilterConditionField,
    val op: FilterConditionOp,
    val value: String
)

@Serializable
data class FilterRule(
    val id: String,
    val accountId: String,
    val name: String,
    val conditions: List<FilterCondition>,
    val matchAll: Boolean = true,
    val action: FilterAction,
    val actionParam: String = "",
    val isEnabled: Boolean = true,
    val priority: Int = 0
)
