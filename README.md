sarlacc-pit
===========

Originally intended to allow multiple services to share a large and dynamically
updateable blacklist. Now a more general purpose library 
for dynamically updating an object by polling a flat file.


Configuration Sources
---------------------

Sarlacc Pit currently allows polling of configs either on the local file system or
on a remote system via HTTP. Setup is similar in either case.

```
new FileSource("etc/my_config.txt", false);
new FileSource("etc/my_config.gz", true);
new HttpSource("http://static.prod.urbanairship.com/configs/my_config");
```

FileSource uses the mtime of the file, HttpSource uses an HTTP conditional Get-If-Newer.

Further sources may be used by implementing the ConfigSource interface.


Data Structures
---------------

Currently four data structures are provided: A Map, List, Set, and a specialized
String set, ImmutableArrayStringSet, optimized for memory footprint for large 
sets such as blacklists.

To use one of the generic data structures you'll need to provide a parse function 
for transforming a line of input into an entry in the resulting dataset. 
See below for examples.


Health Checks
-------------

Instances of UpdateService expose a healthcheck 
(See https://atlassian.prod.urbanairship.com/stash/projects/JLIBS/repos/healthcheck/browse)
via the getHealthcheck() method. It's intended to be included as part of a larger suite of
healthchecks, and will not trigger alerts unless configured to. This healthcheck will report
unhealthy if any of the following is true:

* The last attempt to fetch and process an update failed.
* The service is in any state other that RUNNING.
* The last check was more than a configurable time ago. By default this is 
  4 * the fetch interval, but can be overridden with the setUnhealthyCheckAge()
  method when constructing a new service instance. This can happen if the fetch,
  processing, or callback invocation take more time than expected.


Callbacks
---------

Optionally, you may specify callbacks to be run any time a new value is fetched or
any time a check fails. See the UpdateCallback and FetchFailureCallback interfaces
for details. It is guaranteed that invocations of these callbacks will never overlap 
with each other or other invocations of themselves for a given updating collection. 
Because the callbacks are invoked on the thread responsible for performing fetches, 
it is imperative that they not block or perform significant amounts of work. If 
significant work is needed it should be offloaded to another thread.

On service startup any provided UpdateCallback will be invoked with an empty previous 
collection with an mtime of 0, and the initial value fetched and its mtime. The service
will not be considered started until the initial update callback has returned, and 
any error thrown by it will cause service startup to fail. After the callback returns
scheduled fetch attempts will begin on the specified interval.


Example
-------

General use:

```java
// Create a ConfigSource as above
ConfigSource confSource = new HttpSource("http://static.prod.urbanairship.com/config");

// Build the service
UpdateService<Set<String>> updateService = UpdateService.<String>setServiceBuilder()
                .setServiceName("my_config")
                .setLineProcessor(ImmutableArrayStringSetLineProcessor.SUPPLIER)
                .setConfigSource(confSource)
                .setFetchFailureCallback(failureCallback)
                .setUpdateCallback(updateCallback)
                .setFetchInterval(10, TimeUnit.SECONDS)
                .build();

// Start the service. This will fail if the service can't complete an initial fetch.
ServiceUtils.runService(updateService);

// Get the updating data structure. This set is immutable from a client perspective,
// threadsafe, and safe to keep references to. It will be updated behind the scenes.
// Doing this before starting the service will throw.
Set<String> myUpdatingSet = updateService.getUpdatingCollection();
```

Using a custom parse function:

```java
// Production code should probably do some error handling
Function<String, Map.Entry<String, Integer>> parseFunc = 
    new Function<String, Map.Entry<String, Integer>>() {
                @Override
                public Map.Entry<String, Integer> apply(String input) {
                    String[] chunks = input.split(",");
                    Integer val = Integer.parseInt(chunks[1]);
                    return new AbstractMap.SimpleEntry<String, Integer>(chunks[0], val);
                    }
                };
                
UpdateService<Map<String,Integer>> updateService = 
    UpdateService.<String,Integer>mapServiceBuilder()
                .setServiceName("my_map_config")
                .setLineProcessor(MapLineProcessor.makeSupplier(parseFunc))
                .setConfigSource(configSource)
                .setFetchInterval(10, TimeUnit.SECONDS)
                .build();
```
