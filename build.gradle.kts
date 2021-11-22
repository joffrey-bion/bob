plugins {
    kotlin("jvm") version "1.4.10"
    kotlin("plugin.serialization") version "1.4.10"
    application
}

application {
    mainClass.set("org.hildan.github.secrets.wizard.MainKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.1")

    implementation("com.github.ajalt.clikt:clikt:3.0.0")

    // the apache client is the only one supporting redirects
    implementation("io.ktor:ktor-client-apache:1.4.0")
    implementation("io.ktor:ktor-client-serialization-jvm:1.4.0")
    implementation("io.ktor:ktor-client-logging-jvm:1.4.0")
    implementation("org.slf4j:slf4j-simple:1.8.0-beta4")

    // necessary for encryption of secrets for GitHub
    implementation("com.goterl.lazycode:lazysodium-java:4.3.0")
    implementation("net.java.dev.jna:jna:5.6.0")

    implementation("com.google.oauth-client:google-oauth-client-jetty:1.31.2")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "11"
}
