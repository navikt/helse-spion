import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktorVersion = "1.5.0"
val logback_version = "1.2.1"
val logback_contrib_version = "0.1.5"
val jacksonVersion = "2.11.0"
val prometheusVersion = "0.6.0"
val hikariVersion = "3.3.1"
val vaultJdbcVersion = "1.3.1"
val kafkaVersion = "2.1.1"
val mainClass = "no.nav.helse.spion.web.AppKt"
val junitJupiterVersion = "5.5.0-RC2"
val assertJVersion = "3.12.2"
val mockKVersion = "1.11.0"
val tokenSupportVersion = "1.3.1"
val mockOAuth2ServerVersion = "0.2.1"
val koinVersion = "3.1.2"
val valiktorVersion = "0.9.0"
val cxfVersion = "3.3.8"
val jaxwsVersion = "2.3.1"
val jaxwsToolsVersion = "2.3.1"

val githubPassword: String by project

plugins {
    kotlin("jvm") version "1.4.0"
    id("org.sonarqube") version "2.8"
    id("org.jlleitschuh.gradle.ktlint") version "10.2.0"
    id("com.github.ben-manes.versions") version "0.27.0"
    jacoco
}

sonarqube {
    properties {
        property("sonar.projectKey", "navikt_helse-spion")
        property("sonar.organization", "navit")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.login", System.getenv("SONAR_TOKEN"))
        property("sonar.exclusions", "**/Koin*,**Mock**,**/App**")
    }
}

tasks.jacocoTestReport {
    executionData("build/jacoco/test.exec", "build/jacoco/slowTests.exec")
    reports {
        xml.isEnabled = true
        html.isEnabled = true
    }
}

tasks.withType<JacocoReport> {
    classDirectories.setFrom(
        sourceSets.main.get().output.asFileTree.matching {
            exclude("**/Koin**", "**/App**", "**Mock**")
        }
    )
}

buildscript {
    dependencies {
        classpath("org.junit.platform:junit-platform-gradle-plugin:1.2.0")
    }
}

dependencies {
    constraints {
        implementation("com.nimbusds:nimbus-jose-jwt") {
            version {
                strictly("8.20.1")
            }
            because("JWT validation")
        }
    }

    // Snyk fikser
    implementation("org.eclipse.jetty:jetty-server:9.4.35.v20201120") // overstyrer
    implementation("org.apache.httpcomponents:httpclient:4.5.13") // overstyrer transiente 4.5.6 via ktor-client-apache
    implementation("org.glassfish.jersey.media:jersey-media-jaxb:2.31") // overstyrer transiente 2.30.1
    implementation("org.yaml:snakeyaml:1.26")
    implementation("junit:junit:4.13.1") // overstyrer transiente 4.12
    implementation("com.google.guava:guava:30.0-jre") // overstyrer transiente 29.0-jre
    implementation("io.netty:netty-codec:4.1.59.Final") // overstyrer transiente 4.1.44
    implementation("io.netty:netty-codec-http:4.1.59.Final") // overstyrer transiente 4.1.51.Final gjennom ktor-server-netty    
    // Snyk fikser slutt

    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-auth:$ktorVersion")
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("io.ktor:ktor-client-json:$ktorVersion")
    implementation("io.ktor:ktor-client-jackson:$ktorVersion")
    implementation("io.ktor:ktor-locations:$ktorVersion")

    implementation("org.valiktor:valiktor-core:$valiktorVersion")
    implementation("org.valiktor:valiktor-javatime:$valiktorVersion")
    implementation("no.nav.common:log:2.2021.01.05_08.07-2c586ccadf95")

    implementation("no.nav.tjenestespesifikasjoner:altinn-correspondence-agency-external-basic:1.2019.09.25-00.21-49b69f0625e0")

    implementation("javax.xml.ws:jaxws-api:$jaxwsVersion")
    implementation("org.apache.cxf:cxf-rt-frontend-jaxws:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-features-logging:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-transports-http:$cxfVersion")
    implementation("org.apache.cxf:cxf-rt-ws-security:$cxfVersion") {
        exclude(group = "commons-collections", module = "commons-collections")
    }
    implementation("org.apache.ws.xmlschema:xmlschema-core:2.2.4") // Force newer version of XMLSchema to fix illegal reflective access warning
    implementation("com.sun.xml.ws:jaxws-tools:$jaxwsToolsVersion") {
        exclude(group = "com.sun.xml.ws", module = "policy")
    }

    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("io.insert-koin:koin-ktor:$koinVersion")
    testImplementation("io.insert-koin:koin-test:$koinVersion")
    implementation("no.nav.security:token-client-core:$tokenSupportVersion")
    implementation("no.nav.security:token-validation-ktor:$tokenSupportVersion")
    implementation("no.nav.security:mock-oauth2-server:$mockOAuth2ServerVersion")

    implementation(kotlin("stdlib"))

    implementation("org.slf4j:slf4j-api:1.7.30")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("ch.qos.logback.contrib:logback-jackson:$logback_contrib_version")
    implementation("ch.qos.logback.contrib:logback-json-classic:$logback_contrib_version")
    implementation("net.logstash.logback:logstash-logback-encoder:4.9")
    implementation("org.codehaus.janino:janino:3.0.6")
    implementation("org.flywaydb:flyway-core:7.3.0")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.+")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")

    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("no.nav:vault-jdbc:$vaultJdbcVersion")
    implementation("com.github.tomakehurst:wiremock-standalone:2.25.1")
    implementation("org.postgresql:postgresql:42.2.13")

    implementation("io.prometheus:simpleclient_common:$prometheusVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")

    implementation("no.nav.helsearbeidsgiver:helse-arbeidsgiver-felles-backend:2021.08.27-15-00-1d672")

    implementation("com.github.javafaker:javafaker:1.0.1") // flytt denne til test når generatorene ikke er nødvendige i prod-koden lenger
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.3.2")
    testImplementation("io.mockk:mockk:$mockKVersion")
    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
    testImplementation("org.assertj:assertj-core:$assertJVersion")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testImplementation("io.ktor:ktor-client-mock-jvm:$ktorVersion")
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
    mavenCentral()
    google()
    maven(url = "https://packages.confluent.io/maven/")
    maven {
        credentials {
            username = "x-access-token"
            password = githubPassword
        }
        setUrl("https://maven.pkg.github.com/navikt/inntektsmelding-kontrakt")
    }
    maven {
        credentials {
            username = "x-access-token"
            password = githubPassword
        }
        setUrl("https://maven.pkg.github.com/navikt/helse-arbeidsgiver-felles-backend")
    }
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
        showStackTraces = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.named<Test>("test") {
    include("no/nav/helse/**")
    exclude("no/nav/helse/slowtests/**")
}

task<Test>("slowTests") {
    include("no/nav/helse/slowtests/**")
    outputs.upToDateWhen { false }
}

tasks.withType<Wrapper> {
    gradleVersion = "6.0.1"
}
