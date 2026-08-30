package com.mdimitrov.nqueens.history.presentation

import android.content.Context
import android.text.format.DateUtils

/** The day a board was solved, written the way the device writes dates: "Aug 21". */
internal fun formatSolveDate(
    context: Context,
    millis: Long,
): String =
    DateUtils.formatDateTime(
        context,
        millis,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_NO_YEAR,
    )
