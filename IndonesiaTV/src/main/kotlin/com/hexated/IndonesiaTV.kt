package com.hexated

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.newExtractorLink


class IndonesiaTV : MainAPI() {
    override var mainUrl = "https://raw.githubusercontent.com/Sofie99/Resources/refs/heads/main/iptv.json"
    override var name = "IndonesiaTV"
    override val hasDownloadSupport = false
    override val hasMainPage = true
    override val supportedTypes = setOf(
        TvType.Live
    )

    override val mainPage = mainPageOf(
        "Channels" to mainUrl,
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {

        val json = app.get(mainUrl).text
        val home = tryParseJson<ArrayList<Channels>>(json)?.map {
            newLiveSearchResponse(
                it.channel ?: "",
                ChannelData(it.channel, it.url, it.poster, it.group).toJson(),
                fix = false
            ) {
                posterUrl = it.poster
            }
        } ?: throw ErrorLoadingException()

        return newHomePageResponse(listOf(HomePageList(request.name, home, true)), false)
    }

    override suspend fun load(url: String): LoadResponse {
        val data = parseJson<Channels>(url)
        return newLiveStreamLoadResponse(
            data.channel ?: "",
            url,
            url.toJson()
        ) {
            posterUrl = data.poster
        }

    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val json = parseJson<Channels>(data)
        when {
            json.group.equals("rctiplus") -> {
                invokeRctiPlus(
                    json.channel ?: return false,
                    json.url ?: return false,
                    callback
                )
            }
            json.group.equals("vidio.com") -> {
                invokeVideoCom(
                    json.channel ?: return false,
                    json.url ?: return false,
                    callback
                )
            }
            json.group.equals("transmedia") -> {
                invokeTransMedia(
                    json.channel ?: return false,
                    json.url ?: return false,
                    callback
                )
            }
            else -> {}
        }

        return true
    }

    private suspend fun invokeTransMedia(
        channel: String,
        url: String,
        callback: (ExtractorLink) -> Unit
    ) {
        callback.invoke(
            newExtractorLink(
                channel,
                channel,
                url,
                ExtractorLinkType.M3U8
            ) {
                this.referer = "https://20.detik.com/"
            }
        )
    }

    private suspend fun invokeVideoCom(
        channel: String,
        url: String,
        callback: (ExtractorLink) -> Unit
    ) {
        val video = app.get(
            url, headers = mapOf(
                "X-Api-Key" to "CH1ZFsN4N/MIfAds1DL9mP151CNqIpWHqZGRr+LkvUyiq3FRPuP1Kt6aK+pG3nEC1FXt0ZAAJ5FKP8QU8CZ5/vOCo++GInT1QwtQAeXAHFIp5jyVmvRHXJg9ULThAvmCEHA/pd1hJT1aTwWZhf6YEEBEQw5qL4ozjWr2m9pdPSE=",
                "X-Signature" to "4c171ae6f4feb5c98d11ec9ca0ea67970bb15cb6a894510ce4bb2ef48f6f4c96",
                "X-API-Platform" to "web-desktop",
                "X-Client" to "1758467532.263",
                "X-Secure-Level" to "2",
            )
        ).parsedSafe<VideoComSource>()?.data?.attributes?.hls

        callback.invoke(
            newExtractorLink(
                channel,
                channel,
                video ?: return,
                ExtractorLinkType.M3U8
            ) {
                this.referer = "https://www.vidio.com/"
            }
        )
    }

    private suspend fun invokeRctiPlus(
        channel: String,
        url: String,
        callback: (ExtractorLink) -> Unit
    ) {
        val video = app.get(
            url, headers = mapOf(
                "apikey" to "k1DzR0yYWIyZgvTvixiDHnb4Nl08wSU0",
                "Authorization" to "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ2aWQiOjAsInRva2VuIjoiZjlhYjEyMjg1NmQ3NGYwZiIsInBsIjoid2ViIiwiZGV2aWNlX2lkIjoiMjllNWZkOGEtMDg2YS0xMWVmLTliYTAtMDAxNjNlMDQxOGVjIn0.T0iVov0r2Ai-bhP3NsSoOZhP2WansABSNhrWzvB29-c"
            )
        ).parsedSafe<RctiPlusSource>()?.data?.url

        callback.invoke(
            newExtractorLink(
                channel,
                channel,
                video ?: return,
                ExtractorLinkType.M3U8
            ) {
                this.referer = "https://www.rctiplus.com/"
            }
        )
    }

    data class ChannelData(
        val channel: String? = null,
        val url: String? = null,
        val poster: String? = null,
        val group: String? = null,
    )

    data class Channels(
        @JsonProperty("channel") val channel: String? = null,
        @JsonProperty("url") val url: String? = null,
        @JsonProperty("poster") val poster: String? = null,
        @JsonProperty("group") val group: String? = null,
    )

    data class RctiPlusSource(
        @JsonProperty("data") val data: Data? = null,
    ) {
        data class Data(
            @JsonProperty("url") val url: String? = null,
        )
    }

    data class VideoComSource(
        @JsonProperty("data") val data: Data? = null,
    ) {
        data class Data(
            @JsonProperty("attributes") val attributes: Attributes? = null,
        ) {
            data class Attributes(
                @JsonProperty("hls") val hls: String? = null,
            )
        }
    }

}