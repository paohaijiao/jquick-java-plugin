import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}
dependencies {
    testImplementation("junit:junit:4.13.2")
    implementation("io.github.paohaijiao:jquick-java:2.6.0")
    // jquick 语法基于 ANTLR4 生成，须显式携带与 jquick-java 一致的运行时（4.13.2）
    implementation("org.antlr:antlr4-runtime:4.13.2")

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        intellijIdea("2025.2.6.2")
        testFramework(TestFrameworkType.Platform)
    }
}
