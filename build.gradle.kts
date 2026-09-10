import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
}

// 2024.1（build 241）的最低运行时为 Java 17；编译到 17 字节码可同时兼容 2024.1 及更高版本
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
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
        intellijIdea("2024.1")
        testFramework(TestFrameworkType.Platform)
    }
}
intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            // 最低支持 2024.1（build 241）
            sinceBuild.set("241")
            // 不设置上限：2024.1 及以上（含未来版本）均可安装
            untilBuild.set(provider { null })
        }
    }
}