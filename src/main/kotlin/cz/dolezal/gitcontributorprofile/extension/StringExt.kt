package cz.dolezal.gitcontributorprofile.extension

import cz.dolezal.gitcontributorprofile.domain.LastDays

internal fun String.parseToLastDays(): LastDays? =
    try {
        LastDays(toInt())
    } catch (_: NumberFormatException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
