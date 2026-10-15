package schedule.format

import schedule.SessionType
import schedule.model.Cancelled
import schedule.model.Exam
import schedule.model.Lecture
import schedule.model.Practice
import schedule.model.Session
import schedule.parse.ParseResult
import schedule.parse.RejectReason

private const val SEPARATOR = " | "

fun formatRejection(rejected: ParseResult.Rejected): String =
    "line ${rejected.lineNumber}: ${describe(rejected.reason)}"

fun describe(reason: RejectReason): String = when (reason) {
    RejectReason.InvalidSeparator -> "invalid field separator"
    is RejectReason.WrongFieldCount -> "got ${reason.count} fields, expected 5"
    is RejectReason.EmptyField -> "empty required field \"${reason.field.label}\""
    is RejectReason.InvalidDate -> "invalid date ${quote(reason.value)}"
    is RejectReason.UnknownType -> "unknown session type ${quote(reason.value)}"
    is RejectReason.NotEnoughExamTeachers ->
        "type \"EXAM\" requires at least 2 teachers, got ${reason.count}"
    is RejectReason.ControlCharacters ->
        "field \"${reason.field.label}\" contains control characters: ${quote(reason.value)}"
    is RejectReason.OuterWhitespace ->
        "field \"${reason.field.label}\" has leading or trailing whitespace: ${quote(reason.value)}"
    is RejectReason.InvalidCancelledField ->
        "type \"CANCELLED\" requires \"-\" in field \"${reason.field.label}\", got ${quote(reason.value)}"
    is RejectReason.MissingLocation ->
        "type ${quote(reason.typeName)} requires a location, got \"-\""
    is RejectReason.InvalidTeacher -> "invalid teacher name ${quote(reason.value)}"
    is RejectReason.DuplicateTeacher -> "duplicate teacher ${quote(reason.name)}"
}

private fun quote(value: String): String = "\"${escape(value)}\""

fun escape(value: String): String = buildString {
    for (ch in value) {
        when {
            ch == '\\' -> append("\\\\")
            ch == '"' -> append("\\\"")
            ch.code in 0x20..0x7E -> append(ch)
            else -> append("\\u%04X".format(ch.code))
        }
    }
}

fun typeOf(session: Session): SessionType = when (session) {
    is Lecture -> SessionType.LECTURE
    is Practice -> SessionType.PRACTICE
    is Exam -> SessionType.EXAM
    is Cancelled -> SessionType.CANCELLED
}

fun formatForPeriod(session: Session): String =
    listOf(session.date.toString(), typeOf(session).name, session.topic, locationOf(session))
        .joinToString(SEPARATOR)

fun formatForType(session: Session): String =
    formatForPeriod(session) + SEPARATOR + teachersOf(session)

private fun locationOf(session: Session): String = when (session) {
    is Lecture -> session.location
    is Practice -> session.location
    is Exam -> session.location
    is Cancelled -> "-"
}

private fun teachersOf(session: Session): String = when (session) {
    is Lecture -> session.teachers.joinToString(", ")
    is Practice -> session.teachers.joinToString(", ")
    is Exam -> session.teachers.joinToString(", ")
    is Cancelled -> "-"
}