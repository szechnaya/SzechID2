// use an integer for version numbers
version = 16


cloudstream {
    language = "id"
    // All of these properties are optional, you can safely remove them

    description = "Disabled"
     authors = listOf("Szechnaya ID")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 0 // will be 3 if unspecified
    tvTypes = listOf(
        "AnimeMovie",
        "OVA",
        "Anime",
    )

    iconUrl = "https://animeindo.skin/favicon/favicon-32x32.png"
}