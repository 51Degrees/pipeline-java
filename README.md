# 51Degrees Pipeline

![51Degrees](https://51degrees.com/DesktopModules/FiftyOne/Distributor/Logo.ashx?utm_source=github&utm_medium=repository&utm_content=readme_main&utm_campaign=node-open-source "Data rewards the curious") **Java Pipeline**

[Developer Documentation](https://51degrees.com/pipeline-java/index.html?utm_source=github&utm_medium=repository&utm_content=documentation&utm_campaign=java-open-source "developer documentation")

## Introduction
This repository contains all the projects required to build the Java implementation of the 51Degrees Pipeline API.
Individual engines (For example, device detection) may be in separate repositories.

Reference documentation for the Java implementation can be found on the
[Java API documentation](https://51degrees.com/pipeline-java/index.html) page.

## Pre-requisites

The [tested versions](https://51degrees.com/documentation/_info__tested_versions.html) page shows 
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

## Tests

Most packages contain tests which use junit and mockito. These tests can be run using maven by calling: `mvn test`

## Examples

There are several examples available in the `pipeline.developer-examples` folder that demonstrate 
how to make use of the Pipeline API in isolation. These are described in the table below.
If you want examples that demonstrate how to use 51Degrees products such as device detection, 
then these are available in the corresponding [repository](https://github.com/51Degrees/device-detection-java) 
and on our [website](http://51degrees.com/documentation/_examples__device_detection__index.html).

| Example                                            | Description |
|----------------------------------------------------|-------------|
| pipeline.developer-examples.flowelement            | Shows how to create a custom flow element that returns star sign based on a supplied date of birth. |
| pipeline.developer-examples.onpremise-engine       | Shows how to modify SimpleFlowElement to make use of the 'engine' functionality and use a custom data file to map dates to star signs rather than relying on hard coded data. |
| pipeline.developer-examples.clientside-element     | Shows how to modify SimpleFlowElement to request the data of birth from the user using client-side JavaScript. |
| pipeline.developer-examples.clientside-element-mvc | An example project showing how to use the code from SimpleClientSideElement in a Java web application using the Model-View-Controller Pattern. |
| pipeline.developer-examples.cloud-engine           | Shows how to modify SimpleFlowElement to perform the star sign lookup via a cloud service rather than locally. |
| pipeline.developer-examples.usage-sharing          | Shows how to share usage with 51Degrees. This helps us to keep our products up to date and accurate. |





