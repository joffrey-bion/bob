plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
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
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.clikt)
    implementation(libs.kaml)

    // the apache client is the only one supporting redirects
    implementation(libs.ktor.client.apache)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.slf4j.simple)

    // necessary for encryption of secrets for GitHub
    implementation(libs.lazysodium.java)
    implementation(libs.jna)
}

tasks.register<Copy>("installOnLocalWindows") {
    group = "distribution"
    dependsOn("installDist")
    from(layout.buildDirectory.dir("install/bob"))
    into("${System.getenv("LOCALAPPDATA")}\\Bob")
}
