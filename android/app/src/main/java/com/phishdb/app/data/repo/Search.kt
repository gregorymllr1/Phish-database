package com.phishdb.app.data.repo

data class SearchFilters(
    val query: String? = null,
    val resultType: ResultType? = null,
    val song: String? = null,
    val tour: String? = null,
    val venue: String? = null,
    val date: String? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val year: Int? = null,
    val state: String? = null,
    val city: String? = null,
    val limit: Int = 50,
) {
    fun hasStructuredFilters(): Boolean =
        !song.isNullOrBlank() || !tour.isNullOrBlank() || !venue.isNullOrBlank() ||
            !date.isNullOrBlank() || !dateFrom.isNullOrBlank() || !dateTo.isNullOrBlank() ||
            year != null || !state.isNullOrBlank() || !city.isNullOrBlank()

    fun hasAny(): Boolean = !query.isNullOrBlank() || hasStructuredFilters()
}

enum class ResultType { SHOW, SONG, SONG_PERFORMANCE }

sealed interface SearchResult {
    val entityId: String
    val title: String
    val subtitle: String
    val showdate: String?

    data class Show(
        override val entityId: String,
        override val title: String,
        override val subtitle: String,
        override val showdate: String,
    ) : SearchResult

    data class Performance(
        override val entityId: String,
        override val title: String,
        override val subtitle: String,
        override val showdate: String,
        val song: String,
    ) : SearchResult

    data class Song(
        override val entityId: String,
        override val title: String,
        override val subtitle: String,
    ) : SearchResult {
        override val showdate: String? = null
    }
}

object SetLabels {
    val map = mapOf(
        "1" to "Set 1",
        "2" to "Set 2",
        "3" to "Set 3",
        "4" to "Set 4",
        "e" to "Encore",
        "e2" to "Encore 2",
        "e3" to "Encore 3",
    )

    val order = mapOf("1" to 1, "2" to 2, "3" to 3, "4" to 4, "e" to 5, "e2" to 6, "e3" to 7)

    fun label(key: String?): String = map[key] ?: "Set ${key ?: "?"}"
}

fun formatDuration(seconds: Int?): String {
    if (seconds == null || seconds <= 0) return ""
    val m = seconds / 60
    val s = seconds % 60
    if (m >= 60) {
        val h = m / 60
        val mm = m % 60
        return "%d:%02d:%02d".format(h, mm, s)
    }
    return "%d:%02d".format(m, s)
}
