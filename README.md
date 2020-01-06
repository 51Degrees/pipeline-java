# Pipeline API
This repository contains all the projects required to build the Java implementation of the 51Degrees Pipeline API.
Individual engines (For example, device detection) may be in separate repositories.

## Modules

- pipeline.common - Some shared library classes.
- pipeline.caching - 51Degrees' caching interfaces and high-performance LRU implementation.
- pipeline.core - The core software that comprises the Pipeline API. 
- pipeline.engines - Shared functionality that is available to all 'engines' (specialized 'flow elements')
- pipeline.engines.fiftyone - Functionality that is specific to 51Degrees engines.
- pipeline.cloudrequestengine - An engine that is used to retrieve data from 51Degrees' cloud API.

## Documentation

Conceptual documentation for the Pipeline API can be found at the [51Degrees documentation site][Documenation].
Reference documentation for the Java implementation can be found in the GitHug pages site associated with this repository.


[Documentation]: https://docs.51degrees.com