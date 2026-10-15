package schedule

import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test
import schedule.model.Cancelled
import schedule.model.Exam
import schedule.model.Lecture
import schedule.model.Practice

class ModelTest {
    private val date = LocalDate.of(2026, 10, 5)

    @Test
    fun createsAllFourTypes() {
        val lecture = Lecture(date, "Classes and delegation", "Room 312", listOf("Portnov"))
        assertEquals("Room 312", lecture.location)
        assertEquals(listOf("Portnov"), lecture.teachers)

        val practice = Practice(date, "Domain modeling", "Room 312", listOf("Anokhin"))
        assertEquals(listOf("Anokhin"), practice.teachers)

        val exam = Exam(date, "Oral exam 1", "Room 401", listOf("Portnov", "Stepanova", "ivanov", "Ivanov"))
        assertEquals(listOf("Portnov", "Stepanova", "ivanov", "Ivanov"), exam.teachers)

        val cancelled = Cancelled(date, "Lecture postponed")
        assertEquals("Lecture postponed", cancelled.topic)
    }

    @Test
    fun internalSpacesAreAllowed() {
        val lecture = Lecture(date, "Many  words here", "Main building, room 1", listOf("Anna Maria"))
        assertEquals("Anna Maria", lecture.teachers.single())
    }

    @Test
    fun rejectsInvalidTopic() {
        for (topic in listOf("", "   ", " Topic", "Topic ", "To\npic", "\tTopic")) {
            assertFailsWith<IllegalArgumentException>(topic) { Lecture(date, topic, "Room", listOf("A")) }
            assertFailsWith<IllegalArgumentException>(topic) { Cancelled(date, topic) }
        }
    }

    @Test
    fun rejectsYearOutOfRange() {
        for (year in listOf(0, 10000, -1)) {
            val bad = LocalDate.of(year, 1, 1)
            assertFailsWith<IllegalArgumentException>("year $year") { Cancelled(bad, "Topic") }
            assertFailsWith<IllegalArgumentException>("year $year") { Practice(bad, "Topic", "Room", listOf("A")) }
        }

        Cancelled(LocalDate.of(1, 1, 1), "Topic")
        Cancelled(LocalDate.of(9999, 12, 31), "Topic")
    }

    @Test
    fun rejectsActiveSessionWithoutTeachers() {
        assertFailsWith<IllegalArgumentException> { Lecture(date, "Topic", "Room", emptyList()) }
        assertFailsWith<IllegalArgumentException> { Practice(date, "Topic", "Room", emptyList()) }
        assertFailsWith<IllegalArgumentException> { Exam(date, "Topic", "Room", emptyList()) }
    }

    @Test
    fun rejectsExamWithOneTeacher() {
        assertFailsWith<IllegalArgumentException> { Exam(date, "Topic", "Room", listOf("Ivanov")) }
    }

    @Test
    fun rejectsExamWithDuplicateTeachers() {
        assertFailsWith<IllegalArgumentException> { Exam(date, "Topic", "Room", listOf("Ivanov", "Ivanov")) }
        assertFailsWith<IllegalArgumentException> { Lecture(date, "Topic", "Room", listOf("A", "B", "A")) }
    }

    @Test
    fun rejectsInvalidLocationAndTeacherNames() {
        for (location in listOf("-", "", " ", " Room", "Ro\u0000om")) {
            assertFailsWith<IllegalArgumentException>(location) { Lecture(date, "Topic", location, listOf("A")) }
        }
        for (name in listOf("-", "", " ", " Ivanov", "Ivanov\t")) {
            assertFailsWith<IllegalArgumentException>(name) { Exam(date, "Topic", "Room", listOf("Petrov", name)) }
        }
    }
}