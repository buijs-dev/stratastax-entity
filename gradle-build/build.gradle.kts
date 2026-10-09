plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    mavenLocal()
    maven("https://maven.pkg.github.com/buijs-dev/*") {
        credentials {
            username = providers.gradleProperty("gpr.user").orElse(providers.environmentVariable("GITHUB_ACTOR")).orNull
            password = providers.gradleProperty("gpr.key").orElse(providers.environmentVariable("GITHUB_TOKEN")).orNull
        }
        mavenContent {
            releasesOnly()
            includeGroupByRegex("dev\\.buijs.*")
        }
    }
    maven("https://repo.repsy.io/mvn/buijs-dev/maven") {
        mavenContent {
            snapshotsOnly()
            includeGroupByRegex("dev\\.buijs.*")
        }
    }
}

dependencies {
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
    implementation(libs.stratastax.style.plugin)
    implementation(libs.kover.plugin)
    implementation(libs.dokka.plugin)
    implementation(libs.kotlin.plugin.jvm)
}
