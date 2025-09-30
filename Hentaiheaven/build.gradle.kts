import org.jetbrains.kotlin.konan.properties.Properties
// use an integer for version numbers
version = 7

android {
    defaultConfig {
        val properties = Properties()
        properties.load(project.rootProject.file("local.properties").inputStream())

        buildConfigField("String", "FYCF_API", "\"${properties.getProperty("FYCF_API")}\"")
        buildConfigField("String", "FYCF_ENDPOINT", "\"${properties.getProperty("FYCF_ENDPOINT")}\"")


    }
    
    buildFeatures {
        buildConfig = true
    }
}

cloudstream {
    language = "en"
    // All of these properties are optional, you can safely remove them

    // description = "Lorem Ipsum"
     authors = listOf("SzechnayaID")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 1 // will be 3 if unspecified
    tvTypes = listOf(
        "NSFW",
    )

    iconUrl = "https://www.google.com/s2/favicons?domain=hentaihaven.xxx&sz=%size%"
}