plugins {
    kotlin("jvm") version "1.8.21"
    kotlin("plugin.serialization") version "1.8.21"
    `maven-publish`
}

group = "dev.bright"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.5.0")

    // TODO: figure out if we can get rid of this
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.8.21")

    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:1.5.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:1.5.0")

    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.5.5")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])
        }
    }
}
