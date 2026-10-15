package schedule.parse

enum class Field(val label: String) {
    DATE("date"),
    TYPE("type"),
    TOPIC("topic"),
    LOCATION("location"),
    TEACHERS("teachers"),
}

sealed interface RejectReason {
    data object InvalidSeparator : RejectReason
    data class WrongFieldCount(val count: Int) : RejectReason
    data class EmptyField(val field: Field) : RejectReason
    data class InvalidDate(val value: String) : RejectReason
    data class UnknownType(val value: String) : RejectReason
    data class NotEnoughExamTeachers(val count: Int) : RejectReason
    data class ControlCharacters(val field: Field, val value: String) : RejectReason
    data class OuterWhitespace(val field: Field, val value: String) : RejectReason
    data class InvalidCancelledField(val field: Field, val value: String) : RejectReason
    data class MissingLocation(val typeName: String) : RejectReason
    data class InvalidTeacher(val value: String) : RejectReason
    data class DuplicateTeacher(val name: String) : RejectReason
}