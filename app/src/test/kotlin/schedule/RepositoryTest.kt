package schedule

import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test
import schedule.model.Cancelled
import schedule.model.Exam
import schedule.model.Lecture
import schedule.model.Session
import schedule.repo.AuditEvent
import schedule.repo.AuditedSessionRepository
import schedule.repo.InMemorySessionRepository
import kotlin.test.assertSame

class RepositoryTest {
    private val date = LocalDate.of(2026, 10, 5)

    @Test
    fun teachersListFromSessionCannotBeModified() {
        val exam = Exam(date, "Oral exam", "Room 401", listOf("Ivanov", "Petrov"))

        assertFailsWith<UnsupportedOperationException> {
            (exam.teachers as MutableList<String>).add("Hacker") // Меняем список полученный их Exam
        }

        assertEquals(listOf("Ivanov", "Petrov"), exam.teachers)
    }

    @Test
    fun snapshotDoesNotChangeAfterAdd() {
        val repo = InMemorySessionRepository()
        val first = Cancelled(date, "Oral exam")
        repo.add(first)

        val snapshot = repo.all()
        repo.add(Cancelled(date, "Theory test"))

        assertEquals(listOf(first), snapshot)
        assertEquals(2, repo.all().size)
    }

    @Test
    fun auditLogsSingleAndBatchAdd() {
        val repo = AuditedSessionRepository(InMemorySessionRepository())
        val first = Lecture(date, "Calculus", "Room 105", listOf("Ivanov"))
        repo.add(first)

        val second = listOf<Session>(
            Lecture(date, "Algebra", "Room 67", listOf("Ivanov", "Petrov")),
            Lecture(date, "Kotlin", "online", listOf("Nikita")),
            Lecture(date, "English", "Room 318", listOf("Antonova"))
        )
        repo.addAll(second)

        assertEquals(4, repo.all().size)
        assertEquals(4, repo.totalAdded)
        assertEquals(
            repo.auditLog,
            listOf(AuditEvent("add", 1), AuditEvent("addAll", 2))
        )

    }

    @Test
    fun emptyAddAllCreatesEventWithZero() {
        val repo = AuditedSessionRepository(InMemorySessionRepository())

        repo.addAll(emptyList())

        assertEquals(listOf(AuditEvent("addAll", 0)), repo.auditLog)
        assertEquals(0, repo.totalAdded)
        assertEquals(0, repo.all().size)
    }
}