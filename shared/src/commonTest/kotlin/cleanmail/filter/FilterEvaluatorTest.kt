package cleanmail.filter

import cleanmail.models.*
import kotlinx.datetime.Instant
import kotlin.test.*

class FilterEvaluatorTest {

    private fun email(
        from: String = "sender@example.com",
        fromName: String = "Sender",
        to: List<String> = listOf("me@example.com"),
        cc: List<String> = emptyList(),
        subject: String = "Hello World",
        bodyText: String = "Body text content",
        bodyHtml: String = "<p>Body text content</p>",
        attachments: List<Attachment> = emptyList()
    ) = Email(
        id = "test-id",
        accountId = "acc-1",
        folderId = "INBOX",
        uid = 1L,
        messageId = "<msg@example.com>",
        from = EmailAddress(name = fromName, address = from),
        to = to.map { EmailAddress(address = it) },
        cc = cc.map { EmailAddress(address = it) },
        subject = subject,
        bodyText = bodyText,
        bodyHtml = bodyHtml,
        attachments = attachments,
        date = Instant.fromEpochMilliseconds(0)
    )

    private fun rule(
        field: FilterConditionField,
        op: FilterConditionOp,
        value: String,
        matchAll: Boolean = true
    ) = FilterRule(
        id = "r1",
        accountId = "acc-1",
        name = "Test rule",
        conditions = listOf(FilterCondition(field, op, value)),
        matchAll = matchAll,
        action = FilterAction.MARK_READ
    )

    // --- CONTAINS ---

    @Test fun containsFrom_matches() {
        assertTrue(FilterEvaluator.matches(rule(FilterConditionField.FROM, FilterConditionOp.CONTAINS, "example"), email()))
    }

    @Test fun containsFrom_noMatch() {
        assertFalse(FilterEvaluator.matches(rule(FilterConditionField.FROM, FilterConditionOp.CONTAINS, "nowhere"), email()))
    }

    @Test fun containsSubject_caseInsensitive() {
        assertTrue(FilterEvaluator.matches(rule(FilterConditionField.SUBJECT, FilterConditionOp.CONTAINS, "HELLO"), email()))
    }

    @Test fun containsBody_matchesText() {
        assertTrue(FilterEvaluator.matches(rule(FilterConditionField.BODY, FilterConditionOp.CONTAINS, "body text"), email()))
    }

    // --- NOT_CONTAINS ---

    @Test fun notContainsSubject_matches() {
        assertTrue(FilterEvaluator.matches(rule(FilterConditionField.SUBJECT, FilterConditionOp.NOT_CONTAINS, "spam"), email()))
    }

    @Test fun notContainsSubject_noMatch() {
        assertFalse(FilterEvaluator.matches(rule(FilterConditionField.SUBJECT, FilterConditionOp.NOT_CONTAINS, "Hello"), email()))
    }

    // --- EQUALS ---

    @Test fun equalsFrom_exactAddress() {
        assertTrue(FilterEvaluator.matches(rule(FilterConditionField.FROM, FilterConditionOp.EQUALS, "sender@example.com"), email()))
    }

    @Test fun equalsFrom_caseInsensitive() {
        assertTrue(FilterEvaluator.matches(rule(FilterConditionField.FROM, FilterConditionOp.EQUALS, "SENDER@EXAMPLE.COM"), email()))
    }

    @Test fun equalsFrom_noMatch() {
        assertFalse(FilterEvaluator.matches(rule(FilterConditionField.FROM, FilterConditionOp.EQUALS, "other@example.com"), email()))
    }

    @Test fun equalsSubject_partial_noMatch() {
        // EQUALS must be exact, not partial
        assertFalse(FilterEvaluator.matches(rule(FilterConditionField.SUBJECT, FilterConditionOp.EQUALS, "Hello"), email()))
    }

    // --- STARTS_WITH ---

    @Test fun startsWith_matchesPrefix() {
        assertTrue(FilterEvaluator.matches(rule(FilterConditionField.SUBJECT, FilterConditionOp.STARTS_WITH, "Hello"), email()))
    }

    @Test fun startsWith_caseInsensitive() {
        assertTrue(FilterEvaluator.matches(rule(FilterConditionField.SUBJECT, FilterConditionOp.STARTS_WITH, "hello"), email()))
    }

    @Test fun startsWith_noMatch() {
        assertFalse(FilterEvaluator.matches(rule(FilterConditionField.SUBJECT, FilterConditionOp.STARTS_WITH, "World"), email()))
    }

    @Test fun startsWith_from_name() {
        assertTrue(FilterEvaluator.matches(rule(FilterConditionField.FROM, FilterConditionOp.STARTS_WITH, "Send"), email()))
    }

    // --- ENDS_WITH ---

    @Test fun endsWith_matchesSuffix() {
        assertTrue(FilterEvaluator.matches(rule(FilterConditionField.SUBJECT, FilterConditionOp.ENDS_WITH, "World"), email()))
    }

    @Test fun endsWith_caseInsensitive() {
        assertTrue(FilterEvaluator.matches(rule(FilterConditionField.SUBJECT, FilterConditionOp.ENDS_WITH, "world"), email()))
    }

    @Test fun endsWith_noMatch() {
        assertFalse(FilterEvaluator.matches(rule(FilterConditionField.SUBJECT, FilterConditionOp.ENDS_WITH, "Hello"), email()))
    }

    // --- REGEX ---

    @Test fun regex_simplePattern_matches() {
        assertTrue(FilterEvaluator.matches(rule(FilterConditionField.SUBJECT, FilterConditionOp.REGEX, "Hello.*World"), email()))
    }

    @Test fun regex_emailPattern_matches() {
        assertTrue(FilterEvaluator.matches(rule(FilterConditionField.FROM, FilterConditionOp.REGEX, "\\w+@example\\.com"), email()))
    }

    @Test fun regex_noMatch() {
        assertFalse(FilterEvaluator.matches(rule(FilterConditionField.SUBJECT, FilterConditionOp.REGEX, "^Spam"), email()))
    }

    @Test fun regex_invalidPattern_returnsFalse() {
        // invalid regex should not throw, just return false
        assertFalse(FilterEvaluator.matches(rule(FilterConditionField.SUBJECT, FilterConditionOp.REGEX, "[invalid("), email()))
    }

    @Test fun regex_caseInsensitive() {
        assertTrue(FilterEvaluator.matches(rule(FilterConditionField.SUBJECT, FilterConditionOp.REGEX, "hello world"), email()))
    }

    // --- Multi-condition logic ---

    @Test fun matchAll_allMustPass() {
        val r = FilterRule(
            id = "r1", accountId = "acc-1", name = "Multi",
            conditions = listOf(
                FilterCondition(FilterConditionField.SUBJECT, FilterConditionOp.CONTAINS, "Hello"),
                FilterCondition(FilterConditionField.FROM, FilterConditionOp.CONTAINS, "example")
            ),
            matchAll = true, action = FilterAction.MARK_READ
        )
        assertTrue(FilterEvaluator.matches(r, email()))
    }

    @Test fun matchAll_oneFails_noMatch() {
        val r = FilterRule(
            id = "r1", accountId = "acc-1", name = "Multi",
            conditions = listOf(
                FilterCondition(FilterConditionField.SUBJECT, FilterConditionOp.CONTAINS, "Hello"),
                FilterCondition(FilterConditionField.FROM, FilterConditionOp.CONTAINS, "nowhere")
            ),
            matchAll = true, action = FilterAction.MARK_READ
        )
        assertFalse(FilterEvaluator.matches(r, email()))
    }

    @Test fun matchAny_onePass_matches() {
        val r = FilterRule(
            id = "r1", accountId = "acc-1", name = "Multi",
            conditions = listOf(
                FilterCondition(FilterConditionField.SUBJECT, FilterConditionOp.CONTAINS, "Hello"),
                FilterCondition(FilterConditionField.FROM, FilterConditionOp.CONTAINS, "nowhere")
            ),
            matchAll = false, action = FilterAction.MARK_READ
        )
        assertTrue(FilterEvaluator.matches(r, email()))
    }

    @Test fun emptyConditions_returnsFalse() {
        val r = FilterRule(
            id = "r1", accountId = "acc-1", name = "Empty",
            conditions = emptyList(),
            matchAll = true, action = FilterAction.MARK_READ
        )
        assertFalse(FilterEvaluator.matches(r, email()))
    }

    // --- Field-specific tests ---

    @Test fun to_field_matches() {
        assertTrue(FilterEvaluator.matches(rule(FilterConditionField.TO, FilterConditionOp.CONTAINS, "me@example"), email()))
    }

    @Test fun cc_field_matches() {
        assertTrue(FilterEvaluator.matches(
            rule(FilterConditionField.CC, FilterConditionOp.CONTAINS, "cc@example"),
            email(cc = listOf("cc@example.com"))
        ))
    }

    @Test fun hasAttachment_true_matches() {
        val att = Attachment("file.pdf", "application/pdf", 1024L)
        assertTrue(FilterEvaluator.matches(
            rule(FilterConditionField.HAS_ATTACHMENT, FilterConditionOp.EQUALS, "true"),
            email(attachments = listOf(att))
        ))
    }

    @Test fun hasAttachment_false_matches() {
        assertTrue(FilterEvaluator.matches(
            rule(FilterConditionField.HAS_ATTACHMENT, FilterConditionOp.EQUALS, "false"),
            email(attachments = emptyList())
        ))
    }

    @Test fun hasAttachment_noAttachment_doesNotMatchTrue() {
        assertFalse(FilterEvaluator.matches(
            rule(FilterConditionField.HAS_ATTACHMENT, FilterConditionOp.EQUALS, "true"),
            email(attachments = emptyList())
        ))
    }
}
