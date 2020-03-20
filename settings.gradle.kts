val kotlinVersion = "1.3.70"

pluginManagement {
    repositories {
        jcenter()
        gradlePluginPortal()
    }
    // https://stackoverflow.com/questions/52800536/how-to-use-plugin-version-from-gradle-properties-in-gradle-kotlin-dsl
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                /*
                "org.jetbrains.kotlin.plugin.allopen" -> {
                    val kotlinVersion: String by settings
                    useVersion(kotlinVersion)
                }
                 */
                "org.jetbrains.kotlin.jvm" -> {
                    val kotlinVersion: String by settings
                    useVersion(kotlinVersion)
                }
            }
        }
    }
}

rootProject.name = "imap2signal-gateway"

includeBuild("signal-cli")
