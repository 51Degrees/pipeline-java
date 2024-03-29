- Flow data changes
  - Cancellation token - This was added to the spec as a 'future feature' in the spring 2023 re-write.
  - Stop mechanism - This is marked as obsolete and intended to be replaced by the cancellation token.
    It is NOT included as a requirement in the sprint 2023 spec re-write. However, for the existing implementations, 
    it is the only way to allow customers to prevent default elements from running, and the cancellation token 
    doesn't help with that. 
    For example, a customer was using the server side Apple component and found the overhead from the json + javascript
    elements was significant. These elements were automatically added by the web integration.
    Since they didn't need that functionality, the workaround was to create a custom element that set the 'Stop' flag 
    and configure it to be added at the end but before the json+javascript engines.
    - The spec [requires](https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/web-integration.md#pipeline-configuration)
      that the elements added by the web integration are optional, which would solve this in a different way and allow the
      removal of the stop mechanism.
  - Get property value directly from flow data by string name - This was removed from spec during the spring 2023 re-write.
  - Java does not have 'GetDataTypeFromElement' - see pipeline-specification/features/access-to-results.md
  - Evidence is not immutable. (This was added in the spring 2023 re-write to prevent additional complexity that would be 
    required to handle mutable evidence in some scenarios)
    - SequenceElement writes to evidence so this would need to change if immutable evidence was implemented.
    - The suggestion [in the spec](https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/evidence.md#adding-evidence-values) 
      is to create some function that can return requested values from either element data or evidence. This would allow
      elements such as GetHighEntropyElement to instead output to element data just like everything else and let the 
      consuming element access that data without significant changes.
- Builders 
  - Side-by-side generic class hierarchies of elements and builders creates a very confusing picture. Fixing this would 
    require a redesign to use a different mechanism to share logic. Design patterns such as Strategy or Decorator might
    offer a route forward.    
  - Default values are not defined in a consistent location. Mostly, this is done in builders. In some cases, doing this 
    would not allow existing logic to function in exactly the same way. Suggest redesign to make this entirely consistent 
    and the make values easier to find. (The device detection defaults will always be in the native C code, but all other
    defaults could be addressed.)
  - Device detection pipeline builder - Removed from spec. There is discussion of this in the 'pre packaged builder' paragraph in 
    [reference implementation notes](https://github.com/51Degrees/specifications/blob/main/pipeline-specification/reference-implementation-notes.md#the-pre-packaged-pipeline-builder). 
    Suggest either marking it and related base classes obsolete or spending some effort to investigate how the downsides 
    could be mitigated. 
  - There is no support in the pipeline or builders for 
    [relative paths](https://github.com/51Degrees/specifications/blob/main/pipeline-specification/features/file-system-paths.md). 
    Device detection engine supports relative paths to a limited extent using shared logic in the native code. 
- Usage sharing
  - Java usage sharing code does not generate a snippet on startup containing the static parts of each usage message. This 
    has already been implemented for C# and Node.
- Data update
  - Check uses last modified date from the file system. Should use 'published date' from the data file if available. last 
    modified date should be the fall back.
  - Does not deffer polling for new data file until after the *data update expected* time.
  - Does not allow for update from startup without a data file present.
  - Does not support manual/programmatic update when using in-memory data source.
  - Does not have the 'Data update use formatter' option. This allows user to disable url formatter when using a config file. 
    (E.g. want to use a static URL, rather than distributor to download new data files)
  - No logging for background events (checking for update, new data found ,etc)
  - A general re-design of the data update service is needed to remove complexity and align with the spec following
    changes made in the Spring 2023 spec re-write. 
    - Currently, when timer expires, the code checks local file, then checks URL. In the new spec, this logic would flow 
      differently.
    - The spec suggests preventing the user from creating an engine with an invalid update configuration. (E.g. by using a 
      more restrictive set of builder classes that constrain the options available to those that are valid)
    - Some terminology changes:
      - 'data file' becomes 'data source' - This is to prevent confusion when talking about a data 'file' that is not a file, 
        but a byte array in memory.
      - 'temp data file' becomes 'operational data file' - This is to better reflect the purpose and usage of this copy of the 
        data file.
- Web integration
  - The spec is intentionally vague on exactly how this is implemented. However, it may be that the current implementation is
    focusing too much on reproducing how the C# web integration works, rather than working in a way that is in line with how 
    Java web filters are intended to be used. 
  - Java does not allow configuration of JSON or JS endpoints   
- Device detection
  - [Device detection on premise](https://github.com/51Degrees/specifications/blob/main/device-detection-specification/pipeline-elements/device-detection-on-premise.md#element-data) - 
    mentions additional complexity in the match metric accessors in Java and .NET intended to cope with having separate 
    engines for each component. This logic is no longer needed and could be removed.
  - DD on premise page [mentions](https://github.com/51Degrees/specifications/blob/main/device-detection-specification/pipeline-elements/device-detection-on-premise.md#reference-implementation-notes)
    looking into the possibility of producing separate native language wrapper packages. (As has been discussed internally in the past)
  - Comments in examples talk about the lite file having 'reduced accuracy' and/or 'smaller training data set' and the like. This isn't true, all v4 files use the same training data and detection graph.
- Other
  - JsonBuilderElement does not implement the 'SetProperties' option to allow the user to configure which properties are included in the JSON.

