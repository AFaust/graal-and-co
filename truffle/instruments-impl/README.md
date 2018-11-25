# About
This sub-module defines the implementations for the Truffle-based instrumentations provided in this repository.

# Dealing with Truffle classpath
As mentioned on the [graal-dev](http://mail.openjdk.java.net/pipermail/graal-dev) mailing list in [July 2017](http://mail.openjdk.java.net/pipermail/graal-dev/2017-July/005038.html), any custom languages or tools using the Truffle APIs need to be placed on the Truffle-specific classpath and cannot be loaded from the regular boot classpath, or any extended classpath defined by custom classloaders. Classes on the Truffle-specific classpath are not accessible to regular application classes, necessitating a split into API and implementation classes / libraries. API classes / libraries need to be placed on the boot classpath and thus allow for interaction between custom language / tooling classes and any actual application.

## Example: PolyglotCallstackService
The [PolyglotCallstackService](https://github.com/AFaust/graal-and-co/tree/master/truffle/instruments-api/src/main/java/de/axelfaust/graal/truffle/instruments/api/PolyglotCallstackService.java) is the defined API for an instrumentation service allowing application code to inspect the current callstack of polyglot applications without having to try, parse and evaluate the full JVM thread stack. The [actual instrument](https://github.com/AFaust/graal-and-co/tree/master/truffle/instruments-impl/src/main/java/de/axelfaust/graal/truffle/instruments/internal/PolyglotCallstackTrackerInstrument.java) creates an ExecutionEventNode for every function call site to record additions / removals to the current callstack as execution enters/leaves those sites.

The Docker-based unit test for this (and other) instrument(s) demonstrates the special handling of the classpath:
* the **de.axelfaust.graal.truffle.instruments.api-&lt;version&gt;.jar** is put on the GraalVM boot classpath as a preparation

```xml
<docker.test.prepareCmd>mv /maven/lib/de.axelfaust.graal.truffle.instruments.api-${project.version}.jar \
             /usr/lib/jvm/graalvm-ce-${dep.graalvm.version}/jre/lib/boot/</docker.test.prepareCmd>
```

* the classes of the implementation project (which are bundled in **de.axelfaust.graal.truffle.instruments.impl-&lt;version&gt;.jar** once the build is complete) are put on the Truffle classpath via the **-Dtruffle.class.path.append=** parameter
* the test classes and any supporting libraries are put on the application classpath via the **-cp** parameter

```xml
<docker.test.runCmd>java -cp /maven/test-classes/:/maven/lib/*:/maven/test-lib/* -Dtruffle.class.path.append=/maven/classes/ \
            org.junit.runner.JUnitCore de.axelfaust.graal.truffle.instruments.GraalVMTestSuite</docker.test.runCmd>
```

## Open issues with Truffle classpath

### Logging
Since custom language and instrumentation classes / libraries have to reside on the Truffle classpath, they cannot access any application specific logging APIs / frameworks. There also appears to be no clean / easy way to access any custom log handler that can be [registered via the Context builder](http://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Context.Builder.html#logHandler-java.util.logging.Handler-) as most (if not all) of the classes within Truffle are severely restricted with regards to lack of provided parameters or classes / accessors with public visibility. Without putting entire logging frameworks on either the boot or Truffle classpaths, the only options at this moment seem to be 

* use of Java utils logging
* use of System.out/err
* use of a custom log wrapper defined in an API JAR to be passed / configured on a per-thread basis from application code

### Wrapping / converting Truffle API objects to public polyglot objects
Classes providing custom languages or instrumentation to Truffle-based languages typically have to deal with Truffle API-specific objects. Due to the limitations in class accessibility, it is not possible to expose such objects in the public API of the language or instrumentation. Instead, polyglot wrappers should be used, which are provided as part of the Graal SDK JAR. Unfortunately there appears to be no clean way / accessible API for custom language / instrumentation code to perform wrapping / conversion of these objects.

Examples of this are:
* [Polyglot SourceSection](http://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/SourceSection.html) and [Truffle API SourceSection](https://www.graalvm.org/truffle/javadoc/com/oracle/truffle/api/source/SourceSection.html)
* [TruffleInstrument#Env class](https://www.graalvm.org/truffle/javadoc/com/oracle/truffle/api/instrumentation/TruffleInstrument.Env.html) which does not provide any way to access utilities to convert SourceSection instances (Truffle contains **com.oracle.truffle.api.impl.Accessor$SourceSupport** and **com.oracle.truffle.api.impl.Accessor$EngineSupport** implementation classes that can - among other things - wrap Source and SourceSection instances to their polyglot variant)