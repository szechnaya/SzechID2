package com.hexated

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addAniListId
import com.lagradost.cloudstream3.LoadResponse.Companion.addMalId
import com.lagradost.cloudstream3.utils.*
import kotlinx.coroutines.runBlocking
import org.jsoup.nodes.Element

class Zoronime : MainAPI() {
    override var mainUrl = "https://zoronime.com"
    override var name = "Zoronime"
    override val hasMainPage = true
    override var lang = "id"
    override val supportedTypes = setOf(
        TvType.Anime,
        TvType.AnimeMovie,
        TvType.OVA
    )

    companion object {
        fun getType(t: String): TvType {
            return when {
                t.contains("/anime/", true) -> TvType.Anime
                t.contains("/movie/", true) -> TvType.AnimeMovie
                t.contains("/ova/", true) || t.contains("Special", true) -> TvType.OVA
                else -> TvType.Anime
            }
        }

        fun getStatus(t: String?): ShowStatus {
            return when {
                t?.contains("On-Going", true) == true -> ShowStatus.Ongoing
                else -> ShowStatus.Completed
            }
        }
    }

    override val mainPage = mainPageOf(
        "anime" to "Anime",
        "movie" to "Movie",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get("$mainUrl/${request.data}/page/$page").document
        val home = document.select("div.film_list-wrap div.flw-item").mapNotNull {
            it.toSearchResult()
        }
        return newHomePageResponse(
            request.name,
            home
        )
    }

    private fun Element.toSearchResult(): AnimeSearchResponse? {
        val href = fixUrl(this.selectFirst("a")!!.attr("href"))
        val title = this.selectFirst("h3.film-name a")?.text() ?: return null
        val posterUrl = this.selectFirst("img.film-poster-img")?.attr("data-src")
        val episode = this.selectFirst("div.tick.rtl")?.ownText()
            ?.filter { it.isDigit() }?.toIntOrNull()

        return newAnimeSearchResponse(title, href, TvType.Anime) {
            this.posterUrl = posterUrl
            addSub(episode)
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()
        for (i in 1..2) {
            val res =
                app.get("$mainUrl/page/$i/?s=$query").document.select("div.film_list-wrap div.flw-item")
                    .mapNotNull {
                        it.toSearchResult()
                    }
            searchResponse.addAll(res)
        }
        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        val title = document.selectFirst("h2.film-name.dynamic-name, h2.film-name")?.text() ?: ""
        val poster = document.selectFirst("div#ani_detail div.film-poster img.film-poster-img")
            ?.attr("data-src")
        val tags = document.select("div.item.item-list a").map { it.text() }

        val year =
            document.select("div.item.item-title:has(span:contains(Aired)) a, div.film-stats a[href*=year]")
                .text().filter { it.isDigit() }.toIntOrNull()
        val status = getStatus(
            document.selectFirst("div.item.item-title:has(span:contains(Status)) a")?.text()
                ?: document.selectFirst("div.film-stats > span:nth-child(4)")?.text()
        )
        val type = getType(url)
        val description = document.select("div.film-description.m-hide").text().trim()

        val episodes = if (type == TvType.AnimeMovie) {
            listOf(newEpisode(url))
        } else {
            val button = document.select("div.film-buttons a").attr("href")
            app.get(button).document.select("div#episodes-page-1 a").mapIndexedNotNull { index, element ->
                val episode = element.attr("id").toIntOrNull() ?: (index + 1)
                val link = element.attr("href")
                newEpisode(url = link, initializer = { this.episode = episode }, fix = false)
            }.reversed()
        }

        val recommendations = document.select("div.film_list-wrap div.flw-item").mapNotNull {
            it.toSearchResult()
        }

        val tracker = APIHolder.getTracker(listOf(title), TrackerType.getTypes(type), year, true)

        return newAnimeLoadResponse(title, url, type) {
            engName = title
            posterUrl = tracker?.image ?: poster
            backgroundPosterUrl = tracker?.cover ?: poster
            this.year = year
            addEpisodes(DubStatus.Subbed, episodes)
            showStatus = status
            plot = description
            this.tags = tags
            this.recommendations = recommendations
            addMalId(tracker?.malId)
            addAniListId(tracker?.aniId?.toIntOrNull())
        }

    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val doc = app.get(data).document

        doc.select("div.item a[id*=server]").amap { server ->
            val quality = server.text().filter { it.isDigit() }.toIntOrNull()
            loadFixedExtractor(server.attr("href"), quality, null, subtitleCallback, callback)
        }

        return true
    }

    private suspend fun loadFixedExtractor(
        url: String,
        quality: Int?,
        referer: String? = null,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        loadExtractor(url, referer, subtitleCallback) { link ->
            runBlocking {
                callback.invoke(
                    newExtractorLink(
                        link.name,
                        link.name,
                        link.url,
                        link.type
                    ) {
                        this.referer = link.referer
                        this.quality = quality ?: Qualities.Unknown.value
                        this.headers = link.headers
                        this.extractorData = link.extractorData
                    }
                )
            }
        }
    }

}