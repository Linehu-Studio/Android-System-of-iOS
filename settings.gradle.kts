pluginManagement {
    repositories {
        // NOTE: aliyun's gradle-plugin mirror intermittently 502s on some
        // artifacts (observed with KSP), which aborts plugin resolution —
        // so it is deliberately NOT listed here.
        mavenCentral()
        google()
        maven("https://maven.aliyun.com/repository/public")
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/google")
        google()
        mavenCentral()
    }
}

rootProject.name = "ASI"
include(":app")
