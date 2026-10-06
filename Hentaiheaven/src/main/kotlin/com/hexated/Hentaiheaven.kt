package com.hexated

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class Hentaiheaven : MainAPI() {

    override var mainUrl = "https://hentaihaven.xxx"
    override var name = "Hentaiheaven"
    override val hasMainPage = true
    override var lang = "en"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "/watch/" to "New",
        "/watch/?sort=views" to "Most Views",
        "/watch/?sort=rating" to "Rating",
        "/watch/?sort=az" to "A-Z"
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val url = if (page == 1) {
            "$mainUrl${request.data}"
        } else {
            if (request.data.contains("?")) {
                "$mainUrl${request.data}&page=$page"
            } else {
                "$mainUrl${request.data}?page=$page"
            }
        }

        val html = app.get(url).text

        val flight = Regex(
            """self\.__next_f\.push\((.*?)\)</script>""",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )
            .findAll(html)
            .joinToString("\n") {
                it.groupValues[1]
            }
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")

        println("[$name][MAIN] URL = $url")
        println("[$name][MAIN] HTML size = ${html.length}")
        println("[$name][MAIN] Flight size = ${flight.length}")
        println(
            "[$name][MAIN] Flight matches = ${
                Regex("""self\.__next_f\.push""")
                    .findAll(html)
                    .count()
            }"
        )

        val ids = Regex(
            """"id":(\d+)"""
        )
            .findAll(flight)
            .map {
                it.groupValues[1]
            }
            .toList()

        val slugs = Regex(
            """"slug":"([^"]+)"""
        )
            .findAll(flight)
            .map {
                it.groupValues[1]
            }
            .toList()

        val titles = Regex(
            """"title":\{"rendered":"([^"]+)"""
        )
            .findAll(flight)
            .map {
                it.groupValues[1]
            }
            .toList()

        val thumbnails = Regex(
            """"vraven_remote_thumbnail":"([^"]+)"""
        )
            .findAll(flight)
            .map {
                it.groupValues[1]
            }
            .toList()

        println("[$name][MAIN] IDs = ${ids.size}")
        println("[$name][MAIN] Slugs = ${slugs.size}")
        println("[$name][MAIN] Titles = ${titles.size}")
        println("[$name][MAIN] Thumbnails = ${thumbnails.size}")

        val count = minOf(
            slugs.size,
            titles.size
        )

        val results = (0 until count).map { index ->
            val slug = slugs[index]
            val title = titles[index]

            println(
                "[$name][MAIN] [$index] $title -> $slug"
            )

            newAnimeSearchResponse(
                title,
                "$mainUrl/watch/$slug/",
                TvType.NSFW
            )
        }

        println("[$name][MAIN] Final results = ${results.size}")

        return newHomePageResponse(
            request.name,
            results
        )
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val html = app.get(data).text

        val m3u8 = Regex(
            """https?://[^"'<>\\\s]+\.m3u8(?:\?[^"'<>\\\s]*)?""",
            RegexOption.IGNORE_CASE
        )
            .find(html)
            ?.value
            ?: return false

        println("[$name][LINKS] URL = $data")
        println("[$name][LINKS] M3U8 = $m3u8")

        callback(
            newExtractorLink(
                this.name,
                this.name,
                m3u8,
                ExtractorLinkType.M3U8
            )
        )

        return true
    }
}
