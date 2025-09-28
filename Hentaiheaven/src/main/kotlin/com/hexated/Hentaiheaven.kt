package com.hexated

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.*
import okhttp3.FormBody
import okhttp3.Headers
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

    override val mainPage = mainPageOf(
        "?m_orderby=new-manga" to "New",
        "?m_orderby=views" to "Most Views",
        "?m_orderby=rating" to "Rating",
        "?m_orderby=alphabet" to "A-Z",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get("$mainUrl/page/$page/${request.data}").document
        val home =
            document.select("div.page-listing-item div.col-6.col-md-zarat.badge-pos-1").mapNotNull {
                it.toSearchResult()
            }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): AnimeSearchResponse? {
        val href = fixUrl(this.selectFirst("a")!!.attr("href"))
        val title =
            this.selectFirst("h3 a, h5 a")?.text()?.trim() ?: this.selectFirst("a")?.attr("title")
            ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))
        val episode = this.selectFirst("span.chapter.font-meta a")?.text()?.filter { it.isDigit() }
            ?.toIntOrNull()

        return newAnimeSearchResponse(title, href, TvType.Anime) {
            this.posterUrl = posterUrl
            addSub(episode)
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val link = "$mainUrl/?s=$query&post_type=wp-manga"
        val document = app.get(link).document

        return document.select("div.c-tabs-item > div.c-tabs-item__content").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("div.post-title h1")?.text()?.trim() ?: return null
        val poster = document.select("div.summary_image img").attr("src")
        val tags = document.select("div.genres-content > a").map { it.text() }

        val description = document.select("div.description-summary p").text().trim()
        val trailer = document.selectFirst("a.trailerbutton")?.attr("href")

        val episodes = document.select("div.listing-chapters_wrap ul li").mapNotNull {
            val name = it.selectFirst("a")?.text() ?: return@mapNotNull null
            val image = fixUrlNull(it.selectFirst("a img")?.attr("src"))
            val link = fixUrlNull(it.selectFirst("a")?.attr("href")) ?: return@mapNotNull null
            newEpisode(link) {
                this.name = name
                this.posterUrl = image
            }
        }.reversed()

        val recommendations =
            document.select("div.row div.col-6.col-md-zarat").mapNotNull {
                it.toSearchResult()
            }

        return newAnimeLoadResponse(title, url, TvType.NSFW) {
            engName = title
            posterUrl = poster
            addEpisodes(DubStatus.Subbed, episodes)
            plot = description
            this.tags = tags
            this.recommendations = recommendations
            addTrailer(trailer)
        }

    }

    /*
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val doc = app.get(data).document
        val meta =
            doc.selectFirst("meta[itemprop=thumbnailUrl]")?.attr("content")?.substringAfter("/hh/")
                ?.substringBefore("/") ?: return false
        doc.select("div.player_logic_item iframe").attr("src").let { iframe ->
            val document = app.get(iframe, referer = data).text
            println("Iframe JS: $document")
            val en = Regex("var\\sen\\s=\\s'(\\S+)';").find(document)?.groupValues?.getOrNull(1)
            val iv = Regex("var\\siv\\s=\\s'(\\S+)';").find(document)?.groupValues?.getOrNull(1)

            println("Meta: $meta")
            println("Iframe src: $iframe")
            println("en: $en, iv: $iv")
            //println("Response src: ${res.src}")
            val body = FormBody.Builder()
                .addEncoded("action", "zarat_get_data_player_ajax")
                .addEncoded("a", "$en")
                .addEncoded("b", "$iv")
                .build()

            app.post(
                "$mainUrl/wp-content/plugins/player-logic/api.php",
                requestBody = body,
            ).parsedSafe<Response>()?.data?.sources?.map { res ->
                println("Response src: ${res.src}")
                println("Meta: $meta")
                println("Iframe src: $iframe")
                println("en: $en, iv: $iv")
                callback.invoke(
                    newExtractorLink(
                        this.name,
                        this.name,
                        //res.src?.replace("/hh//", "/hh/$meta/") ?: return@map null,
                        //Test Fix
                        res.src?: return@map null,
                        INFER_TYPE
                    )
                )
            }
        }

        return true
    }*/
    override suspend fun loadLinks(
    data: String,
    isCasting: Boolean,
    subtitleCallback: (SubtitleFile) -> Unit,
    callback: (ExtractorLink) -> Unit
): Boolean {

    val doc = app.get(data).document
    val meta = doc.selectFirst("meta[itemprop=thumbnailUrl]")?.attr("content")
?.substringAfter("/hh/")?.substringBefore("/")?: return false

    val iframe = doc.select("div.player_logic_item iframe").attr("src")

    val dataParam = Regex("[?&]data=([^&]+)").find(iframe)?.groupValues?.getOrNull(1)
    if (dataParam == null ||!dataParam.endsWith("=")) return false

    val decoded = try {
        val raw = Base64.getDecoder().decode(dataParam)
        String(raw)
} catch (e: Exception) {
        println("Failed to decode Base64: ${e.message}")
        return false
}

    val parts = decoded.split(":|::|:")
    if (parts.size!= 2) {
        println("Unexpected format after decoding: $decoded")
        return false
}

    val en = parts[0]
    val iv = Base64.getEncoder().encodeToString(parts[1].toByteArray())

    println("Meta: $meta")
    println("Iframe src: $iframe")
    println("Decoded: $decoded")
    println("en: $en")
    println("iv: $iv")

    val body = FormBody.Builder()
.addEncoded("action", "zarat_get_data_player_ajax")
.addEncoded("a", en)
.addEncoded("b", iv)
.build()

val headers = Headers.Builder()
.add("User-Agent", "Mozilla/5.0")
.add("Accept", "application/json")
.add("Referer", mainUrl)
.build()

   val response = app.post(
       "$mainUrl/wp-content/plugins/player-logic/api.php",
       requestBody = body,
       headers = headers
).parsedSafe<Response>()

if (response == null) {
    println("Response is null or failed to parse")
    return false
}

val sources = response.data?.sources
if (sources.isNullOrEmpty()) {
    println("No sources found in response")
    return false
}

sources.forEach { res ->
    val src = res.src
    if (src == null) {
        println("Source is null, skipping")
        return@forEach
}

    println("Response src: $src")

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

    data class Response(
        @JsonProperty("data") val data: Data? = null,
    )

    data class Data(
        @JsonProperty("sources") val sources: ArrayList<Sources>? = arrayListOf(),
    )

    data class Sources(
        @JsonProperty("src") val src: String? = null,
        @JsonProperty("type") val type: String? = null,
        @JsonProperty("label") val label: String? = null,
    )


}