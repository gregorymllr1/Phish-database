package com.phishdb.app.data.api

import com.phishdb.app.data.model.ApiEnvelope
import com.phishdb.app.data.model.SetlistEntryDto
import com.phishdb.app.data.model.ShowDto
import com.phishdb.app.data.model.SongDto
import com.phishdb.app.data.model.VenueDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PhishApi {
    @GET("shows/artist/phish.json")
    suspend fun shows(@Query("apikey") apiKey: String): ApiEnvelope<ShowDto>

    @GET("setlists/showyear/{year}.json")
    suspend fun setlistsForYear(
        @Path("year") year: Int,
        @Query("apikey") apiKey: String,
    ): ApiEnvelope<SetlistEntryDto>

    @GET("setlists/showdate/{date}.json")
    suspend fun setlistForDate(
        @Path("date") date: String,
        @Query("apikey") apiKey: String,
    ): ApiEnvelope<SetlistEntryDto>

    @GET("shows/showdate/{date}.json")
    suspend fun showForDate(
        @Path("date") date: String,
        @Query("apikey") apiKey: String,
    ): ApiEnvelope<ShowDto>

    @GET("songs.json")
    suspend fun songs(@Query("apikey") apiKey: String): ApiEnvelope<SongDto>

    @GET("venues.json")
    suspend fun venues(@Query("apikey") apiKey: String): ApiEnvelope<VenueDto>
}

object PhishApiError {
    fun message(envelope: ApiEnvelope<*>): String? =
        if (envelope.errorCode != null && envelope.errorCode != 0)
            "API error ${envelope.errorCode}: ${envelope.errorMessage ?: "unknown"}"
        else null
}
