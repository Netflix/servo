The following Tomcat metrics are collected by Servo from JMX. The Tomcat
[service name](https://tomcat.apache.org/tomcat-8.0-doc/config/service.html) must be set
to Catalina in server.xml in order for the metrics to be located in JMX.

```xml
<Server port="8005" shutdown="SHUTDOWN">
  ...
  <Service name="Catalina">
  ...
  </Service>
  ...
</Server>
```

## Metrics

### Executor

These metrics will be reoprted if an
[executor](https://tomcat.apache.org/tomcat-8.0-doc/config/executor.html) is configured.

#### tomcat.currentThreadsBusy

Number of threads that are in use. This value comes from the
[active count](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ThreadPoolExecutor.html#getActiveCount--)
of the executor.

**Unit:** count

**Dimensions:**
* `class`: will have a value of `Executor`.
* `id`: name specified on the executor element.

#### tomcat.completedTaskCount

Rate of completed tasks on the executor. This value comes from the
[completed task count](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ThreadPoolExecutor.html#getCompletedTaskCount--)
of the executor.

**Unit:** tasks per second

**Dimensions:**
* `class`: will have a value of `Executor`.
* `id`: name specified on the executor element.

#### tomcat.maxThreads

Maximum number of threads allowed in the pool. This value comes from the
[maximum pool size](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ThreadPoolExecutor.html#getMaximumPoolSize--)
of the executor.

**Unit:** count

**Dimensions:**
* `class`: will have a value of `Executor`.
* `id`: name specified on the executor element.

#### tomcat.poolSize

Number of threads in the pool. This value comes from the
[pool size](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ThreadPoolExecutor.html#getPoolSize--)
of the executor.

**Unit:** count

**Dimensions:**
* `class`: will have a value of `Executor`.
* `id`: name specified on the executor element.

#### tomcat.queueSize

Number of tasks in the queue waiting to be executed. This value comes from calling size on the
[queue](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ThreadPoolExecutor.html#getQueue--).

**Unit:** count

**Dimensions:**
* `class`: will have a value of `Executor`.
* `id`: name specified on the executor element.


### Global Request Processor

#### tomcat.bytesSent

Amount of data that has been written out to clients.

**Unit:** bytes/second

**Dimensions:**
* `class`: will have a value of `GlobalRequestProcessor`.
* `id`: name of the request processor. This usually indicates the protocol, ip, and port that
  the processor is listening on, e.g. `_http-bio-0.0.0.0-7001_`.

#### tomcat.errorCount

Rate of request errors.

**Unit:** errors/second

**Dimensions:**
* `class`: will have a value of `GlobalRequestProcessor`.
* `id`: name of the request processor. This usually indicates the protocol, ip, and port that
  the processor is listening on, e.g. `_http-bio-0.0.0.0-7001_`.

#### tomcat.maxTime

Maximum time it took to process a request.

**Unit:** milliseconds

**Dimensions:**
* `class`: will have a value of `GlobalRequestProcessor`.
* `id`: name of the request processor. This usually indicates the protocol, ip, and port that
  the processor is listening on, e.g. `_http-bio-0.0.0.0-7001_`.

#### tomcat.processingTime

Amount of time spent processing requests.

**Unit:** milliseconds/second

**Dimensions:**
* `class`: will have a value of `GlobalRequestProcessor`.
* `id`: name of the request processor. This usually indicates the protocol, ip, and port that
  the processor is listening on, e.g. `_http-bio-0.0.0.0-7001_`.

#### tomcat.requestCount

Overall number of requests processed.

**Unit:** requests/second

**Dimensions:**
* `class`: will have a value of `GlobalRequestProcessor`.
* `id`: name of the request processor. This usually indicates the protocol, ip, and port that
  the processor is listening on, e.g. `_http-bio-0.0.0.0-7001_`.

### Thread Pool

Metrics for the default thread pool created for each connector if an executor is not
being used.

#### tomcat.backlog

Number of tasks in the queue waiting to be executed.

**Unit:** count

**Dimensions:**
* `class`: will have a value of `ThreadPool`.
* `id`: name of the request processor. This usually indicates the protocol, ip, and port that
  the processor is listening on, e.g. `_http-nio-7001_`.

#### tomcat.currentThreadCount

Number of threads in the pool.

**Unit:** count

**Dimensions:**
* `class`: will have a value of `ThreadPool`.
* `id`: name of the request processor. This usually indicates the protocol, ip, and port that
  the processor is listening on, e.g. `_http-nio-7001_`.

#### tomcat.currentThreadsBusy

Number of threads that are in use.

**Unit:** count

**Dimensions:**
* `class`: will have a value of `ThreadPool`.
* `id`: name of the request processor. This usually indicates the protocol, ip, and port that
  the processor is listening on, e.g. `_http-nio-7001_`.

#### tomcat.maxThreads

Maximum number of threads allowed in the pool.

**Unit:** count

**Dimensions:**
* `class`: will have a value of `ThreadPool`.
* `id`: name of the request processor. This usually indicates the protocol, ip, and port that
  the processor is listening on, e.g. `_http-nio-7001_`.

