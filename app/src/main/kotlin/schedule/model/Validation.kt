package schedule.model

// true, если в строке есть хотя бы один управляющий символ (\n, \t, \r и т.п.)
fun hasControlChars(s: String): Boolean = s.any { it.isISOControl() }

// true, если строка начинается или заканчивается пробельным символом
fun hasOuterWhitespace(s: String): Boolean =
    (s.firstOrNull()?.isWhitespace() ?: false) || (s.lastOrNull()?.isWhitespace() ?: false)

// Бросает IllegalArgumentException, если value пустая, содержит управляющие
// символы или начинается/заканчивается пробельным символом
fun requireValidText(value: String, fieldName: String) {
    require(value.isNotBlank()) { "$fieldName must not be blank" }
    require(!hasControlChars(value)) { "$fieldName must not contain control char" }
    require(!hasOuterWhitespace(value)) { "$fieldName must not contain outer whitespace" }
}

fun requireValidActiveSession(location: String, teachers: List<String>, minTeachers: Int = 0) {
    requireValidText(location, "location")

    require(location != "-") { "Location must not be '-'" }

    require(teachers.isNotEmpty()) { "Teachers must not be empty" }
    require(teachers.size >= minTeachers) { "Teachers must not be less than $minTeachers teachers" }

    for (teacher in teachers) {
        requireValidText(teacher, "teacher")
        require(teacher != "-") { "Teacher must not be '-'" }
    }

    require(teachers.toSet().size == teachers.size) {
        "Teachers must not contain duplicates: ${teachers}"
    }
}