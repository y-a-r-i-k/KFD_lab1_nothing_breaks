package schedule.model

import java.time.LocalDate

interface Session {
    val date: LocalDate;
    val topic: String
}

class Lecture(override val date: LocalDate, override val topic: String, location: String, teachers: List<String>) :
    Session {

    init {
        require(date.year in 1..9999) { "Year must be between 1 and 9999, got ${date.year}" }

        requireValidText(topic, "topic")
        requireValidText(location, "location")

        require(location != "-") { "Location must not be '-'" }

        require(teachers.isNotEmpty()) { "Teachers must not be empty" }

        for (teacher in teachers) {
            requireValidText(teacher, "teacher")
            require(teacher != "-") { "Teacher must not be '-'" }
        }

        require(teachers.toSet().size == teachers.size) {
            "Teachers must not contain duplicates: ${teachers}"
        }
    }
}

class Practice(date: LocalDate, topic: String, location: String, teachers: List<String>)
class Exam(date: LocalDate, topic: String, location: String, teachers: List<String>)

class Cancelled(override val date: LocalDate, override val topic: String) : Session {
    init {
        require(date.year in 1..9999) { "Year must be between 1 and 9999, got ${date.year}" }

        requireValidText(topic, "topic")
    }
}

