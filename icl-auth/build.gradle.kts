/*
 * Copyright 2026 Open Health Stack Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
import java.security.MessageDigest

abstract class GenerateMavenCentralChecksumsTask : DefaultTask() {
  @get:InputDirectory abstract val repositoryDirectory: DirectoryProperty

  @TaskAction
  fun generate() {
    val repositoryRoot = repositoryDirectory.get().asFile
    if (!repositoryRoot.exists()) {
      error("Maven Central staging repository does not exist: ${repositoryRoot.absolutePath}")
    }

    repositoryRoot
      .walkTopDown()
      .filter(File::isFile)
      .filterNot { it.name.startsWith("maven-metadata") }
      .filterNot { it.extension in setOf("asc", "md5", "sha1", "sha256", "sha512") }
      .forEach { file ->
        writeChecksum(file, "MD5", "md5")
        writeChecksum(file, "SHA-1", "sha1")
        writeChecksum(file, "SHA-256", "sha256")
        writeChecksum(file, "SHA-512", "sha512")
      }
  }

  private fun writeChecksum(file: File, algorithm: String, extension: String) {
    file.resolveSibling("${file.name}.$extension").writeText(hash(file, algorithm))
  }

  private fun hash(file: File, algorithm: String): String {
    val digest = MessageDigest.getInstance(algorithm)

    file.inputStream().buffered().use { input ->
      val buffer = ByteArray(DEFAULT_BUFFER_SIZE)

      while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        digest.update(buffer, 0, read)
      }
    }

    return digest.digest().joinToString("") { "%02x".format(it) }
  }
}

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidKotlinMultiplatformLibrary)
  alias(libs.plugins.composeMultiplatform)
  alias(libs.plugins.composeCompiler)
  id("maven-publish")
  id("spotless-conventions")
  signing
}

group = providers.gradleProperty("POM_GROUP_ID").getOrElse("io.github.Itskiprotich")

version =
  providers
    .gradleProperty("VERSION_NAME")
    .orElse(providers.environmentVariable("VERSION_NAME"))
    .orElse(providers.environmentVariable("GITHUB_REF_NAME").map { it.removePrefix("v") })
    .getOrElse("0.1.0-SNAPSHOT")

val mavenCentralStagingDirectory = layout.buildDirectory.dir("maven-central/staging-repository")
val mavenCentralBundleDirectory = layout.buildDirectory.dir("maven-central")

kotlin {
  // TODO(AGP-9.0): rename `androidLibrary { }` to `android { }` once AGP is upgraded.
  androidLibrary {
    namespace = "icl.ohs.libs.auth"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    minSdk = libs.versions.android.minSdk.get().toInt()

    compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }

    withHostTest {}
  }

  iosArm64()
  iosSimulatorArm64()

  jvm()

  js { browser() }

  @OptIn(ExperimentalWasmDsl::class) wasmJs { browser() }

  sourceSets {
    commonMain.dependencies {
      implementation(libs.compose.runtime)
      implementation(libs.compose.ui)
      implementation(libs.compose.foundation)
      implementation(libs.compose.material3)
      implementation(libs.kotlinx.serialization.json)
      implementation(libs.ktor.client.core)
      implementation(libs.ktor.client.cio)
    }

    commonTest.dependencies {
      implementation(libs.kotlin.test)
      implementation(libs.compose.uiTest)
      implementation(libs.kotlinx.coroutines.test)
      implementation(libs.ktor.client.mock)
    }

    jvmTest.dependencies { implementation(compose.desktop.currentOs) }
  }
}

val isCi = providers.environmentVariable("CI").map(String::toBoolean).getOrElse(false)

if (isCi) {
  tasks
    .matching {
      it.name in
        setOf(
          "compileTestDevelopmentExecutableKotlinJs",
          "compileTestProductionExecutableKotlinJs",
          "jsBrowserTest",
          "wasmJsBrowserTest",
          "testAndroidHostTest",
        )
    }
    .configureEach { enabled = false }
}

publishing {
  repositories {
    mavenLocal()

    maven {
      name = "MavenCentralStaging"
      url = uri(mavenCentralStagingDirectory)
    }
  }

  publications.withType<MavenPublication>().configureEach {
    // Maven Central requires a javadoc jar for every non-pom artifact.
    val placeholderJavadocJar =
      tasks.register<Jar>("${name}JavadocJar") {
        archiveBaseName.set(artifactId)
        archiveVersion.set(project.provider { project.version.toString() })
        archiveClassifier.set("javadoc")
        from(layout.projectDirectory.file("README.md"))
      }

    artifact(placeholderJavadocJar)

    pom {
      name.set(providers.gradleProperty("POM_NAME").getOrElse("ICL Auth"))
      description.set(
        providers
          .gradleProperty("POM_DESCRIPTION")
          .getOrElse("Kotlin Multiplatform auth UI library for Compose Multiplatform apps.")
      )
      url.set(
        providers
          .gradleProperty("POM_URL")
          .getOrElse("https://github.com/Itskiprotich/icl-auth-mobile")
      )

      licenses {
        license {
          name.set(
            providers.gradleProperty("POM_LICENSE_NAME").getOrElse("Apache License, Version 2.0")
          )
          url.set(
            providers
              .gradleProperty("POM_LICENSE_URL")
              .getOrElse("https://www.apache.org/licenses/LICENSE-2.0.txt")
          )
        }
      }

      developers {
        developer {
          id.set(providers.gradleProperty("POM_DEVELOPER_ID").getOrElse("Itskiprotich"))
          name.set(
            providers.gradleProperty("POM_DEVELOPER_NAME").getOrElse("IntelliSOFT Consulting")
          )
          email.set(
            providers
              .gradleProperty("POM_DEVELOPER_EMAIL")
              .getOrElse("keeprawteachjapheth@gmail.com")
          )
          organization.set(
            providers
              .gradleProperty("POM_DEVELOPER_ORGANIZATION")
              .getOrElse("IntelliSOFT Consulting")
          )
          organizationUrl.set(
            providers
              .gradleProperty("POM_DEVELOPER_ORGANIZATION_URL")
              .getOrElse("https://github.com/Itskiprotich")
          )
        }
      }

      scm {
        url.set(
          providers
            .gradleProperty("POM_SCM_URL")
            .getOrElse("https://github.com/Itskiprotich/icl-auth-mobile")
        )
        connection.set(
          providers
            .gradleProperty("POM_SCM_CONNECTION")
            .getOrElse("scm:git:git://github.com/Itskiprotich/icl-auth-mobile.git")
        )
        developerConnection.set(
          providers
            .gradleProperty("POM_SCM_DEVELOPER_CONNECTION")
            .getOrElse("scm:git:ssh://git@github.com/Itskiprotich/icl-auth-mobile.git")
        )
      }
    }
  }
}

val cleanMavenCentralStagingRepository =
  tasks.register<Delete>("cleanMavenCentralStagingRepository") {
    delete(mavenCentralStagingDirectory)
  }

tasks.matching { it.name == "publishAllPublicationsToMavenCentralStagingRepository" }.configureEach {
  dependsOn(cleanMavenCentralStagingRepository)
}

val generateMavenCentralChecksums =
  tasks.register<GenerateMavenCentralChecksumsTask>("generateMavenCentralChecksums") {
    dependsOn("publishAllPublicationsToMavenCentralStagingRepository")
    repositoryDirectory.set(mavenCentralStagingDirectory)
  }

tasks.register<Zip>("bundleMavenCentralRelease") {
  dependsOn(generateMavenCentralChecksums)
  archiveFileName.set("central-bundle.zip")
  destinationDirectory.set(mavenCentralBundleDirectory)
  from(mavenCentralStagingDirectory) {
    exclude("**/maven-metadata*.xml", "**/maven-metadata*.xml.*")
  }
}

signing {
  val signingKey =
    providers.gradleProperty("SIGNING_KEY").orElse(providers.environmentVariable("SIGNING_KEY"))
  val signingPassword =
    providers
      .gradleProperty("SIGNING_PASSWORD")
      .orElse(providers.environmentVariable("SIGNING_PASSWORD"))

  if (!signingKey.orNull.isNullOrBlank()) {
    useInMemoryPgpKeys(signingKey.get(), signingPassword.orNull)
    sign(publishing.publications)
  }
}
