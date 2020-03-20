val kotlinVersion = "1.3.70"

pluginManagement {
    repositories {
        jcenter()
        gradlePluginPortal()
    }
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

