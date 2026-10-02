plugins {
    id("stratastax.kotlin")
    id("stratastax.publishing")
    id("stratastax.repositories")
    id("stratastax.spotless")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            artifactId = "entity"

            pom {
                name = "Stratastax Entity"
                description = "Building blocks for entity modelling"
                url = "https://github.com/buijs-dev/stratastax-entity"

                licenses {
                    license {
                        name = "MIT License"
                        url = "https://github.com/buijs-dev/stratastax-entity/blob/main/LICENSE"
                    }
                }

                developers {
                    developer {
                        id = "buijs-dev"
                        name = "Gillian Buijs"
                        email = "info@buijs.dev"
                    }
                }

                scm {
                    connection = "scm:git:https://github.com/buijs-dev/stratastax-entity.git"
                    developerConnection = "scm:git:ssh://git@github.com/buijs-dev/stratastax-entity.git"
                    url = "https://github.com/buijs-dev/stratastax-entity"
                }
            }
        }
    }
}

dependencies {
    implementation(libs.bundles.test)
}
