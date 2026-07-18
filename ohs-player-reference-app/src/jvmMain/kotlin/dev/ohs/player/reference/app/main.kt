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
package dev.ohs.player.reference.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.ohs.fhir.FhirEngine
import dev.ohs.fhir.FhirEngineConfiguration
import dev.ohs.fhir.FhirEngineProvider
import dev.ohs.player.reference.app.auth.FileAuthSessionStore
import dev.ohs.player.reference.app.auth.defaultReferenceStorageDirectory
import dev.ohs.player.reference.app.auth.initializeReferenceAuth
import dev.ohs.player.reference.app.data.di.initKoin
import dev.ohs.player.reference.app.data.repository.FhirEngineRepository
import dev.ohs.player.reference.app.data.repository.FhirRepository
import dev.ohs.player.reference.app.data.repository.SeededFhirRepository
import org.koin.dsl.module

fun main() = application {
  val storageDirectory = defaultReferenceStorageDirectory()
  initializeReferenceAuth(FileAuthSessionStore(storageDirectory))
  if (FhirEngineProvider.isNotInitialized()) {
    FhirEngineProvider.init(
      FhirEngineConfiguration(storageDirectory = storageDirectory.absolutePath)
    )
  }
  initKoin(
    module {
      single<FhirEngine> { FhirEngineProvider.getInstance() }
      single<FhirRepository> { SeededFhirRepository(FhirEngineRepository(get())) }
    }
  )
  Window(onCloseRequest = ::exitApplication, title = "OHS Player Reference App") { App() }
}
