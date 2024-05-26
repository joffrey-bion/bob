plugins {
    val kotlinVersion = "2.0.0"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    application
}

application {
    mainClass.set("org.hildan.bob.MainKt")
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation("com.github.ajalt.clikt:clikt:4.4.0")
    implementation("com.charleskorn.kaml:kaml:0.59.0")

    // the apache client is the only one supporting redirects
    val ktorVersion = "2.3.11"
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-logging-jvm:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    implementation("org.slf4j:slf4j-simple:2.1.0-alpha1")

    // necessary for encryption of secrets for GitHub
    implementation("com.goterl:lazysodium-java:5.1.4")
    implementation("net.java.dev.jna:jna:5.14.0")
}

tasks.create<Copy>("installOnLocalWindows") {
    group = "distribution"
    dependsOn("installDist")
    from(layout.buildDirectory.dir("install/bob"))
    into("${System.getenv("LOCALAPPDATA")}\\Bob")
}
