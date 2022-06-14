plugins {
    val kotlinVersion = "1.7.0"
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

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")

    implementation("com.github.ajalt.clikt:clikt:3.3.0")

    // the apache client is the only one supporting redirects
    val ktorVersion = "1.6.7"
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-client-serialization-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-logging-jvm:$ktorVersion")
    implementation("org.slf4j:slf4j-simple:1.8.0-beta4")

    // necessary for encryption of secrets for GitHub
    implementation("com.goterl:lazysodium-java:5.1.1")
    implementation("net.java.dev.jna:jna:5.10.0")

    implementation("com.google.oauth-client:google-oauth-client-jetty:1.32.1")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "11"
}

tasks.create<Copy>("installOnLocalWindows") {
    group = "distribution"
    dependsOn("installDist")
    from("$buildDir/install/bob")
    into("${System.getenv("LOCALAPPDATA")}\\Bob")
}
