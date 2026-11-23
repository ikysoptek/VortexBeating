/**
 * Vortex Music Project (C) 2026
 */

package com.nanzbeatles.nanas.ui.screens.wrapped

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

object WrappedConstants {
    val YEAR: Int
        get() = LocalDate.now().year

    val MONTH: Int
        get() = LocalDate.now().monthValue // 1-based (1 = Januari .. 11 = November)

    val PLAYLIST_NAME: String
        get() = "Ringkasan $displayMonthYear"

    const val BG_START_SEC = 71L // 1:11

    val displayMonthYear: String
        get() = LocalDate.now()
            .month
            .getDisplayName(TextStyle.FULL, Locale("id", "ID"))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("id", "ID")) else it.toString() }
}
