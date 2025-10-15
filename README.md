# Fluently

[![](https://git.karmakrafts.dev/kk/fluently/badges/master/pipeline.svg)](https://git.karmakrafts.dev/kk/fluently/-/pipelines)
[![](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo.maven.apache.org%2Fmaven2%2Fdev%2Fkarmakrafts%2Ffluently%2Ffluently-core%2Fmaven-metadata.xml
)](https://git.karmakrafts.dev/kk/fluently/-/packages)
[![](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fcentral.sonatype.com%2Frepository%2Fmaven-snapshots%2Fdev%2Fkarmakrafts%2Ffluently%2Ffluently-core%2Fmaven-metadata.xml
)](https://git.karmakrafts.dev/kk/fluently/-/packages)

An implementation of [Project Fluent](https://projectfluent.org/) in pure Kotlin for Kotlin Multiplatform.

Fluently is a highly versatile and dynamic localization system which utilizes the Fluent language.
It allows for asymmetric localizations and embedding localization logic directly into the resource.

### Exclusive features & fixes

Fluently offers some exclusive fixes and additional features compared to the official Fluent implementation,  
including but not limited to:

- Named arguments may accept any type of inline expression.
  * Fixes https://github.com/projectfluent/fluent/issues/230
- Remove the requirement for superfluous equal sign after an identifier
  * Fixes https://github.com/projectfluent/fluent/issues/190

### How to use it

First, add the official Maven Central repository to your `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        maven("https://central.sonatype.com/repository/maven-snapshots")
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        maven("https://central.sonatype.com/repository/maven-snapshots")
        mavenCentral()
    }
}
```

Then add a dependency on the library in your root buildscript:

```kotlin
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("dev.karmakrafts.fluently:fluently-core:<version>")
                // Optional support for coroutines based reactivity
                implementation("dev.karmakrafts.fluently:fluently-reactive:<version>")
            }
        }
    }
}
```

Or, if you are only using Kotlin/JVM, add it to your top-level dependencies block instead.

### Using the Gradle plugin

Fluently optionally offers a Gradle plugin which generates type safe localization bindings
from your Fluent files. This works for both Kotlin Multiplatform and Kotlin JVM.  
Simple apply the plugin as follows:

```kotlin
plugins {
    id("dev.karmakrafts.fluently.fluently-gradle-plugin") version "<version>"
}
```

The plugin can be configured using the `fluently` project extension.

**Example TBA**

### Loading Fluent files

In order to load a localization file, the `LocalizationFile` class can be used as follows:

```kotlin
val fileContent: String = loadMyFile()
val file = LocalizationFile.parse(fileContent)
```

The `parse` function also allows for an optional trailing closure which lets you define
builtin functions and variables which are exported into the Fluent file using a `EvaluationContextBuilder`:

```kotlin
val file = LocalizationFile.parse(fileContent) {
    // Define custom variables
    variable("myVar", string("Hello, World!"))

    // Define custom functions
    function("MYFUNC") {
        returnType = ExprType.STRING
        parameter("myParam", ExprType.STRING)
        action { ctx, args ->
            val myParam = args.first().evaluate(ctx)
            string("My result value")
        }
    }
}
```

> **Tip:**
> On the JVM and Android, there's also a `parseResource` extension, which allows
> loading localization files directly as a JAR resource.

### Loading localization bundles

Localization bundles are a concept introduced by Fluently, not Fluent itself.  
They allow specifying all localization files and defaults for an entire application
in a convenient JSON5 based format:

```kotlin
// Get the content of your JSON file as a String
val bundleJson = loadMyBundle()

// Parse the bundle JSON
val bundle = LocalizationBundle.fromJsonString(bundleJson)

// Load Fluent files directly from the bundle
val file = manager.loadLocale("en-US", { path ->
    // The resource provider lets you specify custom loading behaviour
    loadMyFluentFile(path)
}) { /* context init */ }
```

> **Tip:**
> The repository provides a JSON schema for working with Fluently bundles under
> the `schema` directory. You can use it by adding a `$schema` variable to your
> top-level JSON object and providing a local path or URL to the schema file.

> **Tip:**
> On the JVM and Android, there's also a `fromResource` extension, which allows
> loading bundles directly as a JAR resource. In order to load the actual fluent
> files from the bundle, a supplementary `loadLocaleFromResource` extensions for
> the bundle instance is also provided.

### Using reactivity

Fluently also offers an optional `fluently-reactive` module, which integrates the
localization system with a kotlinx.coroutines based reactivity layer.  
This can be especially useful for UI applications which utilize Compose or Swing:

```kotlin
// The manager needs a CoroutineContext for coroutine lifecycle management;
// This scope/context should stick around the application's entire lifetime
val supervisor = SupervisorJob()
val coroutineScope = CoroutineScope(Dispatchers.Default + supervisor)

// Parse the bundle JSON
val bundle = LocalizationBundle.fromJsonString(bundleJson)

// Create a reactive localization manager with a resource provider
val manager = LocalizationManager(bundle, { path ->
    // The resource provider lets you specify custom loading behaviour
    loadMyFluentFile(path)
}, coroutineScope.coroutineContext)

// Create a cold flow for the given localization entry
val someValue = MutableStateFlow("Some text")
val entry = manager.format("entryName", "attribName") { 
    // We can define variables based on other flows, so the localization is re-emitted when the input variable changes
    variable("someValue", string(someValue))
}
```

In order to properly memoize formatted strings and to handle hot flow lifecycles,  
a class `LocalizationScope` may be used in conjunction with the manager:

```kotlin
// The scope requires its own coroutine scope so memoized hot flows obtained
// from the scope share their lifetime with the relevant Window/VM
val supervisor = SupervisorJob()
val coroutineScope = CoroutineScope(Dispatchers.Swing + supervisor)

// Create a new scope from the manager and the current coroutine scope
val scope = LocalizationScope(manager, coroutineScope)

// Obtain memoized hot flows for a given localization entry:
val entry = scope.format("entryName", "attribName") { /* context init */ }
```