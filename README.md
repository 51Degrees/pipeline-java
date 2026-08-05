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

## Native library access

`pipeline.engines.fiftyone` contains `LibLoader`, which calls `System.load` to load the
native libraries used by the on-premise engines. `System.load` is a *restricted method*
([JEP 472](https://openjdk.org/jeps/472)): Java 24 and 25 warn once per calling module,
and a later release will refuse the call and throw `IllegalCallerException`.

```
WARNING: A restricted method in java.lang.System has been called
WARNING: java.lang.System::load has been called by fiftyone.pipeline.engines.fiftyone.flowelements.interop.LibLoader in an unnamed module
WARNING: Use --enable-native-access=ALL-UNNAMED to avoid a warning for callers in this module
WARNING: Restricted methods will be blocked in a future release unless native access is enabled
```

There are two ways to grant the permission.

### On the classpath

This is the default layout and needs no changes to how you package or deploy:

```bash
java -cp "libs/*" --enable-native-access=ALL-UNNAMED com.example.Main
```

Everything on the classpath belongs to the unnamed module, so `ALL-UNNAMED` is the only
name the JVM will accept there. This is a rule of the module system, not something these
packages can improve on.

### Naming 51Degrees code alone

If granting native access to every jar on the classpath is too broad, move
**only** `pipeline.engines.fiftyone` onto the module path and leave your application and
all other dependencies - including the other four 51Degrees jars - on the classpath:

```bash
java -cp "libs/*" --module-path mods/pipeline.engines.fiftyone-4.5.7.jar --add-modules fiftyone.pipeline.engines.fiftyone --enable-native-access=fiftyone.pipeline.engines.fiftyone com.example.Main
```

The same jar must not appear on both paths, so keep it in a directory of its own rather
than in the one the classpath wildcard covers.

All five packages carry a module name, so any of them can be moved to the module path
the same way, but only `fiftyone.pipeline.engines.fiftyone` needs to be:

| Package                   | Module name                          |
|---------------------------|--------------------------------------|
| pipeline.common           | `fiftyone.common`                    |
| pipeline.caching          | `fiftyone.caching`                   |
| pipeline.core             | `fiftyone.pipeline.core`             |
| pipeline.engines          | `fiftyone.pipeline.engines`          |
| pipeline.engines.fiftyone | `fiftyone.pipeline.engines.fiftyone` |

The names come from an `Automatic-Module-Name` manifest entry, so the jars are ordinary
non-modular jars, build identically on every JDK from 8 upwards, and impose nothing on
consumers who stay on the classpath.

Device detection users need to grant access to a second module as well. The JNI `native`
declarations live in `device-detection.hash.engine.on-premise`, and JEP 472 checks
binding them against the module that declares them - see the
[device-detection-java README](https://github.com/51Degrees/device-detection-java#native-library-access).

### Configuring builders from the module path

`PipelineBuilder.buildFromConfiguration` discovers element builders by scanning the
class path, which does not include the module path. When a builder ships in a jar you
have moved to the module path, name it by its fully qualified class name instead of its
short name:

```xml
<Element>
    <BuilderName>fiftyone.pipeline.engines.fiftyone.flowelements.ShareUsageBuilder</BuilderName>
</Element>
```

This affects `SequenceElementBuilder`, `SetHeadersElementBuilder` and `ShareUsageBuilder`,
which ship inside `pipeline.engines.fiftyone`. Short names continue to work for every
builder on the class path.

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





