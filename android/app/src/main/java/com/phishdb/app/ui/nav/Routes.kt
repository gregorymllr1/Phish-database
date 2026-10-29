package com.phishdb.app.ui.nav

object Routes {
    const val SEARCH = "search"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val SHOW = "show/{date}"
    const val SONG = "song/{name}"

    fun show(date: String) = "show/$date"
    fun song(name: String) = "song/${java.net.URLEncoder.encode(name, "UTF-8")}"
}
