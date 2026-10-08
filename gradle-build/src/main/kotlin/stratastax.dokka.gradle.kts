plugins {
    id("stratastax.repositories")
    id("org.jetbrains.dokka")
}

val repository = providers.gradleProperty("stratastax.repository").get()

dokka {
    val name = if (project == rootProject) {
        providers.gradleProperty("stratastax.displayName").get()
    } else {
        project.name
    }

    moduleName.set(name)
    dokkaSourceSets.configureEach {
        sourceLink {
            localDirectory.set(rootDir)
            remoteUrl("https://github.com/buijs-dev/$repository/tree/main")
            remoteLineSuffix.set("#L")
        }
    }
}
