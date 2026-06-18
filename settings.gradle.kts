pluginManagement {
    repositories {
        maven {
            url = uri("http://192.168.1.6:9081/repository/maven-public/")
            isAllowInsecureProtocol = true
        }

        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven {
            url = uri("http://192.168.1.6:9081/repository/maven-public/")
            isAllowInsecureProtocol = true
        }

        google()
        mavenCentral()
    }
}

rootProject.name = "SKAT-Load-cell"
include(":app")
 