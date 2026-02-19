plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.spring") version "2.2.0"
    kotlin("plugin.jpa") version "2.2.0"
    kotlin("plugin.allopen") version "2.2.0"
    id("org.springframework.boot") version "4.1.0-SNAPSHOT"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
    jacoco
}

group = "com.mafauser"
version = "0.0.1-SNAPSHOT"
description = "Default Backend Service Initializer"

defaultTasks("build")

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/snapshot") }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-graphql")
    implementation("org.springframework.boot:spring-boot-starter-kafka")
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("com.bucket4j:bucket4j_jdk17-core:8.16.1")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-cache-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-graphql-test")
    testImplementation("org.springframework.boot:spring-boot-starter-kafka-test")
    testImplementation("org.springframework.boot:spring-boot-starter-opentelemetry-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework:spring-webflux")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.2.3")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-kafka")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

// JPA plugin provides noarg for @Entity; allOpen required for Hibernate proxies
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

// Debug agent for bootRun: add only when -Pdebug=true AND Gradle extension hasn't
// already injected jdwp via JAVA_TOOL_OPTIONS (avoid "Cannot load JVM TI agent twice").
// Use -PdebugPort=5006 if 5005 is in use.
tasks.bootRun {
    val javaToolOptions = System.getenv("JAVA_TOOL_OPTIONS") ?: ""
    val extensionProvidesDebug = javaToolOptions.contains("jdwp")
    val enableDebug = project.findProperty("debug") == "true" && !extensionProvidesDebug
    if (enableDebug) {
        val port = project.findProperty("debugPort") ?: "5005"
        val suspend = if (project.findProperty("debugSuspend") == "n") "n" else "y"
        jvmArgs =
            listOf(
                "-agentlib:jdwp=transport=dt_socket,server=y,suspend=$suspend,address=*:$port",
            )
    }
}

// Used by Kotlin Language Server (e.g. kls-classpath script) to resolve dependencies in the IDE.
// Use test compileClasspath so both main and test sources resolve (test classpath includes main).
tasks.register("printClasspath") {
    val cp = sourceSets["test"].compileClasspath
    doLast {
        println(cp.asPath)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
    testLogging {
        events("passed", "skipped", "failed")
        showExceptions = true
        showStackTraces = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        displayGranularity = 0
    }
}

val jacocoExcludes =
    listOf(
        "**/Application.class",
        "**/ApplicationKt.class",
        "**/com/mafauser/service/Application*.class",
        "**/*Entity.class",
        "**/entity/**/*.class",
        "**/Example.class",
    )

// JaCoCo: report after test; exclude main entry, entities, and generated/config
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    classDirectories.setFrom(
        sourceSets.main.get().output.classesDirs.files.map { dir ->
            fileTree(dir) { exclude(jacocoExcludes) }
        },
    )
}

// JaCoCo: coverage verification (90% line minimum). Run explicitly or from CI job; not part of build/check.
tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    classDirectories.setFrom(
        sourceSets.main.get().output.classesDirs.files.map { dir ->
            fileTree(dir) { exclude(jacocoExcludes) }
        },
    )
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.90".toBigDecimal()
            }
        }
    }
}

// ktlint configuration
ktlint {
    version.set("1.8.0")
    android.set(false)
    ignoreFailures.set(false)
    reporters {
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
        reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
    }
}

// Run ktlint check before build
tasks.named("check") {
    dependsOn("ktlintCheck")
}

// Make build fail if ktlint fails
tasks.withType<org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask> {
    mustRunAfter(tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>())
}
