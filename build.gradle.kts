plugins {
    id("stratastax.library")
}

description = "Building blocks for entity modelling"

publishing {
    publications.named<MavenPublication>("mavenJava") { artifactId = "entity" }
}

dependencies {
    implementation(libs.bundles.test)
}
