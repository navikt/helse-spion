import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktorVersion = "1.2.4"
val logback_version = "1.2.1"
val jacksonVersion = "2.9.9"
val prometheusVersion = "0.6.0"
val flywayVersion = "6.0.8"
val hikariVersion = "3.3.1"
val vaultJdbcVersion = "1.3.1"
val kafkaVersion = "2.0.1"


val mainClass = "no.nav.helse.spion.web.AppKt"
val junitJupiterVersion = "5.5.0-RC2"
val assertJVersion = "3.12.2"
val mockKVersion = "1.9.3"
val tokenSupportVersion = "1.1.3"



plugins {
    kotlin("jvm") version "1.3.50"
    `build-scan`
}

buildscript {
    dependencies {
        classpath("org.junit.platform:junit-platform-gradle-plugin:1.2.0")
    }
}

dependencies {
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")

    implementation("no.nav.security:token-validation-ktor:$tokenSupportVersion")

    implementation(kotlin("stdlib"))

    implementation("ch.qos.logback:logback-classic:$logback_version")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.9.+")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    implementation("org.apache.kafka:kafka-streams:$kafkaVersion")

    implementation("org.flywaydb:flyway-core:$flywayVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("no.nav:vault-jdbc:$vaultJdbcVersion")

    implementation("io.prometheus:simpleclient_common:$prometheusVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")

    testImplementation("io.mockk:mockk:$mockKVersion")
    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testImplementation("org.assertj:assertj-core:$assertJVersion")

    testRuntime("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
}

tasks.named<KotlinCompile>("compileKotlin")

tasks.named<KotlinCompile>("compileKotlin") {
    kotlinOptions.jvmTarget = "11"
}

tasks.named<KotlinCompile>("compileTestKotlin") {
    kotlinOptions.jvmTarget = "11"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    jcenter()
    mavenCentral()
    maven("https://kotlin.bintray.com/ktor")
}

tasks.named<Jar>("jar") {
    baseName = ("app")

    manifest {
        attributes["Main-Class"] = mainClass
        attributes["Class-Path"] = configurations.runtimeClasspath.get().joinToString(separator = " ") {
            it.name
        }
    }

    doLast {
        configurations.runtimeClasspath.get().forEach {
            val file = File("$buildDir/libs/${it.name}")
            if (!file.exists())
                it.copyTo(file)
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.withType<Wrapper> {
    gradleVersion = "5.6.2"
}