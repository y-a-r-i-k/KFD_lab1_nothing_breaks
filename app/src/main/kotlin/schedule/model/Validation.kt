package schedule.model

// true, если в строке есть хотя бы один управляющий символ (\n, \t, \r и т.п.)
fun hasControlChars(s: String): Boolean = s.any { it.isISOControl() }

// true, если строка начинается или заканчивается пробельным символом
fun hasOuterWhitespace(s: String): Boolean =
    (s.firstOrNull()?.isWhitespace() ?: false) || (s.lastOrNull()?.isWhitespace() ?: false)

fun requireValidText(value: String, fieldName: String) {
    require(value.isNotBlank()) { "$fieldName must not be blank" }
    require(!hasControlChars(value)) { "$fieldName must not contain control char" }
    require(!hasOuterWhitespace(value)) { "$fieldName must not contain outer whitespace" }
}
