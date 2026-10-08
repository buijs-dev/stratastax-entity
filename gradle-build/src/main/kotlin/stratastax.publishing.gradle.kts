plugins {
    `maven-publish`
}

group = "dev.buijs.stratastax"

val repository = providers.gradleProperty("stratastax.repository").get()
val displayName = providers.gradleProperty("stratastax.displayName").get()
val githubUrl = "https://github.com/buijs-dev/$repository"

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            val nameConvention = if (project == rootProject) {
                displayName
            } else {
                "$displayName - ${project.name}"
            }

            name.convention(nameConvention)
            description.convention(provider { project.description })
            url.convention(githubUrl)

            licenses {
                license {
                    name = "MIT License"
                    url = "$githubUrl/blob/main/LICENSE"
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
                connection = "scm:git:$githubUrl.git"
                developerConnection = "scm:git:ssh://git@github.com/buijs-dev/$repository.git"
                url = githubUrl
            }
        }
    }
}
