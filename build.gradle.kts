import org.gradle.api.JavaVersion.VERSION_21

plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.3.3" // Aligned with cf-lob-platform
	id("io.spring.dependency-management") version "1.1.7"
}

group = "tech.edgx"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
	sourceCompatibility = VERSION_21
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.3") // Align with cf-lob-platform
		mavenBom("org.jmolecules:jmolecules-bom:2023.1.0") // Match cf-lob-platform
	}
}

repositories {
	mavenLocal()
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa") // Add for JPA repositories
	implementation("io.hypersistence:hypersistence-utils-hibernate-63:3.10.2")
	implementation("org.springframework:spring-aspects") // For JPA auditing
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.springframework.boot:spring-boot-starter-validation") // Add for Validator
	implementation("org.springframework.boot:spring-boot-starter-oauth2-client") // Add for Keycloak
	implementation("org.springframework.security:spring-security-oauth2-resource-server")

	val REEVE_VERSION = "1.0.1"
	implementation("org.cardanofoundation:cf-lob-platform-blockchain_common:${REEVE_VERSION}")
	implementation("org.cardanofoundation:cf-lob-platform-blockchain_publisher:${REEVE_VERSION}")
	implementation("org.cardanofoundation:cf-lob-platform-blockchain_reader:${REEVE_VERSION}")
	implementation("org.cardanofoundation:cf-lob-platform-organisation:${REEVE_VERSION}")
	implementation("org.cardanofoundation:cf-lob-platform-support:${REEVE_VERSION}")
	implementation("org.cardanofoundation:cf-lob-platform-accounting_reporting_core:${REEVE_VERSION}")
	implementation("org.cardanofoundation:cf-lob-platform-netsuite_altavia_erp_adapter:${REEVE_VERSION}")
	implementation("org.cardanofoundation:cf-lob-platform-notification_gateway:${REEVE_VERSION}")

	implementation("com.bloxbean.cardano:cardano-client-crypto:0.6.0")
	implementation("com.bloxbean.cardano:cardano-client-backend-blockfrost:0.6.0")
	implementation("com.bloxbean.cardano:cardano-client-quicktx:0.6.0")

	// Javers for auditing
	implementation("org.javers:javers-core:7.6.1")

	// jMolecules dependencies
	implementation("org.jmolecules:jmolecules-ddd")
	implementation("org.jmolecules:jmolecules-events") // Comment out if not needed

	implementation("org.springframework:spring-aspects") // Add for JPA auditing

	implementation("org.mockito:mockito-core:5.14.2") // Add for mocking in production

	implementation("org.zalando:problem-spring-web-starter:0.29.1")
	implementation("com.google.guava:guava:33.3.0-jre")
	implementation("org.apache.commons:commons-collections4:4.4")
	implementation("io.vavr:vavr:0.10.4")
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