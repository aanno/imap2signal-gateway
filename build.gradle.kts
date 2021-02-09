val javaVersion = "11"

plugins {
    id("org.javamodularity.moduleplugin") version "1.7.0"
    `java-library`
    `maven-publish`
    idea
    eclipse
    kotlin("jvm") version "1.4.30"
    `java-library-distribution`

    // Plugin which checks for dependency updates with help/dependencyUpdates task.
    id("com.github.ben-manes.versions") version "0.36.0"
    // Plugin which can update Gradle dependencies, use help/useLatestVersions
    id("se.patrikerdes.use-latest-versions") version "0.2.15"
}

group = "com.github.aanno.imap2signal"
version = "0.0.2-SNAPSHOT"

val otherdbus by configurations.creating

repositories {
    mavenCentral()
    flatDir {
        dirs(
            "submodules/secret-service/target"
            , "submodules/signal-cli/build/libs"
            , "submodules/signal-cli/lib/build/libs"
        )
    }
}

configurations {
    all {
        resolutionStrategy {
            preferProjectModules()
            setForcedModules(
                "com.sun.mail:jakarta.mail:1.6.5"
                , "org.ow2.asm:asm:9.1"
                , "org.ow2.asm:asm-commons:9.1"
                , "org.ow2.asm:asm-analysis:9.1"
                , "org.ow2.asm:asm-tree:9.1"
                , "org.ow2.asm:asm-util:9.1"
            )
        }
        exclude("com.sun.mail", "javax.mail")
    }
}

dependencies {
    // force new asm version
    // implementation("org.ow2.asm:asm-commons:9.1")
    // implementation("org.ow2.asm:asm-util:9.1")

    implementation(kotlin("stdlib-jdk8"))
    // api("com.sun.mail", "javax.mail", "1.6.2")
    implementation("com.sun.mail:jakarta.mail:1.6.5")
    // api("jakarta.mail:jakarta.mail-api:1.6.5")
    // api("de.swiesend", "secret-service", "1.0.0-RC.3")
    // does not work: not gradle but mvn!
    // api(":secret-service")
    api("", "secret-service", "1.4.0")
    api("org.slf4j", "slf4j-api", "1.7.30")
    implementation("org.slf4j", "slf4j-simple", "1.7.30")

    implementation("", "signal-cli", "0.7.4") {
        exclude("com.github.bdeneuter", "dbus-java")
        exclude("org.freedesktop.dbus", "dbus-java")
    }
    implementation("", "lib", "0.7.4") {
        exclude("com.github.bdeneuter", "dbus-java")
        exclude("org.freedesktop.dbus", "dbus-java")
    }

    // TODO aanno:
    // These are the transitive dependencies for 'signal-cli' and 'lib'
    implementation("org.whispersystems", "signal-client-java", "0.2.3")
    api("com.github.turasa:signal-service-java:2.15.3_unofficial_18")
    implementation("com.google.protobuf:protobuf-javalite:3.10.0")
    implementation("org.bouncycastle:bcprov-jdk15on:1.68")
    // TODO aanno: if included => error: module not found: slf4j.api
    // implementation("org.slf4j:slf4j-api:1.7.30")

    // ATTENTION: deps of signal-cli (UPDATED)
    // api("com.squareup.okio", "okio", "1.17.5")
    // api("com.squareup.okhttp3", "okhttp", "3.14.7")

    // api("com.github.hypfvieh", "dbus-java", "3.2.0")
    // otherdbus("com.github.hypfvieh", "dbus-java", "3.2.0")
    /// api("com.github.bdeneuter", "dbus-java", "2.7")
    api("com.github.hypfvieh", "dbus-java", "3.2.4")

    api("com.github.marlonlom", "timeago", "4.0.3")
    implementation("com.google.guava", "guava", "28.2-jre")
    api("com.google.code.findbugs", "jsr305", "3.0.2")

    // ??? (from maven?)
    api("at.favre.lib", "hkdf", "1.1.0")

    // https://github.com/bbottema/email-rfc2822-validator
    implementation("com.github.bbottema", "emailaddress-rfc2822", "2.1.4")

    // runtimeOnly("org.slf4j", "slf4j-jdk14", "1.7.0")

    // testApi("junit", "junit", "4.12")

    // JUnit 5
    testImplementation("org.junit.jupiter", "junit-jupiter-api", "5.7.1")
    testRuntimeOnly("org.junit.jupiter", "junit-jupiter-engine", "5.7.1")
    testRuntimeOnly("org.junit.platform", "junit-platform-console", "1.7.1")

    // Kotlintest
    testImplementation("io.kotlintest", "kotlintest-core", "3.4.2")
    testImplementation("io.kotlintest", "kotlintest-assertions", "3.4.2")
    testImplementation("io.kotlintest", "kotlintest-runner-junit5", "3.4.2")
}

java {
    withJavadocJar()
    withSourcesJar()
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
    withType<JavaCompile> {
        doFirst {
            options.compilerArgs.addAll(listOf(
                "--release", "11",
                "-deprecation", "-Xlint:all,-serial"
                // "--add-exports=java.xml/com.sun.org.apache.xerces.internal.parsers=com.github.aanno.dbtoolchain"
                // , "--add-modules jnr.enxio"
                // , "-cp", "jnr-enxio-0.19.jar"
                // , "--add-modules", "ALL-MODULE-PATH",
                // , "--module-path", classpath.asPath
            ) /* + moduleJvmArgs + patchModule */)
            println("Args for for ${name} are ${options.allCompilerArgs}")
        }
    }
    wrapper {
        distributionType = Wrapper.DistributionType.ALL
        version = "6.8.2"
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

    jar {
        manifest {
            attributes(
                mapOf(
                    // "ModuleMainClass" to "com.github.aanno.imap2signal.MailFetch",
                    // "MainClass" to "com.github.aanno.imap2signal.MailFetch",
                    "Main-Class" to "com.github.aanno.imap2signal.MailFetch"
                    /* with java 11 module path, the following will NOT work
                    , "Class-Path" to configurations.runtimeClasspath
                        .get().map{ file -> "lib/" + file.getName()}
                        .joinToString(" ")
                        */
                )
            )
        }

        dependsOn("generatePomFileForMavenPublication")
        // from("$buildDir/publications/maven/pom-default.xml")
        // into("META-INF/maven/${group}/${name}")

        into("META-INF/maven/${project.group}/${project.name}") {
            from("$buildDir/publications/maven/pom-default.xml")
            rename(".*", "pom.xml")
        }
    }
    val copyOtherDBus by register<Copy>("copyOtherDBus") {
        from(otherdbus)
        into("$projectDir/otherdbus")
    }
    distributions {
        main {
            contents {
                into("otherdbus") {
                    from("$projectDir/otherdbus")
                }
            }
        }
    }
}



publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}


tasks.named("build") {
    dependsOn("copyOtherDBus")
}
