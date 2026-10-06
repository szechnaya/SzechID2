package com.hexated

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class Hentaiheaven : MainAPI() {

    override var mainUrl = "https://hentaihaven.xxx"
    override var homeUrl = "https://hentaihaven.xxx/watch/"
    override var name = "Hentaiheaven"
    override val hasMainPage = true
    override var lang = "en"
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.NSFW)

    override val mainPage = mainPageOf(
        "?sort=new" to "New",
        "?sort=views" to "Most Views",
        "?sort=rating" to "Rating",
        "?sort=az" to "A-Z",
        "?sort=latest" to "Latest"
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val url = if (page == 1) {
            "$homeUrl${request.data}"
        } else {
            "${homeUrl}page/$page/${request.data}"
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

        val slugs = Regex(
            """"slug":"([^"]+)"""
        )
            .findAll(flight)
            .map { it.groupValues[1] }
            .toList()

        val titles = Regex(
            """"title":\{"rendered":"([^"]+)"""
        )
            .findAll(flight)
            .map { it.groupValues[1] }
            .toList()

        val thumbnails = Regex(
            """"vraven_remote_thumbnail":"([^"]+)"""
        )
            .findAll(flight)
            .map { it.groupValues[1] }
            .toList()

        val count = minOf(
            slugs.size,
            titles.size,
            thumbnails.size
        )

        val results = (0 until count).map { index ->
            val slug = slugs[index]
            val title = titles[index]
                .replace("\\\"", "\"")
            val thumbnail = thumbnails[index]

            newAnimeSearchResponse(
                title,
                "$mainUrl/watch/$slug/",
                TvType.NSFW
            ).apply {
                posterUrl = "https://img.hentaihaven.xxx/$thumbnail"
            }
        }

        return newHomePageResponse(
            request.name,
            results
        )
    }

    /*
    override suspend fun load(url: String): LoadResponse? {
        val slug = url
            .substringAfter("/watch/")
            .trimEnd('/')
            .substringBefore("/episode-")

        val html = app.get(url).text

        val title = Regex(
            """"title":\{"rendered":"([^"]+)"""
        )
            .find(html)
            ?.groupValues
            ?.getOrNull(1)
            ?.replace("\\\"", "\"")
            ?: slug
                .replace("-", " ")
                .replaceFirstChar { it.uppercase() }

        val episodes = mutableListOf<Episode>()

        for (episodeNumber in 1..100) {
            val episodeUrl =
                "$mainUrl/watch/$slug/episode-$episodeNumber/"

            val response = app.get(
                episodeUrl,
                timeout = 10
            )

            if (!response.isSuccessful) {
                break
            }

            if (!response.url.toString().contains(
                    "/watch/$slug/episode-$episodeNumber/"
                )
            ) {
                break
            }

            episodes.add(
                newEpisode(
                    episodeUrl,
                    episodeNumber
                )
            )
        }

        if (episodes.isEmpty()) {
            episodes.add(
                newEpisode(
                    url,
                    1
                )
            )
        }

        return newAnimeLoadResponse(
            title,
            url,
            TvType.NSFW
        ) {
            addEpisodes(
                DubStatus.Subbed,
                episodes
            )
        }
    }*/
    
        override suspend fun load(url: String): LoadResponse? {
        val slug = url
            .substringAfter("/watch/")
            .trimEnd('/')
            .substringBefore("/episode-")
    
        val html = app.get(url).text
    
        val title = Regex(
            """"title":\{"rendered":"([^"]+)"""
        )
            .find(html)
            ?.groupValues
            ?.getOrNull(1)
            ?.replace("\\\"", "\"")
            ?: slug
                .replace("-", " ")
                .replaceFirstChar { it.uppercase() }
    
        val episodes = mutableListOf<Episode>()
    
        for (episodeNumber in 1..100) {
            val episodeUrl =
                "$mainUrl/watch/$slug/episode-$episodeNumber/"
    
            val response = app.get(
                episodeUrl,
                timeout = 10
            )
    
            if (!response.isSuccessful) {
                break
            }
    
            if (!response.url.toString().contains(
                    "/watch/$slug/episode-$episodeNumber/"
                )
            ) {
                break
            }
    
            episodes.add(
                newEpisode(episodeUrl) {
                    name = "Episode $episodeNumber"
                    episode = episodeNumber
                }
            )
        }
    
        if (episodes.isEmpty()) {
            episodes.add(
                newEpisode(url) {
                    name = "Episode 1"
                    episode = 1
                }
            )
        }
    
        return newAnimeLoadResponse(
            title,
            url,
            TvType.NSFW
        ) {
            addEpisodes(
                DubStatus.Subbed,
                episodes
            )
        }
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
