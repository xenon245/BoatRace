plugins {
    kotlin("jvm") version "1.5.20"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    `maven-publish`
}

val relocate = (findProperty("relocate") as? String)?.toBoolean() ?: true

group = "com.github.xenon"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://jitpack.io/")
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly("com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.5")
    implementation("com.github.monun:tap:+")
    implementation("com.github.monun:kommand:0.8.1")
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }
    processResources {
        filesMatching("**/*.yml") {
            expand(project.properties)
        }
    }
    test {
        useJUnitPlatform()
        doLast {
            file("logs").deleteRecursively()
        }
    }
    create<Jar>("sourcesJar") {
        from(sourceSets["main"].allSource)
        archiveClassifier.set("sources")
    }
    shadowJar {
        archiveBaseName.set(project.property("pluginName").toString())
        archiveVersion.set("")
        archiveClassifier.set("")
        if (relocate) {
            relocate("com.github.monun.kommand", "${rootProject.group}.${rootProject.name}.kommand")
            relocate("com.github.monun.tap", "${rootProject.group}.${rootProject.name}.tap")
            relocate("it.unimi.dsi", "${rootProject.group}.${rootProject.name}.it.unimi.dsi")
        }

        doFirst {
            println("relocate = $relocate")
        }
    }
    build {
        dependsOn(shadowJar)
    }
    create<Copy>("copyToServer") {
        from(shadowJar)
        val plugins = File(rootDir, ".server/plugins")
        if(File(plugins, shadowJar.get().archiveFileName.get()).exists()) {
            into(File(plugins, "update"))
        } else {
            into(plugins)
        }
    }
}

publishing {
    publications {
        create<MavenPublication>(project.property("pluginName").toString()) {
            artifactId = project.name
            from(components["java"])
            artifact(tasks["sourcesJar"])
        }
    }
}