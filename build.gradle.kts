val javaVersion = "11"

plugins {
    java
    idea
    eclipse
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
    api(":signal-cli") {
        exclude("com.github.bdeneuter", "dbus-java")
        exclude("org.freedesktop.dbus", "dbus-java")
    }
    // api("com.github.hypfvieh", "dbus-java", "3.2.0")
    api("com.github.bdeneuter", "dbus-java", "2.7")
    api("org.bouncycastle", "bcprov-jdk15on", "1.64")

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

idea {
    module {
        setDownloadJavadoc(true)
        setDownloadSources(true)
    }
}

eclipse {
    classpath {
        setDownloadJavadoc(true)
        setDownloadSources(true)
    }
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

    // https://stackoverflow.com/questions/52596968/build-source-jar-with-gradle-kotlin-dsl
    val sourcesJar by creating(Jar::class) {
        dependsOn(JavaPlugin.CLASSES_TASK_NAME)
        classifier = "sources"
        from(sourceSets["main"].allSource)
    }
    val javadocJar by creating(Jar::class) {
        dependsOn(JavaPlugin.JAVADOC_TASK_NAME)
        classifier = "javadoc"
        from(javadoc)
    }
    artifacts {
        add("archives", sourcesJar)
        add("archives", javadocJar)
    }
}
