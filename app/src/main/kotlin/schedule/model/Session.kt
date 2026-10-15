package schedule.model

import java.time.LocalDate

sealed interface Session {
    val date: LocalDate
    val topic: String
}

class Lecture(override val date: LocalDate, override val topic: String, val location: String, teachers: List<String>) :
    Session {

    val teachers: List<String> = java.util.List.copyOf(teachers)

    init {
        require(date.year in 1..9999) { "Year must be between 1 and 9999, got ${date.year}" }
        requireValidText(topic, "topic")
        requireValidActiveSession(location, this.teachers)
    }
}

class Practice(override val date: LocalDate, override val topic: String, val location: String, teachers: List<String>) :
    Session {

    val teachers: List<String> = java.util.List.copyOf(teachers)

    init {
        require(date.year in 1..9999) { "Year must be between 1 and 9999, got ${date.year}" }
        requireValidText(topic, "topic")
        requireValidActiveSession(location, this.teachers)
    }
}

class Exam(override val date: LocalDate, override val topic: String, val location: String, teachers: List<String>) : Session {
    val teachers: List<String> = java.util.List.copyOf(teachers)

    init {
        require(date.year in 1..9999) { "Year must be between 1 and 9999, got ${date.year}" }
        requireValidText(topic, "topic")
        requireValidActiveSession(location, this.teachers, 2)
    }
}

class Cancelled(override val date: LocalDate, override val topic: String) : Session {
    init {
        require(date.year in 1..9999) { "Year must be between 1 and 9999, got ${date.year}" }

        requireValidText(topic, "topic")
    }
}

