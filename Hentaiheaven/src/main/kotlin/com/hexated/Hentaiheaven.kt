package com.hexated

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class Hentaiheaven : MainAPI() {

    override var mainUrl = "https://hentaihaven.xxx"
    override var name = "Hentaiheaven"
    override val hasMainPage = true
    override var lang = "en"
    override val hasDownloadSupport = true

    override val supportedTypes = setOf(
        TvType.NSFW
    )

    override val mainPage = mainPageOf(
        "/watch/" to "New",
        "/watch/?sort=views" to "Most Views",
        "/watch/?sort=rating" to "Rating",
        "/watch/?sort=az" to "A-Z",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
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

        val html = app.get(url).text

        val pattern = Regex(
            """\{"id":(\d+).*?"slug":"([^"]+)".*?"title":\{"rendered":"(.*?)"\}.*?"vraven_remote_thumbnail":"([^"]+)"""",
            setOf(RegexOption.DOT_MATCHES_ALL)
        )

        val home = pattern.findAll(html)
            .mapNotNull { match ->
                val slug = match.groupValues[2]

                val title = match.groupValues[3]
                    .replace("\\/", "/")
                    .replace("\\u0026", "&")

                newAnimeSearchResponse(
                    title,
                    "$mainUrl/watch/$slug/",
                    TvType.NSFW
                )
            }
            .toList()

        return newHomePageResponse(
            request.name,
            home
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
