plugins {
    id("fabric-loom") version "1.7-SNAPSHOT"
}

base {
    archivesName = project.property("archives_base_name").toString()
    version = project.property("mod_version").toString()
    group = project.property("maven_group").toString()
}

repositories {
    maven("https://maven.meteordev.org/releases") {
        name = "Meteor Dev Releases"
    }
    maven("https://maven.meteordev.org/snapshots") {
        name = "Meteor Dev Snapshots"
    }
}

dependencies {
    // Fabric
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${project.property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")

    // Meteor
    modImplementation("meteordevelopment:meteor-client:${project.property("meteor_version")}") {
        isChanging = true // this makes sure the meteor version is always the latest possible, set this to false if you want lower network usage
    }
}

tasks {
    processResources {
        val projectProperties = mapOf(
            "version" to project.version,
            "mc_version" to project.property("minecraft_version"),
            "commit" to (project.findProperty("commit") ?: "")
        )

        inputs.properties(projectProperties)

        filesMatching("fabric.mod.json") {
            expand(projectProperties) {
                escapeBackslash = true
            }
        }
    }

    jar {
        from("LICENSE") {
            rename { "${it}_${project.base.archivesName.get()}" }
        }
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release = 21
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21

        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }
}
