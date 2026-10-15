package schedule

import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertIs
import org.junit.jupiter.api.Test
import schedule.format.describe
import schedule.format.formatRejection
import schedule.model.Cancelled
import schedule.model.Exam
import schedule.model.Lecture
import schedule.model.Practice
import schedule.parse.Field
import schedule.parse.ParseResult
import schedule.parse.RejectReason
import schedule.parse.parseLine

class ParserTest {
    private fun reasonOf(line: String): RejectReason =
        assertIs<ParseResult.Rejected>(parseLine(1, line), line).reason

    private fun messageOf(line: String, number: Long = 7): String =
        formatRejection(assertIs<ParseResult.Rejected>(parseLine(number, line), line))

    @Test
    fun acceptsAllFourTypes() {
        val lecture = assertIs<ParseResult.Accepted>(parseLine(1, "2026-10-05|LECTURE|Classes and delegation|Room 312|Portnov"))
        assertEquals(1L, lecture.lineNumber)
        val l = assertIs<Lecture>(lecture.session)
        assertEquals(LocalDate.of(2026, 10, 5), l.date)
        assertEquals("Classes and delegation", l.topic)
        assertEquals("Room 312", l.location)
        assertEquals(listOf("Portnov"), l.teachers)

        assertIs<Practice>(assertIs<ParseResult.Accepted>(parseLine(2, "2026-10-08|PRACTICE|Domain modeling|Room 312|Anokhin")).session)

        val exam = assertIs<Exam>(assertIs<ParseResult.Accepted>(parseLine(3, "2026-10-15|EXAM|Oral exam 1|Room 401|Portnov,Stepanova")).session)
        assertEquals(listOf("Portnov", "Stepanova"), exam.teachers)

        val cancelled = assertIs<Cancelled>(assertIs<ParseResult.Accepted>(parseLine(4, "2026-10-19|CANCELLED|Lecture postponed|-|-")).session)
        assertEquals("Lecture postponed", cancelled.topic)
    }

    @Test
    fun keepsCaseAndInternalSpaces() {
        val exam = assertIs<Exam>(assertIs<ParseResult.Accepted>(parseLine(1, "0001-01-01|EXAM|A  b|Main hall|Anna Maria,anna maria")).session)
        assertEquals(listOf("Anna Maria", "anna maria"), exam.teachers)
        assertEquals(LocalDate.of(1, 1, 1), exam.date)
    }

    @Test
    fun producesRequiredMessages() {
        assertEquals("line 7: invalid date \"2026-13-45\"", messageOf("2026-13-45|LECTURE|Topic|Room 1|Ivanov"))
        assertEquals("line 7: empty required field \"date\"", messageOf("|LECTURE|Topic|Room 1|Ivanov"))
        assertEquals("line 7: invalid field separator", messageOf("2026-10-05;LECTURE;Topic;Room 1;Ivanov"))
        assertEquals("line 7: unknown session type \"SEMINAR\"", messageOf("2026-10-05|SEMINAR|Topic|Room 1|Ivanov"))
        assertEquals(
            "line 7: type \"EXAM\" requires at least 2 teachers, got 1",
            messageOf("2026-10-05|EXAM|Topic|Room 1|Ivanov"),
        )
        assertEquals("line 7: got 6 fields, expected 5", messageOf("2026-10-05|LECTURE|Topic|Room 1|Ivanov|extra"))
    }

    @Test
    fun emptyLineIsInvalidSeparator() {
        assertEquals(RejectReason.InvalidSeparator, reasonOf(""))
        assertEquals(RejectReason.InvalidSeparator, reasonOf("   "))
    }

    @Test
    fun trailingEmptyFieldsAreCounted() {
        assertEquals(RejectReason.EmptyField(Field.TEACHERS), reasonOf("2026-10-05|LECTURE|Topic|Room 1|"))
        assertEquals(RejectReason.WrongFieldCount(6), reasonOf("2026-10-05|LECTURE|Topic|Room 1|Ivanov|"))
        assertEquals(RejectReason.WrongFieldCount(4), reasonOf("2026-10-05|LECTURE|Topic|Room 1"))
        assertEquals(RejectReason.WrongFieldCount(2), reasonOf("|"))
    }

    @Test
    fun firstBlankFieldWins() {
        assertEquals(RejectReason.EmptyField(Field.TOPIC), reasonOf("2026-10-05|LECTURE| |  |Ivanov"))
        assertEquals(RejectReason.EmptyField(Field.TYPE), reasonOf("bad date|\t|Topic|Room|Ivanov"))
    }

    @Test
    fun rejectsInvalidDates() {
        for (date in listOf("0000-01-01", "2026-02-29", "2026-1-05", " 2026-10-05", "2026-10-05 ", "26-10-05", "2026/10/05", "２０２６-10-05")) {
            assertEquals(RejectReason.InvalidDate(date), reasonOf("$date|LECTURE|Topic|Room|Ivanov"), date)
        }
        assertIs<ParseResult.Accepted>(parseLine(1, "2028-02-29|LECTURE|Leap|Room|Ivanov"))
        assertIs<ParseResult.Accepted>(parseLine(1, "9999-12-31|LECTURE|Last|Room|Ivanov"))
    }

    @Test
    fun typeIsCaseSensitive() {
        assertEquals(RejectReason.UnknownType("exam"), reasonOf("2026-10-05|exam|Topic|Room|A,B"))
        assertEquals(RejectReason.UnknownType(" EXAM"), reasonOf("2026-10-05| EXAM|Topic|Room|A,B"))
    }

    @Test
    fun examCountsOnlyNonEmptyNonDashItems() {
        assertEquals(RejectReason.NotEnoughExamTeachers(1), reasonOf("2026-10-05|EXAM|Topic|Room|Ivanov,-"))
        assertEquals(RejectReason.NotEnoughExamTeachers(1), reasonOf("2026-10-05|EXAM|Topic|Room|Ivanov, ,"))
        assertEquals(RejectReason.NotEnoughExamTeachers(0), reasonOf("2026-10-05|EXAM|Topic|Room|-"))
    }

    @Test
    fun multipleDefectsGiveOneReasonByPriority() {
        assertEquals(RejectReason.InvalidDate("2026-13-45"), reasonOf("2026-13-45|SEMINAR|Topic|Room 1|Ivanov"))
        assertEquals(RejectReason.DuplicateTeacher("Ivanov"), reasonOf("2026-10-05|EXAM|Topic|Room 1|Ivanov,Ivanov"))

        assertEquals(RejectReason.InvalidSeparator, reasonOf("2026-13-45 SEMINAR"))

        assertEquals(RejectReason.WrongFieldCount(6), reasonOf("||||| "))

        assertEquals(RejectReason.EmptyField(Field.TOPIC), reasonOf("2026-13-45|SEMINAR||Room|Ivanov"))

        assertEquals(RejectReason.InvalidDate("2026-02-30"), reasonOf("2026-02-30|SEMINAR|Topic|Room|Ivanov"))

        assertEquals(RejectReason.UnknownType("SEMINAR"), reasonOf("2026-10-05|SEMINAR| Topic |-|Ivanov"))

        assertEquals(RejectReason.NotEnoughExamTeachers(1), reasonOf("2026-10-05|EXAM| Topic |-|Ivanov"))
    }

    @Test
    fun domainChecksKeepTheirOrder() {
        assertEquals(
            RejectReason.ControlCharacters(Field.LOCATION, "Ro\tom"),
            reasonOf("2026-10-05|LECTURE| Topic|Ro\tom|Ivanov"),
        )

        assertEquals(
            RejectReason.OuterWhitespace(Field.TOPIC, "Topic "),
            reasonOf("2026-10-05|LECTURE|Topic | Room|Ivanov"),
        )

        assertEquals(
            RejectReason.OuterWhitespace(Field.TEACHERS, " Petrov"),
            reasonOf("2026-10-05|LECTURE|Topic|Room|Ivanov, Petrov,Sidorov "),
        )

        assertEquals(
            RejectReason.OuterWhitespace(Field.LOCATION, "- "),
            reasonOf("2026-10-19|CANCELLED|Topic|- |Ivanov"),
        )

        assertEquals(
            RejectReason.InvalidCancelledField(Field.LOCATION, "Room 1"),
            reasonOf("2026-10-19|CANCELLED|Topic|Room 1|Ivanov"),
        )
        assertEquals(
            RejectReason.InvalidCancelledField(Field.TEACHERS, "Ivanov"),
            reasonOf("2026-10-19|CANCELLED|Topic|-|Ivanov"),
        )

        assertEquals(RejectReason.MissingLocation("PRACTICE"), reasonOf("2026-10-05|PRACTICE|Topic|-|-"))

        assertEquals(RejectReason.InvalidTeacher(""), reasonOf("2026-10-05|LECTURE|Topic|Room|A,A,,B"))
        assertEquals(RejectReason.InvalidTeacher("-"), reasonOf("2026-10-05|LECTURE|Topic|Room|-"))
        assertEquals(RejectReason.InvalidTeacher(" "), reasonOf("2026-10-05|EXAM|Topic|Room|A,B, "))

        assertEquals(RejectReason.DuplicateTeacher("B"), reasonOf("2026-10-05|LECTURE|Topic|Room|A,B,C,B,A"))
    }

    @Test
    fun carriageReturnInsideLineIsControlCharacter() {
        assertEquals(
            RejectReason.ControlCharacters(Field.TEACHERS, "Ivanov\r"),
            reasonOf("2026-10-05|LECTURE|Topic|Room|Ivanov\r"),
        )
    }

    @Test
    fun describesCustomReasons() {
        assertEquals(
            "field \"topic\" contains control characters: \"a\\u0009b\"",
            describe(RejectReason.ControlCharacters(Field.TOPIC, "a\tb")),
        )
        assertEquals(
            "field \"teachers\" has leading or trailing whitespace: \" Petrov\"",
            describe(RejectReason.OuterWhitespace(Field.TEACHERS, " Petrov")),
        )
        assertEquals(
            "type \"CANCELLED\" requires \"-\" in field \"location\", got \"Room 1\"",
            describe(RejectReason.InvalidCancelledField(Field.LOCATION, "Room 1")),
        )
        assertEquals("type \"LECTURE\" requires a location, got \"-\"", describe(RejectReason.MissingLocation("LECTURE")))
        assertEquals("invalid teacher name \"\"", describe(RejectReason.InvalidTeacher("")))
        assertEquals("duplicate teacher \"Ivanov\"", describe(RejectReason.DuplicateTeacher("Ivanov")))
    }

    @Test
    fun escapesValuesInMessages() {
        assertEquals(
            "invalid date \"a\\\\b\\\"c\\u000A\\u0434\\u007F\"",
            describe(RejectReason.InvalidDate("a\\b\"c\nд\u007F")),
        )

        assertEquals("unknown session type \"\\uD83D\\uDE00\"", describe(RejectReason.UnknownType("😀")))

        val message = messageOf("2026-10-05|Лекция\n|Topic|Room|Ivanov")
        assertEquals("line 7: unknown session type \"\\u041B\\u0435\\u043A\\u0446\\u0438\\u044F\\u000A\"", message)
    }

    @Test
    fun lineNumberIsKept() {
        assertEquals(42L, parseLine(42, "").lineNumber)
        assertEquals("line 3000000000: invalid field separator", messageOf("", 3_000_000_000))
    }
}