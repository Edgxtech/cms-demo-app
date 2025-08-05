import org.gradle.api.JavaVersion.VERSION_21

plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    kotlin("plugin.jpa") version "1.9.25"
    id("org.springframework.boot") version "3.3.0" // Aligned with cf-lob-platform background follower app
    id("io.spring.dependency-management") version "1.1.6"
    id("org.graalvm.buildtools.native") version "0.9.26"
    id("com.github.ben-manes.versions") version "0.48.0"
}

group = "tech.edgx"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    sourceCompatibility = VERSION_21
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}
dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    testImplementation("io.rest-assured:rest-assured:5.4.0")
    testImplementation("org.wiremock:wiremock-standalone:3.9.1")

    implementation("org.springframework.boot:spring-boot-starter-cache")
    testCompileOnly("org.springframework.boot:spring-boot-starter-test")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")

    testImplementation("org.springframework.boot:spring-boot-starter-test")

    runtimeOnly("org.springframework.boot:spring-boot-properties-migrator")

    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    implementation("org.zalando:problem-spring-web-starter:0.29.1")

    val lombokVersion = "1.18.34"
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")

    testCompileOnly("org.projectlombok:lombok:$lombokVersion")
    testAnnotationProcessor("org.projectlombok:lombok:$lombokVersion")

    implementation("com.querydsl:querydsl-jpa")
    annotationProcessor("com.querydsl:querydsl-apt")

    val cardanoClientVersion = "0.6.0"
    implementation("com.bloxbean.cardano:cardano-client-crypto:$cardanoClientVersion")
    implementation("com.bloxbean.cardano:cardano-client-backend-blockfrost:$cardanoClientVersion")

    implementation("io.blockfrost:blockfrost-java:0.1.3")

    implementation("io.vavr:vavr:0.10.4")

    implementation("org.cardano.foundation:cf-lob-ledger-follower-app:1.0.1-SNAPSHOT")

    // yaci-store dependencies
    implementation("com.bloxbean.cardano:yaci-store-spring-boot-starter:0.1.0-rc5")
    implementation("com.bloxbean.cardano:yaci-store-blocks-spring-boot-starter:0.1.0-rc5")
    implementation("com.bloxbean.cardano:yaci-store-transaction-spring-boot-starter:0.1.0-rc5")
    implementation("com.bloxbean.cardano:yaci-store-metadata-spring-boot-starter:0.1.0-rc5")

    runtimeOnly("org.postgresql:postgresql")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    implementation("me.paulschwarz:spring-dotenv:4.0.0")

    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.25")
}
kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks {
    val ENABLE_PREVIEW = "--enable-preview"
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.add(ENABLE_PREVIEW)
    }
    withType<Test> {
        useJUnitPlatform()
        jvmArgs(ENABLE_PREVIEW) // For tests
    }
    withType<org.springframework.boot.gradle.tasks.run.BootRun> {
        jvmArgs(ENABLE_PREVIEW) // For bootRun
    }
}