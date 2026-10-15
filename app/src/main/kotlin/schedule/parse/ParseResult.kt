package schedule.parse

import schedule.model.Session

sealed interface ParseResult {
    val lineNumber: Long

    data class Accepted(override val lineNumber: Long, val session: Session) : ParseResult
    data class Rejected(override val lineNumber: Long, val reason: RejectReason) : ParseResult
}