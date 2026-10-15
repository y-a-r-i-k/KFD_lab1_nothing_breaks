package schedule.parse

import java.time.LocalDate
import java.time.format.DateTimeParseException
import schedule.SessionType
import schedule.model.Cancelled
import schedule.model.Exam
import schedule.model.Lecture
import schedule.model.Practice
import schedule.model.hasControlChars
import schedule.model.hasOuterWhitespace

private const val FIELD_COUNT = 5

private const val NO_VALUE = "-"

private val DATE_SHAPE = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")

fun parseLine(lineNumber: Long, line: String): ParseResult {
    fun reject(reason: RejectReason) = ParseResult.Rejected(lineNumber, reason)

    if ('|' !in line) return reject(RejectReason.InvalidSeparator)

    val fields = line.split('|')
    if (fields.size != FIELD_COUNT) return reject(RejectReason.WrongFieldCount(fields.size))

    val emptyIndex = fields.indexOfFirst { it.isBlank() }
    if (emptyIndex >= 0) return reject(RejectReason.EmptyField(Field.entries[emptyIndex]))

    val (dateText, typeText, topic, location, teachersText) = fields

    val date = parseDate(dateText) ?: return reject(RejectReason.InvalidDate(dateText))

    val type = SessionType.entries.firstOrNull { it.name == typeText }
        ?: return reject(RejectReason.UnknownType(typeText))

    val teachers = teachersText.split(',')

    if (type == SessionType.EXAM) {
        val count = teachers.count { it.isNotBlank() && it != NO_VALUE }
        if (count < 2) return reject(RejectReason.NotEnoughExamTeachers(count))
    }

    findDomainViolation(type, topic, location, teachersText, teachers)?.let { return reject(it) }

    val session = when (type) {
        SessionType.LECTURE -> Lecture(date, topic, location, teachers)
        SessionType.PRACTICE -> Practice(date, topic, location, teachers)
        SessionType.EXAM -> Exam(date, topic, location, teachers)
        SessionType.CANCELLED -> Cancelled(date, topic)
    }
    return ParseResult.Accepted(lineNumber, session)
}

private fun parseDate(text: String): LocalDate? {
    if (!DATE_SHAPE.matches(text)) return null
    val date = try {
        LocalDate.parse(text)
    } catch (_: DateTimeParseException) {
        return null
    }
    return if (date.year in 1..9999) date else null
}

private fun findDomainViolation(
    type: SessionType,
    topic: String,
    location: String,
    teachersText: String,
    teachers: List<String>,
): RejectReason? {
    if (hasControlChars(topic)) return RejectReason.ControlCharacters(Field.TOPIC, topic)
    if (hasControlChars(location)) return RejectReason.ControlCharacters(Field.LOCATION, location)
    teachers.firstOrNull { hasControlChars(it) }
        ?.let { return RejectReason.ControlCharacters(Field.TEACHERS, it) }

    if (hasOuterWhitespace(topic)) return RejectReason.OuterWhitespace(Field.TOPIC, topic)
    if (hasOuterWhitespace(location)) return RejectReason.OuterWhitespace(Field.LOCATION, location)
    teachers.firstOrNull { it.isNotBlank() && hasOuterWhitespace(it) }
        ?.let { return RejectReason.OuterWhitespace(Field.TEACHERS, it) }

    if (type == SessionType.CANCELLED) {
        if (location != NO_VALUE) return RejectReason.InvalidCancelledField(Field.LOCATION, location)
        if (teachersText != NO_VALUE) return RejectReason.InvalidCancelledField(Field.TEACHERS, teachersText)
        return null
    }

    if (location == NO_VALUE) return RejectReason.MissingLocation(type.name)

    teachers.firstOrNull { it.isBlank() || it == NO_VALUE }
        ?.let { return RejectReason.InvalidTeacher(it) }

    val seen = HashSet<String>()
    teachers.firstOrNull { !seen.add(it) }
        ?.let { return RejectReason.DuplicateTeacher(it) }

    return null
}