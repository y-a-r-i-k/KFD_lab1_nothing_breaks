package schedule

import java.time.LocalDate
import schedule.format.formatForPeriod
import schedule.format.formatForType
import schedule.format.formatRejection
import schedule.format.typeOf
import schedule.parse.ParseResult
import schedule.parse.parseLine

interface CommandHandlers {
    fun period(command: Command.Period): Int
    fun byType(command: Command.ByType): Int
    fun errors(command: Command.Errors): Int
}

object StudentCommandHandlers : CommandHandlers {
    override fun period(command: Command.Period): Int = runPeriod(command)
    override fun byType(command: Command.ByType): Int = runType(command)
    override fun errors(command: Command.Errors): Int = runErrors(command)
}

private fun exitCode(hasRejected: Boolean): Int = if (hasRejected) 1 else 0

private fun LocalDate.isWithin(from: LocalDate, to: LocalDate): Boolean =
    !isBefore(from) && !isAfter(to)

fun runPeriod(command: Command.Period): Int {
    var total = 0
    var hasRejected = false
    forEachScheduleLine(command.file) { number, line ->
        when (val result = parseLine(number, line)) {
            is ParseResult.Accepted ->
                if (result.session.date.isWithin(command.from, command.to)) {
                    println(formatForPeriod(result.session))
                    total += 1
                }
            is ParseResult.Rejected -> hasRejected = true
        }
    }
    println("Total: $total")
    return exitCode(hasRejected)
}

fun runType(command: Command.ByType): Int {
    var total = 0
    var hasRejected = false
    forEachScheduleLine(command.file) { number, line ->
        when (val result = parseLine(number, line)) {
            is ParseResult.Accepted ->
                if (typeOf(result.session) == command.type) {
                    println(formatForType(result.session))
                    total += 1
                }
            is ParseResult.Rejected -> hasRejected = true
        }
    }
    println("Total: $total")
    return exitCode(hasRejected)
}

fun runErrors(command: Command.Errors): Int {
    var lines = 0L
    var rejected = 0L
    forEachScheduleLine(command.file) { number, line ->
        lines = number
        val result = parseLine(number, line)
        if (result is ParseResult.Rejected) {
            println(formatRejection(result))
            rejected += 1
        }
    }
    println("Rejected: $rejected of $lines")
    return exitCode(rejected > 0)
}