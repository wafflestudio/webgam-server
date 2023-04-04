import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "3.0.1"
	id("io.spring.dependency-management") version "1.1.0"
	id("org.asciidoctor.jvm.convert") version "3.3.2"
	kotlin("jvm") version "1.8.0"
	kotlin("plugin.spring") version "1.8.0"
	kotlin("plugin.jpa") version "1.8.0"
	kotlin("plugin.serialization") version "1.5.0"
	kotlin("kapt") version "1.8.0"
}

group = "com.wafflestudio"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
	mavenCentral()
}

extra["spring-security.version"]="6.0.2"
extra["snippetsDir"] = file("build/generated-snippets")

dependencies {
	//implementation("org.springframework.security:spring-security-web:6.0.2")
	//implementation("org.springframework.security:spring-security-config:6.0.2")
	implementation("org.springframework:spring-messaging")
	implementation("org.springframework.security:spring-security-messaging:6.0.2")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa:3.0.1")
	implementation("org.springframework.boot:spring-boot-starter-data-redis:3.0.1")
	implementation("org.springframework.boot:spring-boot-starter-security:3.0.1")
	implementation("org.springframework.boot:spring-boot-starter-web:3.0.1")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.0")
	implementation("org.jetbrains.kotlin:kotlin-reflect:1.7.22")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.22")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
	implementation("com.google.code.gson:gson:2.10.1")
	implementation("io.jsonwebtoken:jjwt-api:0.11.5")
	implementation("io.hypersistence:hypersistence-utils-hibernate-60:3.0.1")
	implementation("com.querydsl:querydsl-jpa:5.0.0:jakarta")
	implementation("software.amazon.awssdk:secretsmanager:2.20.5")
	implementation("software.amazon.awssdk:sts:2.20.7")
	implementation("org.springframework.boot:spring-boot-starter-websocket")
	implementation("org.webjars:webjars-locator-core") // TODO delete unnecessary
	implementation("org.webjars:sockjs-client:1.0.2")
	implementation("org.webjars:stomp-websocket:2.3.3")
	implementation("org.webjars:bootstrap:3.4.0")
	implementation("org.webjars:jquery:3.6.2")
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect")
	kapt("com.querydsl:querydsl-apt:5.0.0:jakarta")
	runtimeOnly("com.mysql:mysql-connector-j:8.0.31")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
	runtimeOnly("io.jsonwebtoken:jjwt-gson:0.11.5")
	testImplementation("org.springframework.boot:spring-boot-starter-test:3.0.1")
	testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc:3.0.0")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
	testImplementation("io.kotest:kotest-runner-junit5:5.5.4")
	testImplementation("io.kotest:kotest-assertions-core:5.5.4")
	testImplementation("io.kotest.extensions:kotest-extensions-spring:1.1.2")
	testImplementation("io.mockk:mockk:1.13.2")
	testImplementation("com.ninja-squad:springmockk:4.0.0")
	testImplementation("com.h2database:h2:2.1.214")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict")
		jvmTarget = "17"
	}
}

tasks {
	val snippetsDir = file("$buildDir/generated-snippets")

	clean {
		delete("src/main/resources/static/docs")
	}

	test {
		useJUnitPlatform()
		systemProperty("org.springframework.restdocs.outputDir", snippetsDir)
		outputs.dir(snippetsDir)
	}

	build {
		dependsOn("copyDocument")
	}

	asciidoctor {
		dependsOn(test)

		attributes(
			mapOf("snippets" to snippetsDir)
		)
		inputs.dir(snippetsDir)

		baseDirFollowsSourceFile()

		sources {
			include("**/index.adoc","**/common/*.adoc")
		}

		doFirst {
			delete("src/main/resources/static/docs")
		}
	}

	register<Copy>("copyDocument") {
		dependsOn(asciidoctor)

		destinationDir = file(".")
		from(asciidoctor.get().outputDir) {
			into("src/main/resources/static/docs")
		}
	}

	bootJar {
		dependsOn(asciidoctor)

		from(asciidoctor.get().outputDir) {
			into("BOOT-INF/classes/static/docs")
		}

		duplicatesStrategy = DuplicatesStrategy.INCLUDE
	}
}