package schedule.repo

import schedule.model.Session


interface SessionRepository {
    fun add(session: Session)
    fun addAll(sessions: List<Session>)
    fun all(): List<Session>
}

class InMemorySessionRepository : SessionRepository {
    private val sessions = mutableListOf<Session>()

    override fun add(session: Session) {
        this.sessions.add(session)
    }

    override fun addAll(sessions: List<Session>) {
        this.sessions.addAll(sessions)
    }

    override fun all(): List<Session> {
        return this.sessions.toList()
    }
}

data class AuditEvent(
    val operation: String,
    val count: Int,
)

class AuditedSessionRepository(private val delegate: SessionRepository) : SessionRepository by delegate {

    private val events = mutableListOf<AuditEvent>()

    var totalAdded: Int = 0
        private set

    val auditLog: List<AuditEvent>
        get() = events.toList()

    override fun add(session: Session) {
        delegate.add(session)
        this.events.add(AuditEvent("add", 1))
        totalAdded++
    }

    override fun addAll(sessions: List<Session>) {
        delegate.addAll(sessions)
        this.events.add(AuditEvent("addAll", sessions.size))
        totalAdded += sessions.size
    }
}