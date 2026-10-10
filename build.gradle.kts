plugins {
    id("stratastax.library")
}

description = "Building blocks for entity modeling"

publishing {
    publications.named<MavenPublication>("mavenJava") { artifactId = "entity" }
}

dependencies {
    testImplementation(libs.bundles.test)
}
