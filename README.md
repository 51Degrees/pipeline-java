# 51Degrees Pipeline

![51Degrees](https://51degrees.com/img/logo.png?utm_source=github&utm_medium=readme&utm_campaign=pipeline-java&utm_content=readme.md&utm_term=top "Data rewards the curious") **Java Pipeline**

[Developer Documentation](https://51degrees.com/pipeline-java/index.html?utm_source=github&utm_medium=readme&utm_campaign=pipeline-java&utm_content=readme.md&utm_term=top "developer documentation")

## Introduction
This repository contains all the projects required to build the Java implementation of the 51Degrees Pipeline API.

The [specification](https://github.com/51Degrees/specifications/blob/main/pipeline-specification/README.md)
is also available on GitHub and is recommended reading if you wish to understand
the concepts and design of this API.

Reference documentation for the Java implementation can be found on the
[Java API documentation](https://51degrees.com/pipeline-java/index.html?utm_source=github&utm_medium=readme&utm_campaign=pipeline-java&utm_content=readme.md&utm_term=introduction) page.

## Pre-requisites

The [tested versions](https://51degrees.com/documentation/_info__tested_versions.html?utm_source=github&utm_medium=readme&utm_campaign=pipeline-java&utm_content=readme.md&utm_term=pre-requisites) page shows 
the JDK versions that we currently test against. The software may run fine against other versions, 
but additional caution should be applied.

## Contents
 
- pipeline.common - Some shared library classes.
- pipeline.caching - 51Degrees' caching interfaces and high-performance LRU implementation.
- pipeline.core - The core software that comprises the Pipeline API. 
- pipeline.engines - Shared functionality that is available to all 'engines' (specialized 'flow elements')
- pipeline.engines.fiftyone - Functionality that is specific to 51Degrees engines.
- pipeline.cloudrequestengine - An engine that is used to retrieve data from 51Degrees' cloud API.

## Installation

Packages can be found on Maven under the group [com.51degrees](https://mvnrepository.com/artifact/com.51degrees).

Alternatively clone this git repository and in the root run `mvn install` to build and install the packages locally. 

## Java modules and native library access

`pipeline.engines.fiftyone` contains `LibLoader`, which calls `System.load` to load
the native libraries used by the on-premise engines. `System.load` is a *restricted
method*: recent JDKs warn when it is called without permission, and a future release
will block it outright (see [JEP 472](https://openjdk.org/jeps/472)).

```
WARNING: A restricted method in java.lang.System has been called
WARNING: java.lang.System::load has been called by fiftyone.pipeline.engines.fiftyone.flowelements.interop.LibLoader in an unnamed module
WARNING: Use --enable-native-access=ALL-UNNAMED to avoid a warning for callers in this module
WARNING: Restricted methods will be blocked in a future release unless native access is enabled
```

To let you grant that permission to 51Degrees code alone rather than to everything on
the classpath, the packages below declare module names:

| Package                   | Module name                          |
|---------------------------|--------------------------------------|
| pipeline.common           | `fiftyone.common`                    |
| pipeline.caching          | `fiftyone.caching`                   |
| pipeline.core             | `fiftyone.pipeline.core`             |
| pipeline.engines          | `fiftyone.pipeline.engines`          |
| pipeline.engines.fiftyone | `fiftyone.pipeline.engines.fiftyone` |

### Granting native access

Put the 51Degrees JARs on the **module path** and name the module that loads the
native libraries:

```bash
java --module-path libs --enable-native-access=fiftyone.pipeline.engines.fiftyone -m your.app/com.example.Main
```

With the Maven exec plugin:

```xml
<configuration>
    <arguments>
        <argument>--enable-native-access=fiftyone.pipeline.engines.fiftyone</argument>
        ...
    </arguments>
</configuration>
```

Module names only exist on the module path. If the 51Degrees JARs are on the
**classpath** - which is still the default and remains fully supported - they are part
of the unnamed module, and the only permission the JVM will accept is:

```bash
java -cp "libs/*" --enable-native-access=ALL-UNNAMED com.example.Main
```

Passing a module name while running from the classpath has no effect and reports
`WARNING: Unknown module: fiftyone.pipeline.engines.fiftyone specified to --enable-native-access`.

### Notes for module path users

- The JARs are multi-release: the module descriptors live in `META-INF/versions/9`,
  so the same artifacts still run on Java 8, where they are ordinary non-modular JARs.
- Only these five packages are named. Downstream packages such as `device-detection`
  and your own application do not need module declarations - unnamed modules can read
  named ones.
- `PipelineOptionsFactory` (XML/JSON pipeline configuration) uses JAXB, and the JAXB
  implementation is not discoverable through `ServiceLoader` on the module path. Add it
  to the module graph explicitly:

```bash
java --module-path libs --add-modules com.sun.xml.bind --enable-native-access=fiftyone.pipeline.engines.fiftyone -m your.app/com.example.Main
```

- `PipelineBuilder.buildFromConfiguration` finds element builders by scanning the
  classpath. Builders supplied from the module path are not visible to that scan, so
  configuration-driven pipelines should keep their element packages on the classpath.

## Tests

Most packages contain tests which use junit and mockito. These tests can be run using maven by calling: `mvn test`

## Examples

There are several examples available in the `pipeline.developer-examples` folder that demonstrate 
how to make use of the Pipeline API in isolation. These are described in the table below.
If you want examples that demonstrate how to use 51Degrees products such as device detection, 
then these are available in the corresponding [repository](https://github.com/51Degrees/device-detection-java) 
and on our [website](https://51degrees.com/documentation/_examples__device_detection__index.html?utm_source=github&utm_medium=readme&utm_campaign=pipeline-java&utm_content=readme.md&utm_term=examples).

| Example                                            | Description |
|----------------------------------------------------|-------------|
| pipeline.developer-examples.flowelement            | Shows how to create a custom flow element that returns star sign based on a supplied date of birth. |
| pipeline.developer-examples.onpremise-engine       | Shows how to modify SimpleFlowElement to make use of the 'engine' functionality and use a custom data file to map dates to star signs rather than relying on hard coded data. |
| pipeline.developer-examples.clientside-element     | Shows how to modify SimpleFlowElement to request the data of birth from the user using client-side JavaScript. |
| pipeline.developer-examples.clientside-element-mvc | An example project showing how to use the code from SimpleClientSideElement in a Java web application using the Model-View-Controller Pattern. |
| pipeline.developer-examples.cloud-engine           | Shows how to modify SimpleFlowElement to perform the star sign lookup via a cloud service rather than locally. |
| pipeline.developer-examples.usage-sharing          | Shows how to share usage with 51Degrees. This helps us to keep our products up to date and accurate. |





