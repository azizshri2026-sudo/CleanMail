package cleanmail.filter

import cleanmail.db.CleanMailDatabase
import cleanmail.imap.ImapClient
import cleanmail.models.Email
import cleanmail.models.FilterRule

class FilterEngine(
    private val db: CleanMailDatabase,
    private val imapClient: ImapClient
) {
    private val executor = FilterActionExecutor(db, imapClient)

    /**
     * Apply all enabled rules (sorted by priority) to a single email.
     * Returns the list of rules that matched.
     */
    suspend fun applyRules(email: Email): List<FilterRule> {
        val rules = db.filterRuleQueries
            .selectByAccount(email.accountId)
            .executeAsList()
            .map { it.toDomain() }

        val matched = mutableListOf<FilterRule>()
        for (rule in rules) {
            if (FilterEvaluator.matches(rule, email)) {
                executor.execute(rule, email)
                matched.add(rule)
            }
        }
        return matched
    }

    /**
     * Apply rules to a batch of incoming emails (e.g. after a folder sync).
     */
    suspend fun applyRulesBatch(emails: List<Email>): Map<String, List<FilterRule>> {
        val rules = emails.firstOrNull()?.let { email ->
            db.filterRuleQueries
                .selectByAccount(email.accountId)
                .executeAsList()
                .map { it.toDomain() }
        } ?: return emptyMap()

        return emails.associate { email ->
            val matched = mutableListOf<FilterRule>()
            for (rule in rules) {
                if (FilterEvaluator.matches(rule, email)) {
                    executor.execute(rule, email)
                    matched.add(rule)
                }
            }
            email.id to matched
        }
    }

    /**
     * Preview which emails would match a rule — without executing any action.
     */
    fun previewRule(rule: FilterRule, emails: List<Email>): List<Email> =
        emails.filter { FilterEvaluator.matches(rule, it) }
}
