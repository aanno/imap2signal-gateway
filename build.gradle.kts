val javaVersion = "11"

plugins {
    java
    kotlin("jvm") version "1.3.70"
}

group = "com.github.aanno.imap2signal"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    api("com.sun.mail", "javax.mail", "1.6.2")
    api("de.swiesend", "secret-service", "1.0.0-RC.3")
    api("org.slf4j", "slf4j-api", "1.7.0")
    runtimeOnly("org.slf4j", "slf4j-jdk14", "1.7.0")

    // testApi("junit", "junit", "4.12")

    // JUnit 5
    testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.3.1")
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine" , "5.3.1")
    testRuntimeOnly("org.junit.platform", "junit-platform-console", "1.6.0")

    // Kotlintest
    testImplementation("io.kotlintest", "kotlintest-core", "3.4.2")
    testImplementation("io.kotlintest", "kotlintest-assertions", "3.4.2")
    testImplementation("io.kotlintest", "kotlintest-runner-junit5", "3.4.2")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_11
}
tasks {
    wrapper {
        distributionType = Wrapper.DistributionType.ALL
        version = "6.2.2"
    }
    compileKotlin {
        kotlinOptions.jvmTarget = javaVersion
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = javaVersion
    }
    // https://stackoverflow.com/questions/50128728/how-do-i-use-the-native-junit-5-support-in-gradle-with-the-kotlin-dsl
    // Use the built-in JUnit support of Gradle.
    "test"(Test::class) {
        useJUnitPlatform()
    }
}
