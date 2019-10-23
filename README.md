sarlacc-pit
===========

Originally intended to allow multiple services to share a large updateable deny list, now a more general 
purpose library for dynamically updating a collection by polling a canonical source.

`sarlacc-pit` essentially caches the full contents of some backing store, updating it in the background 
on a cadence. Applications such as accept and deny lists where traditional caching would require 
storing negative results as well as positive are ideal. Other common applications are small datasets 
such as per-customer overrides which might otherwise be in config files.

Unlike a cache, reads from `sarlacc-pit` are always in memory, and the backing store going unavailable 
will simply cause the data to be stale, rather than lookups failing if the required value is not in the
cache.


Configuration Sources
---------------------

Sarlacc Pit allows polling of a variety of config sources. Included in the base client are three,
FileConfigSource, HttpConfigSource, and MultipleHttpConfigSource. In addition the sarlacc-gcloud 
module includes a GcsConfigSource backed by Google Cloud Storage. Further sources may be added by 
implementing the ConfigSource interface. Ideally, config sources provide some get-if-newer functionality,
but if not both `fetch()` operations can be made unqualified.


Data Structures
---------------

Currently three data structures are supported: maps, lists, and sets. 


General use
-----------


```java
// Create a ConfigSource as above
ConfigSource<InputStream> confSource = new HttpSource("http://my.website.com/service_config.properties");

// Build the service. All parameters here are required, see below for further additional parameters.
UpdateService<Set<String>> updateService = UpdateService.<String>setServiceBuilder()
                .setServiceName("my_config")
                .setLineProcessor(ImmutableArrayStringSetLineProcessor.SUPPLIER)
                .setConfigSource(confSource)
                .setFetchInterval(10, TimeUnit.SECONDS)
                .build();

// Start the service. By default this will throw if the service can't complete an initial fetch.
// See setFallbackValue() below.
updateService.startAsync().awaitRunning();

// Get the updating data structure. It is immutable from a client perspective,
// threadsafe, and safe to keep references to. New versions of the data will be atomically
// swapped in. While this is safe to call before service startup, any attempt to read from the
// returned structure will throw IllegalStateException if the UpdateService is not running.
Set<String> myUpdatingSet = updateService.getUpdatingCollection();
```


Optional Service Parameters
---------------------------

#### Metrics

        setMetricRegistry(MetricRegistry metricRegistry)
        setMetricNamer(MetricNamer metricNamer);

If a Dropwizard MetricsRegistry is provided, the service will expose a number of metrics, see the 
UpdateService.Metrics class for a full list. By default, metrics will be named in a JMX compatible
manner, but a MetricNamer may be provided to customize naming.

#### Update Callback

        setUpdateCallback(UpdateCallback<D> updateCallback)

Provide a callback to be invoked every time a new value is fetched from the config source. 
On service startup any provided UpdateCallback will be invoked with an absent previous 
collection and the initial value fetched and its version. Any exceptions thrown by the callback will
be logged but otherwise ignored. 

It is guaranteed that invocations of this callback will never overlap, and will never overlap with
an invocation of the failure callback if defined. Execution of this callback is performed on the thread
responsible for polling and fetching new values, so performing significant work in it is not advised.

#### Failure Callback

        setFailureCallback(FailureCallback failureCallback)
        
Provide a callback to be invoked any time a fetch or process operation throws. Depending on the return value of the 
callback either no action will be taken, the exposed collection will be put in an error state, or the whole service
will be shut down. Note that if the initial fetch fails and the failure callback returns SHUT_DOWN, an exception will
be thrown instead.        

It is guaranteed that invocations of this callback will never overlap, and will never overlap with
an invocation of the update callback if defined. Execution of this callback is performed on the thread
responsible for polling and fetching new values, so performing significant work in it is not advised.


#### Fallback Value

        setFallbackValue(D fallbackValue, long fallbackVersion)

By default, failure to perform the initial `fetch()` and `process()` operations will cause service startup to fail. This
means that in the event of a config source outage new instances will not be able to start, though existing 
instances will continue using the last known good value. If a fallback value is provided, it will be used
until a fetch is successful. The `Fallbacks` class provides static methods for building common fallback structures.


#### Max Failures

        setMaxFailures(long maxFailures) {

By default, the mirrored collection remains usable no matter how stale its become. If an upper bound is needed,
the service can be configured to start rejecting reads with an `IllegalStateException` if `maxFailures` fetch attempts
in a row fail.


Monitoring
----------

UpdateService instances are designed to survive arbitrary unavailability of the config source that backs 
them. This means that the collection provided by the service can go arbitrarily out of sync with that backing
source. Services should monitor the age of the last successful fetch, either using Dropwizard metrics or 
custom handling using the `UpdateService.getLastSuccessfulCheck()` method, and take action if the difference
is greater than is tolerable.
