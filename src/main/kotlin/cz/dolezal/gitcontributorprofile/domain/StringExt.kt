package cz.dolezal.gitcontributorprofile.domain

internal fun String.parseToLastDays(): LastDays? =
    try {
        LastDays(toInt())
    } catch (_: NumberFormatException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
