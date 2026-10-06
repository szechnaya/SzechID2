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
    
        val ids = Regex(
            """"id":(\d+)"""
        ).findAll(html)
            .map { it.groupValues[1] }
            .toList()
    
        val slugs = Regex(
            """"slug":"([^"]+)""""
        ).findAll(html)
            .map { it.groupValues[1] }
            .toList()
    
        val titles = Regex(
            """"title":\{"rendered":"([^"]+)""""
        ).findAll(html)
            .map {
                it.groupValues[1]
                    .replace("\\/", "/")
                    .replace("\\u0026", "&")
            }
            .toList()
    
        val thumbnails = Regex(
            """"vraven_remote_thumbnail":"([^"]+)""""
        ).findAll(html)
            .map {
                it.groupValues[1]
                    .replace("\\/", "/")
            }
            .toList()
    
        println("[$name][MAIN] HTML size = ${html.length}")
        println("[$name][MAIN] IDs = ${ids.size}")
        println("[$name][MAIN] Slugs = ${slugs.size}")
        println("[$name][MAIN] Titles = ${titles.size}")
        println("[$name][MAIN] Thumbnails = ${thumbnails.size}")
    
        val count = minOf(
            slugs.size,
            titles.size
        )
    
        val home = (0 until count).mapNotNull { index ->
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
    
        println("[$name][MAIN] Final results = ${home.size}")
    
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
