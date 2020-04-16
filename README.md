# DEPRECATED

This project receives minimal maintenance to keep software that relies on it working. There
is no active development or planned feature improvement. For any new projects it is recommended
to use the [Spectator] library instead.

For more details see the [Servo comparison] page in the Spectator docs.

[Spectator]: https://github.com/Netflix/spectator
[Servo comparison]: http://netflix.github.io/spectator/en/latest/intro/servo-comparison/

# No-Op Registry

As of version 0.13.0, the default monitor registry is a no-op implementation to minimize
the overhead for legacy apps that still happen to have some usage of Servo. If the previous
behavior is needed, then set the following system property:

```
com.netflix.servo.DefaultMonitorRegistry.registryClass=com.netflix.servo.jmx.JmxMonitorRegistry
```

# Servo: Application Metrics in Java

> servo v. : WATCH OVER, OBSERVE

>Latin.

Servo provides a simple interface for exposing and publishing application metrics in Java.  The primary goals are:

* **Leverage JMX**: JMX is the standard monitoring interface for Java and can be queried by many existing tools.
* **Keep It Simple**: It should be trivial to expose metrics and publish metrics without having to write lots of code such as [MBean interfaces](http://docs.oracle.com/javase/tutorial/jmx/mbeans/standard.html).
* **Flexible Publishing**: Once metrics are exposed, it should be easy to regularly poll the metrics and make them available for internal reporting systems, logs, and services like [Amazon CloudWatch](http://aws.amazon.com/cloudwatch/).

This has already been implemented inside of Netflix and most of our applications currently use it.

## Project Details

### Build Status

[![Build Status](https://travis-ci.org/Netflix/servo.svg)](https://travis-ci.org/Netflix/servo/builds)

### Versioning

Servo is released with a 0.X.Y version because it has not yet reached full API stability.

Given a version number MAJOR.MINOR.PATCH, increment the:

* MINOR version when there are binary incompatible changes, and
* PATCH version when new functionality or bug fixes are backwards compatible.

### Documentation

 * [GitHub Wiki](https://github.com/Netflix/servo/wiki)
 * [Javadoc](http://netflix.github.io/servo/current/servo-core/docs/javadoc/)

### Communication

* Google Group: [Netflix Atlas](https://groups.google.com/forum/#!forum/netflix-atlas)
* For bugs, feedback, questions and discussion please use [GitHub Issues](https://github.com/Netflix/servo/issues).
* If you want to help contribute to the project, see [CONTRIBUTING.md](https://github.com/Netflix/servo/blob/master/CONTRIBUTING.md) for details.


## Project Usage

### Build

To build the Servo project:

```
$ git clone https://github.com/Netflix/servo.git
$ cd servo
$ ./gradlew build
```

More details can be found on the [Getting Started](https://github.com/Netflix/servo/wiki/Getting-Started) page of the wiki.

### Binaries

Binaries and dependency information can be found at [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Ccom.netflix.servo).

Maven Example:

```
<dependency>
    <groupId>com.netflix.servo</groupId>
    <artifactId>servo-core</artifactId>
    <version>0.12.7</version>
</dependency>
```

Ivy Example:

```
<dependency org="com.netflix.servo" name="servo-core" rev="0.12.7" />
```

## License

Copyright 2012-2016 Netflix, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at:

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
