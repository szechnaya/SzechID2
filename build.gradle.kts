import com.android.build.gradle.BaseExtension
import com.lagradost.cloudstream3.gradle.CloudstreamExtension
import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }

    dependencies {
        classpath("com.android.tools.build:gradle:8.7.3")
        classpath("com.github.recloudstream:gradle:-SNAPSHOT")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

fun Project.cloudstream(
    configuration: CloudstreamExtension.() -> Unit
) = extensions
    .getByName<CloudstreamExtension>("cloudstream")
    .configuration()

fun Project.android(
    configuration: BaseExtension.() -> Unit
) = extensions
    .getByName<BaseExtension>("android")
    .configuration()

subprojects {
    apply(plugin = "com.android.library")
    apply(plugin = "kotlin-android")
    apply(plugin = "com.lagradost.cloudstream3.gradle")

    cloudstream {
        setRepo(
            System.getenv("GITHUB_REPOSITORY")
                ?: "https://github.com/hexated/cloudstream-extensions-hexated"
        )

        authors = listOf("Hexated")
    }

    android {
        namespace = "com.hexated"

        defaultConfig {
            minSdk = 21
            compileSdkVersion(35)
            targetSdk = 35
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)

            freeCompilerArgs.addAll(
                "-Xno-call-assertions",
                "-Xno-param-assertions",
                "-Xno-receiver-assertions"
            )
        }
    }

    dependencies {
        add(
            "implementation",
            "com.lagradost:cloudstream3:pre-release"
        )

        add(
            "implementation",
            kotlin("stdlib")
        )

        add(
            "implementation",
            "com.github.Blatzar:NiceHttp:0.4.13"
        )

        add(
            "implementation",
            "org.jsoup:jsoup:1.18.3"
        )

        add(
            "implementation",
            "com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1"
        )

        add(
            "implementation",
            "io.karn:khttp-android:0.1.2"
        )

        add(
            "implementation",
            "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0"
        )

        add(
            "implementation",
            "org.mozilla:rhino:1.7.14"
        )
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
