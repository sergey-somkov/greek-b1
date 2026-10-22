// app/ is a self-contained build with a single project: no nested app/app module.
// content/ and tools/ in the repository root stay outside the Gradle build.
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "greek-b1"
