package cleanmail.filter

import cleanmail.db.CleanMailDatabase
import cleanmail.models.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class FilterRepository(private val db: CleanMailDatabase) {

    fun getRules(accountId: String): List<FilterRule> =
        db.filterRuleQueries.selectAll(accountId).executeAsList().map { it.toDomain() }

    fun getEnabledRules(accountId: String): List<FilterRule> =
        db.filterRuleQueries.selectByAccount(accountId).executeAsList().map { it.toDomain() }

    fun saveRule(rule: FilterRule) {
        db.filterRuleQueries.insert(rule.toDbEntity())
    }

    fun deleteRule(id: String) {
        db.filterRuleQueries.delete(id)
    }

    fun setEnabled(id: String, enabled: Boolean) {
        db.filterRuleQueries.setEnabled(if (enabled) 1L else 0L, id)
    }
}

// --- DB mapping ---

fun cleanmail.db.Filter_rule.toDomain(): FilterRule = FilterRule(
    id = id,
    accountId = account_id,
    name = name,
    conditions = Json.decodeFromString(conditions_json),
    matchAll = match_all == 1L,
    action = FilterAction.valueOf(action),
    actionParam = action_param,
    isEnabled = is_enabled == 1L,
    priority = priority.toInt()
)

fun FilterRule.toDbEntity(): cleanmail.db.Filter_rule = cleanmail.db.Filter_rule(
    id = id,
    account_id = accountId,
    name = name,
    conditions_json = Json.encodeToString(conditions),
    match_all = if (matchAll) 1L else 0L,
    action = action.name,
    action_param = actionParam,
    is_enabled = if (isEnabled) 1L else 0L,
    priority = priority.toLong()
)
