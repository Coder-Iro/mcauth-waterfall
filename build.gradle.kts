plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "io.mrsmc"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
}

repositories {
    mavenCentral()
    maven {
        name = "sonatype"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
    maven {
        name = "destroystokyo-repo"
        url = uri("https://repo.destroystokyo.com/repository/maven-public/")
    }
}

dependencies {
    implementation("org.jetbrains:annotations:21.0.1")
    compileOnly("io.github.waterfallmc:waterfall-api:1.17-R0.1-SNAPSHOT")
    implementation("com.j256.two-factor-auth:two-factor-auth:1.3")
    implementation("redis.clients:jedis:3.6.3")
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    manifest {
        attributes["Main-Class"] = "io.mrsmc.mcauth.Mcauth"
    }
}