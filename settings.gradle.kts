import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

rootProject.name = "IntelliJ Platform Plugin Template"

pluginManagement {
    plugins {
        id("org.jetbrains.kotlin.jvm") version "2.1.20"
        id("org.jetbrains.changelog") version "2.5.0"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("org.jetbrains.intellij.platform.settings") version "2.16.0"
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    // Configure all projects' repositories
    repositories {
        // 本地 Maven 仓库优先：允许直接使用 D:\idea\jquick-java 执行 mvn install 后发布的本地构件，
        // 确保插件使用的 jquick-java 引擎与本地源码一致（避免等待 Central 发布的滞后）。
        mavenLocal()
        mavenCentral()

        // IntelliJ Platform Gradle Plugin Repositories Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
        intellijPlatform {
            defaultRepositories()
        }
    }
}
