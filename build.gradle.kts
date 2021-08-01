plugins {
    kotlin("jvm") version "1.5.21"
}

group = "io.mrsmc"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.destroystokyo.com/repository/maven-public/")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.github.waterfallmc:waterfall-api:1.17-R0.1-SNAPSHOT")

    implementation("com.j256.two-factor-auth:two-factor-auth:1.3")
    implementation("redis.clients:jedis:3.6.3")
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = JavaVersion.VERSION_16.toString()
        targetCompatibility = JavaVersion.VERSION_16.toString()
        options.encoding = "x-windows-949"
    }
}