plugins {
  java
  id("com.gradleup.shadow") version "9.6.1"
  id("xyz.jpenilla.run-paper") version "3.1.0"
}

group = "net.labymod"

repositories {
  mavenCentral()
  maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
  maven("https://dist.labymod.net/api/v1/maven/release/")
}

dependencies {
  // Oldest supported server version. Only Bukkit API is used, so it runs on Spigot and Paper.
  compileOnly("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
  implementation("net.labymod.serverapi:server-bukkit:1.0.14") {
    // Voice chat and BetterPerspective integrations would auto-register via ServiceLoader
    exclude(group = "net.labymod.serverapi.integration")
  }

  testImplementation(platform("org.junit:junit-bom:5.13.4"))
  testImplementation("org.junit.jupiter:junit-jupiter")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")
  testImplementation("org.spigotmc:spigot-api:1.20.1-R0.1-SNAPSHOT")
}

tasks.test {
  useJUnitPlatform()
}

java {
  toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks.withType<JavaCompile>().configureEach {
  options.encoding = "UTF-8"
  // 1.20.1 servers may still run on Java 17
  options.release = 17
}

tasks.processResources {
  val props = mapOf("version" to project.version)
  inputs.properties(props)
  filesMatching("plugin.yml") {
    expand(props)
  }
}

tasks.shadowJar {
  archiveBaseName = "LabysWatchParty"
  archiveClassifier = ""
  // Own copy of the Server API, so an installed LabyModServerAPI plugin can't clash with ours
  relocate("net.labymod.serverapi", "net.labymod.watchparty.lib.serverapi")
  mergeServiceFiles()
  exclude("META-INF/maven/**")
}

tasks.jar {
  enabled = false
}

tasks.assemble {
  dependsOn(tasks.shadowJar)
}

tasks.runServer {
  minecraftVersion("26.2")
  javaLauncher = javaToolchains.launcherFor {
    languageVersion = JavaLanguageVersion.of(25)
  }
}
