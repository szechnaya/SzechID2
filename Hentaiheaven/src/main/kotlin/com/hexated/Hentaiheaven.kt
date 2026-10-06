package com.hexated

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.*
import okhttp3.MultipartBody
import org.jsoup.nodes.Element
import java.util.Base64

class Hentaiheaven : MainAPI() {

    override var mainUrl = "https://hentaihaven.xxx"
    override var name = "Hentaiheaven"
    override val hasMainPage = true
    override var lang = "en"
    override val hasDownloadSupport = true

    override val supportedTypes = setOf(
        TvType.NSFW
    )

    // =========================================================
    // MAIN PAGE
    // =========================================================

    override val mainPage = mainPageOf(
        "/watch/" to "New",
        "/watch/?sort=views" to "Most Views",
        "/watch/?sort=rating" to "Rating",
        "/watch/?sort=az" to "A-Z",
    )

    // =========================================================
    // DEBUG
    // =========================================================

    private fun debug(tag: String, message: String) {
        println("[$name][$tag] $message")
    }

    // =========================================================
    // MAIN PAGE
    // =========================================================

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        debug("MAIN_PAGE", "========================================")
        debug("MAIN_PAGE", "getMainPage() called")
        debug("MAIN_PAGE", "page    = $page")
        debug("MAIN_PAGE", "name    = ${request.name}")
        debug("MAIN_PAGE", "data    = ${request.data}")
        debug("MAIN_PAGE", "mainUrl = $mainUrl")

        /*
         * Page 1:
         *   /watch/
         *   /watch/?sort=views
         *
         * Page > 1:
         *   sementara menggunakan ?page=X
         *
         * Ini sengaja dibuat mudah diubah setelah kita
         * melihat struktur pagination situs.
         */

        val basePath = request.data

        val url = if (page == 1) {
            "$mainUrl$basePath"
        } else {
            if (basePath.contains("?")) {
                "$mainUrl$basePath&page=$page"
            } else {
                "$mainUrl$basePath?page=$page"
            }
        }

        debug("MAIN_PAGE", "Request URL: $url")

        return try {

            val response = app.get(url)

            debug("MAIN_PAGE", "HTTP request completed")
            debug("MAIN_PAGE", "Status: ${response.code}")
            debug("MAIN_PAGE", "Response URL: ${response.url}")

            val document = response.document

            debug(
                "MAIN_PAGE",
                "Document title: ${document.title()}"
            )

            // -------------------------------------------------
            // DEBUG STRUCTURE
            // -------------------------------------------------

            debug(
                "MAIN_PAGE",
                "Total div: ${document.select("div").size}"
            )

            debug(
                "MAIN_PAGE",
                "Total article: ${document.select("article").size}"
            )

            debug(
                "MAIN_PAGE",
                "Total li: ${document.select("li").size}"
            )

            debug(
                "MAIN_PAGE",
                "Total a: ${document.select("a").size}"
            )

            debug(
                "MAIN_PAGE",
                "page-listing-item: ${
                    document.select("div.page-listing-item").size
                }"
            )

            debug(
                "MAIN_PAGE",
                "c-tabs-item: ${
                    document.select("div.c-tabs-item").size
                }"
            )

            debug(
                "MAIN_PAGE",
                "col-6.col-md-zarat: ${
                    document.select("div.col-6.col-md-zarat").size
                }"
            )

            debug(
                "MAIN_PAGE",
                "listing-chapters_wrap: ${
                    document.select("div.listing-chapters_wrap").size
                }"
            )

            // -------------------------------------------------
            // OLD SELECTOR
            // -------------------------------------------------

            val oldSelector =
                "div.page-listing-item div.col-6.col-md-zarat.badge-pos-1"

            val oldElements =
                document.select(oldSelector)

            debug(
                "MAIN_PAGE",
                "OLD selector [$oldSelector] = ${oldElements.size}"
            )

            // -------------------------------------------------
            // TRY COMMON SELECTORS
            // -------------------------------------------------

            val selectors = listOf(
                "div.col-6.col-md-zarat",
                "div.page-listing-item .row > div",
                "div.c-tabs-item__content",
                "div.c-tabs-item",
                "article",
                ".page-listing-item",
                ".c-tabs-item__content",
                ".item-summary",
                ".item-thumb"
            )

            selectors.forEach { selector ->

                try {

                    val count =
                        document.select(selector).size

                    debug(
                        "MAIN_PAGE",
                        "Selector [$selector] = $count"
                    )

                } catch (e: Exception) {

                    debug(
                        "MAIN_PAGE",
                        "Selector error [$selector]: ${e.message}"
                    )
                }
            }

            // -------------------------------------------------
            // PARSE USING CURRENT KNOWN SELECTOR
            // -------------------------------------------------

            val home =
                oldElements.mapIndexedNotNull { index, element ->

                    debug(
                        "MAIN_PAGE",
                        "Parsing old-selector item #$index"
                    )

                    try {

                        element.toSearchResult()

                    } catch (e: Exception) {

                        debug(
                            "MAIN_PAGE",
                            "Parser error #$index: ${e.message}"
                        )

                        e.printStackTrace()

                        null
                    }
                }

            debug(
                "MAIN_PAGE",
                "Final parsed results: ${home.size}"
            )

            home.forEachIndexed { index, result ->

                debug(
                    "MAIN_PAGE",
                    "RESULT[$index] = $result"
                )
            }

            newHomePageResponse(
                request.name,
                home
            )

        } catch (e: Exception) {

            debug(
                "MAIN_PAGE",
                "FAILED: ${e::class.java.name}: ${e.message}"
            )

            e.printStackTrace()

            throw e
        }
    }

    // =========================================================
    // SEARCH RESULT PARSER
    // =========================================================

    private fun Element.toSearchResult(): AnimeSearchResponse? {

        debug("SEARCH_RESULT", "----------------------------------------")
        debug(
            "SEARCH_RESULT",
            "Tag=${this.tagName()} classes=${this.classNames()}"
        )

        return try {

            // -------------------------------------------------
            // HREF
            // -------------------------------------------------

            val firstAnchor =
                this.selectFirst("a")

            if (firstAnchor == null) {

                debug(
                    "SEARCH_RESULT",
                    "No <a> found"
                )

                return null
            }

            val rawHref =
                firstAnchor.attr("href")

            debug(
                "SEARCH_RESULT",
                "Raw href: $rawHref"
            )

            val href =
                fixUrl(rawHref)

            debug(
                "SEARCH_RESULT",
                "Fixed href: $href"
            )

            // -------------------------------------------------
            // TITLE
            // -------------------------------------------------

            val titleElement =
                this.selectFirst("h3 a, h5 a")

            debug(
                "SEARCH_RESULT",
                "Title element found: ${titleElement != null}"
            )

            val title =
                titleElement
                    ?.text()
                    ?.trim()
                    ?: this.selectFirst("a")
                        ?.attr("title")
                    ?: run {

                        debug(
                            "SEARCH_RESULT",
                            "Title not found"
                        )

                        return null
                    }

            debug(
                "SEARCH_RESULT",
                "Title: $title"
            )

            // -------------------------------------------------
            // POSTER
            // -------------------------------------------------

            val image =
                this.selectFirst("img")

            debug(
                "SEARCH_RESULT",
                "Image element found: ${image != null}"
            )

            val rawPoster =
                image?.attr("src")

            debug(
                "SEARCH_RESULT",
                "Raw poster: $rawPoster"
            )

            val posterUrl =
                fixUrlNull(rawPoster)

            debug(
                "SEARCH_RESULT",
                "Fixed poster: $posterUrl"
            )

            // -------------------------------------------------
            // EPISODE
            // -------------------------------------------------

            val episodeText =
                this.selectFirst(
                    "span.chapter.font-meta a"
                )?.text()

            debug(
                "SEARCH_RESULT",
                "Episode text: $episodeText"
            )

            val episode =
                episodeText
                    ?.filter { it.isDigit() }
                    ?.toIntOrNull()

            debug(
                "SEARCH_RESULT",
                "Episode parsed: $episode"
            )

            // -------------------------------------------------
            // RESULT
            // -------------------------------------------------

            val result =
                newAnimeSearchResponse(
                    title,
                    href,
                    TvType.Anime
                ) {

                    this.posterUrl =
                        posterUrl

                    addSub(episode)
                }

            debug(
                "SEARCH_RESULT",
                "Result created successfully"
            )

            result

        } catch (e: Exception) {

            debug(
                "SEARCH_RESULT",
                "FAILED: ${e::class.java.name}: ${e.message}"
            )

            e.printStackTrace()

            null
        }
    }

    // =========================================================
    // SEARCH
    // =========================================================

    override suspend fun search(
        query: String
    ): List<SearchResponse> {

        debug("SEARCH", "========================================")
        debug("SEARCH", "search() called")
        debug("SEARCH", "Query: $query")

        val link =
            "$mainUrl/?s=$query&post_type=wp-manga"

        debug(
            "SEARCH",
            "Request URL: $link"
        )

        return try {

            val response =
                app.get(link)

            debug(
                "SEARCH",
                "HTTP request completed"
            )

            debug(
                "SEARCH",
                "Status: ${response.code}"
            )

            debug(
                "SEARCH",
                "Response URL: ${response.url}"
            )

            val document =
                response.document

            debug(
                "SEARCH",
                "Document title: ${document.title()}"
            )

            // -------------------------------------------------
            // DEBUG SELECTORS
            // -------------------------------------------------

            val selectors = listOf(
                "div.c-tabs-item",
                "div.c-tabs-item__content",
                "div.page-listing-item",
                "article",
                "div.col-6.col-md-zarat",
                ".item-summary",
                ".item-thumb"
            )

            selectors.forEach { selector ->

                debug(
                    "SEARCH",
                    "Selector [$selector] = ${
                        document.select(selector).size
                    }"
                )
            }

            val selector =
                "div.c-tabs-item > div.c-tabs-item__content"

            val elements =
                document.select(selector)

            debug(
                "SEARCH",
                "Search elements: ${elements.size}"
            )

            val results =
                elements.mapIndexedNotNull { index, element ->

                    debug(
                        "SEARCH",
                        "Parsing result #$index"
                    )

                    try {

                        element.toSearchResult()

                    } catch (e: Exception) {

                        debug(
                            "SEARCH",
                            "Parser error #$index: ${e.message}"
                        )

                        e.printStackTrace()

                        null
                    }
                }

            debug(
                "SEARCH",
                "Final search results: ${results.size}"
            )

            results.forEachIndexed { index, result ->

                debug(
                    "SEARCH",
                    "RESULT[$index] = $result"
                )
            }

            results

        } catch (e: Exception) {

            debug(
                "SEARCH",
                "FAILED: ${e::class.java.name}: ${e.message}"
            )

            e.printStackTrace()

            throw e
        }
    }

    // =========================================================
    // LOAD
    // =========================================================

    override suspend fun load(
        url: String
    ): LoadResponse? {

        debug("LOAD", "========================================")
        debug("LOAD", "load() called")
        debug("LOAD", "URL: $url")

        return try {

            val response =
                app.get(url)

            debug(
                "LOAD",
                "HTTP request completed"
            )

            debug(
                "LOAD",
                "Status: ${response.code}"
            )

            debug(
                "LOAD",
                "Response URL: ${response.url}"
            )

            val document =
                response.document

            debug(
                "LOAD",
                "Document title: ${document.title()}"
            )

            // -------------------------------------------------
            // TITLE
            // -------------------------------------------------

            val titleElement =
                document.selectFirst(
                    "div.post-title h1"
                )

            debug(
                "LOAD",
                "Title element found: ${titleElement != null}"
            )

            val title =
                titleElement
                    ?.text()
                    ?.trim()

            debug(
                "LOAD",
                "Title: $title"
            )

            if (title == null) {

                debug(
                    "LOAD",
                    "Title is null -> returning null"
                )

                return null
            }

            // -------------------------------------------------
            // POSTER
            // -------------------------------------------------

            val posterElements =
                document.select(
                    "div.summary_image img"
                )

            debug(
                "LOAD",
                "Poster elements: ${posterElements.size}"
            )

            val poster =
                posterElements.attr("src")

            debug(
                "LOAD",
                "Poster: $poster"
            )

            // -------------------------------------------------
            // TAGS
            // -------------------------------------------------

            val genreElements =
                document.select(
                    "div.genres-content > a"
                )

            debug(
                "LOAD",
                "Genre elements: ${genreElements.size}"
            )

            val tags =
                genreElements.map {
                    it.text()
                }

            debug(
                "LOAD",
                "Tags: $tags"
            )

            // -------------------------------------------------
            // DESCRIPTION
            // -------------------------------------------------

            val descriptionElements =
                document.select(
                    "div.description-summary p"
                )

            debug(
                "LOAD",
                "Description elements: ${descriptionElements.size}"
            )

            val description =
                descriptionElements
                    .text()
                    .trim()

            debug(
                "LOAD",
                "Description length: ${description.length}"
            )

            debug(
                "LOAD",
                "Description: $description"
            )

            // -------------------------------------------------
            // TRAILER
            // -------------------------------------------------

            val trailerElement =
                document.selectFirst(
                    "a.trailerbutton"
                )

            debug(
                "LOAD",
                "Trailer element found: ${
                    trailerElement != null
                }"
            )

            val trailer =
                trailerElement?.attr("href")

            debug(
                "LOAD",
                "Trailer: $trailer"
            )

            // -------------------------------------------------
            // EPISODES
            // -------------------------------------------------

            val chapterElements =
                document.select(
                    "div.listing-chapters_wrap ul li"
                )

            debug(
                "LOAD",
                "Chapter elements: ${chapterElements.size}"
            )

            val episodes =
                chapterElements
                    .mapIndexedNotNull { index, element ->

                        debug(
                            "LOAD",
                            "Parsing chapter #$index"
                        )

                        try {

                            val anchor =
                                element.selectFirst("a")

                            if (anchor == null) {

                                debug(
                                    "LOAD",
                                    "No anchor -> skip"
                                )

                                return@mapIndexedNotNull null
                            }

                            val name =
                                anchor.text()

                            debug(
                                "LOAD",
                                "Chapter name: $name"
                            )

                            val image =
                                fixUrlNull(
                                    anchor
                                        .selectFirst("img")
                                        ?.attr("src")
                                )

                            debug(
                                "LOAD",
                                "Chapter image: $image"
                            )

                            val link =
                                fixUrlNull(
                                    anchor.attr("href")
                                )

                            debug(
                                "LOAD",
                                "Chapter link: $link"
                            )

                            if (link == null) {

                                debug(
                                    "LOAD",
                                    "Link null -> skip"
                                )

                                return@mapIndexedNotNull null
                            }

                            newEpisode(link) {

                                this.name =
                                    name

                                this.posterUrl =
                                    image
                            }

                        } catch (e: Exception) {

                            debug(
                                "LOAD",
                                "Chapter error: ${e.message}"
                            )

                            e.printStackTrace()

                            null
                        }
                    }
                    .reversed()

            debug(
                "LOAD",
                "Final episode count: ${episodes.size}"
            )

            // -------------------------------------------------
            // RECOMMENDATIONS
            // -------------------------------------------------

            val recommendationElements =
                document.select(
                    "div.row div.col-6.col-md-zarat"
                )

            debug(
                "LOAD",
                "Recommendation elements: ${
                    recommendationElements.size
                }"
            )

            val recommendations =
                recommendationElements
                    .mapIndexedNotNull { index, element ->

                        debug(
                            "LOAD",
                            "Parsing recommendation #$index"
                        )

                        try {

                            element.toSearchResult()

                        } catch (e: Exception) {

                            debug(
                                "LOAD",
                                "Recommendation error: ${e.message}"
                            )

                            null
                        }
                    }

            debug(
                "LOAD",
                "Final recommendations: ${
                    recommendations.size
                }"
            )

            // -------------------------------------------------
            // LOAD RESPONSE
            // -------------------------------------------------

            val result =
                newAnimeLoadResponse(
                    title,
                    url,
                    TvType.NSFW
                ) {

                    engName =
                        title

                    posterUrl =
                        poster

                    addEpisodes(
                        DubStatus.Subbed,
                        episodes
                    )

                    plot =
                        description

                    this.tags =
                        tags

                    this.recommendations =
                        recommendations

                    addTrailer(
                        trailer
                    )
                }

            debug(
                "LOAD",
                "LoadResponse created successfully"
            )

            result

        } catch (e: Exception) {

            debug(
                "LOAD",
                "FAILED: ${e::class.java.name}: ${e.message}"
            )

            e.printStackTrace()

            throw e
        }
    }

    // =========================================================
    // LOAD LINKS
    // SAMA SEPERTI KODE AWAL
    // =========================================================

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val doc = app.get(data).document

        val meta =
            doc.selectFirst(
                "meta[itemprop=thumbnailUrl]"
            )?.attr("content")
                ?.substringAfter("/hh/")
                ?.substringBefore("/")
                ?: return false

        val iframe =
            doc.select(
                "div.player_logic_item iframe"
            ).attr("src")

        val dataParam =
            Regex("[?&]data=([^&]+)")
                .find(iframe)
                ?.groupValues
                ?.getOrNull(1)

        if (dataParam == null)
            return false

        val decoded = try {

            val raw =
                Base64.getDecoder()
                    .decode(dataParam)

            String(raw)

        } catch (e: Exception) {

            println(
                "Failed to decode Base64: ${e.message}"
            )

            return false
        }

        val parts =
            decoded.split(":|::|:")

        if (parts.size != 2) {

            println(
                "Unexpected format after decoding: $decoded"
            )

            return false
        }

        val en =
            parts[0]

        val iv =
            Base64.getEncoder()
                .encodeToString(
                    parts[1].toByteArray()
                )

        val body =
            MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "action",
                    "zarat_get_data_player_ajax"
                )
                .addFormDataPart(
                    "a",
                    en
                )
                .addFormDataPart(
                    "b",
                    iv
                )
                .build()

        val fycfUrl =
            BuildConfig.FYCF_ENDPOINT

        val FYCF_API =
            BuildConfig.FYCF_API

        val response =
            app.post(
                "$fycfUrl/?token=$FYCF_API&url=$mainUrl/wp-content/plugins/player-logic/api.php",
                requestBody = body,
                timeout = 60_000
            ).parsedSafe<Response>()

        if (response == null) {

            println(
                "Response is null or failed to parse"
            )

            return false
        }

        val sources =
            response.data?.sources

        if (sources.isNullOrEmpty()) {

            println(
                "No sources found in response $response"
            )

            return false
        }

        sources.forEach { res ->

            val src =
                res.src

            if (src == null) {

                println(
                    "Source is null, skipping"
                )

                return@forEach
            }

            println(
                "Response src: $src"
            )

            callback.invoke(
                newExtractorLink(
                    this.name,
                    this.name,
                    src,
                    INFER_TYPE
                )
            )
        }

        return true
    }

    // =========================================================
    // RESPONSE MODELS
    // =========================================================

    data class Response(
        @JsonProperty("data")
        val data: Data? = null,
    )

    data class Data(
        @JsonProperty("sources")
        val sources: ArrayList<Sources>? =
            arrayListOf(),
    )

    data class Sources(
        @JsonProperty("src")
        val src: String? = null,

        @JsonProperty("type")
        val type: String? = null,

        @JsonProperty("label")
        val label: String? = null,
    )
}