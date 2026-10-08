plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
    implementation(libs.stratastax.style.plugin)
    implementation(libs.kover.plugin)
    implementation(libs.dokka.plugin)
    implementation(libs.kotlin.plugin.jvm)
}
