package com.mdimitrov.nqueens.history.presentation

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import java.util.Date
import java.util.Locale

/** When a board was solved, as short as the device writes it: "Aug 21, 14:32". */
internal fun formatSolveDate(
    context: Context,
    millis: Long,
): String =
    DateUtils.formatDateTime(
        context,
        millis,
        DateUtils.FORMAT_SHOW_DATE or
            DateUtils.FORMAT_SHOW_TIME or
            DateUtils.FORMAT_ABBREV_MONTH or
            DateUtils.FORMAT_NO_YEAR,
    )

/**
 * The same moment down to the second, for the label a screen reader reads out. Two equally fast
 * solves of one board within a minute of each other are ordinary — solve, play again, solve — and
 * a delete button has to name which of them it removes. It is not drawn, so its length costs
 * nothing on screen.
 */
internal fun formatSolveMoment(
    context: Context,
    millis: Long,
): String {
    val hours = if (DateFormat.is24HourFormat(context)) "H" else "h"
    val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "MMMd${hours}mmss")

    return DateFormat.format(pattern, Date(millis)).toString()
}
