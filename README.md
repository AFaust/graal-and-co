# About
This project and its sub-module(s) currently act as a playground for me to familiarize myself with Graal, GraalVM, Truffle, and anything else in that sphere. I intend to eventually add polyglot integrations into [Alfresco products](https://www.alfresco.com/) (Alfresco Content Services / Alfresco Process Services). For that reason the structure of this repository will already be a bit more complex than warranted for a pure playground project, as I plan to transition to a regular project as my understanding of all things GraalVM evolves...

# Build

## Requirements
This repository uses a Maven build that includes a Docker-based runtime environment for JUnit tests. This is to simplify upgrading and testing with different GraalVM release candidates / releases as soon as they are available, and eliminate any platform dependence, e.g. allow interested users to build this project even on platforms without a GraalVM release yet, such as Windows.

The build requires that an installation of Apache Maven and Docker is available on the local machine. The Docker runtime currently uses a SNAPSHOT-image built from [my company image collection](https://github.com/Acosix/acosix-docker-generic/tree/master/baseimage-graalvm) using [Phusion baseimage](https://github.com/phusion/baseimage-docker) and a simple unpack-install of the GraalVM distribution.

## Running the build

The easiest way to run the build is by using the Maven standard "install" goal on the root level.

```
mvn install
```