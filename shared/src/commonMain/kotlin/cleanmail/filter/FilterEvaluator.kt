package cleanmail.filter

import cleanmail.models.*

object FilterEvaluator {

    fun matches(rule: FilterRule, email: Email): Boolean {
        if (rule.conditions.isEmpty()) return false
        return if (rule.matchAll)
            rule.conditions.all { evaluate(it, email) }
        else
            rule.conditions.any { evaluate(it, email) }
    }

    private fun evaluate(condition: FilterCondition, email: Email): Boolean {
        val value = condition.value
        val field = fieldValue(condition.field, email)
        return when (condition.op) {
            FilterConditionOp.CONTAINS     -> field.any { it.contains(value, ignoreCase = true) }
            FilterConditionOp.NOT_CONTAINS -> field.none { it.contains(value, ignoreCase = true) }
            FilterConditionOp.EQUALS       -> field.any { it.equals(value, ignoreCase = true) }
            FilterConditionOp.STARTS_WITH  -> field.any { it.startsWith(value, ignoreCase = true) }
            FilterConditionOp.ENDS_WITH    -> field.any { it.endsWith(value, ignoreCase = true) }
            FilterConditionOp.REGEX        -> runCatching {
                val regex = Regex(value, RegexOption.IGNORE_CASE)
                field.any { regex.containsMatchIn(it) }
            }.getOrDefault(false)
        }
    }

    private fun fieldValue(field: FilterConditionField, email: Email): List<String> = when (field) {
        FilterConditionField.FROM         -> listOf(email.from.address, email.from.name)
        FilterConditionField.TO           -> email.to.flatMap { listOf(it.address, it.name) }
        FilterConditionField.CC           -> email.cc.flatMap { listOf(it.address, it.name) }
        FilterConditionField.SUBJECT      -> listOf(email.subject)
        FilterConditionField.BODY         -> listOf(email.bodyText, email.bodyHtml)
        FilterConditionField.HAS_ATTACHMENT -> listOf(email.attachments.isNotEmpty().toString())
    }
}
