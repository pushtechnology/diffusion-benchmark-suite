Benchmark Suite
===============

This project is a collection of synthetic benchmarks used to drive performance of
the diffusion product. Benchmarks of primary interest are:

Reasonable throughput test - Increments load by adding clients until a decline in yield is perceived.

Unreasonable throughput test - Increments the load thorughput test duration.

Minimum (best case) Latency test - One message at a time RTT ping.

Client churn test

Build
=====

In order to create a redistributable invoke ant as follows with DIFFUSION_HOME set appropriately
to the root of a Diffusion installation. When testing on multiple machines it is expected that
environment directory structures are equivalent w.r.t. runtime dependencies.

# ant
Buildfile: /Users/dennis/Push/p4/dennis_mac/Build/BenchmarkSuite/load-runner/build.xml

clean:
   [delete] Deleting directory /Users/dennis/Push/p4/dennis_mac/Build/BenchmarkSuite/load-runner/target

prepare:
    [mkdir] Created dir: /Users/dennis/Push/p4/dennis_mac/Build/BenchmarkSuite/load-runner/target/java/classes

build:
     [echo] /opt/Diffusion4.5.0_01/
    [javac] Compiling 41 source files to /Users/dennis/Push/p4/dennis_mac/Build/BenchmarkSuite/load-runner/target/java/classes
    [javac] Note: /Users/dennis/Push/p4/dennis_mac/Build/BenchmarkSuite/load-runner/src/publishers/InjectionPublisher.java uses or overrides a deprecated API.
    [javac] Note: Recompile with -Xlint:deprecation for details.

dist:
      [jar] Building jar: /Users/dennis/Push/p4/dennis_mac/Build/BenchmarkSuite/load-runner/target/dist/lib/diffusionbenchmark.jar
     [copy] Copying 1 file to /Users/dennis/Push/p4/dennis_mac/Build/BenchmarkSuite/load-runner/target/dist/lib
     [copy] Copying 1 file to /Users/dennis/Push/p4/dennis_mac/Build/BenchmarkSuite/load-runner/target/dist
     [copy] Copying 1 file to /Users/dennis/Push/p4/dennis_mac/Build/BenchmarkSuite/load-runner/target/dist
     [copy] Copying 1 file to /Users/dennis/Push/p4/dennis_mac/Build/BenchmarkSuite/load-runner/target/dist
     [copy] Copying 1 file to /Users/dennis/Push/p4/dennis_mac/Build/BenchmarkSuite/load-runner/target/dist
     [copy] Copying 1 file to /Users/dennis/Push/p4/dennis_mac/Build/BenchmarkSuite/load-runner/target/dist
  [copydir] DEPRECATED - The copydir task is deprecated.  Use copy instead.
  [copydir] Copying 6 files to /Users/dennis/Push/p4/dennis_mac/Build/BenchmarkSuite/load-runner/target/dist/etc
  [copydir] DEPRECATED - The copydir task is deprecated.  Use copy instead.
  [copydir] Copying 1 file to /Users/dennis/Push/p4/dennis_mac/Build/BenchmarkSuite/load-runner/target/dist/META-INF
      [zip] Building zip: /Users/dennis/Push/p4/dennis_mac/Build/BenchmarkSuite/load-runner/benchmark-server.zip

all:

BUILD SUCCESSFUL
Total time: 2 seconds

Setup
=====

Ensure that each benchmark client/server has a benchmark-server directory. For example:

# ssh root@my-machine mkdir /root/benchmark-server

Build a benchmark suite distribution

# ant

Usage
=====

1. Run the throughput benchmark with host on machine 'test3' connecting via 4 interfaces on the 192.168.54/24 and 10.0.0/24 networks.

# ant -f throughput-test.xml -Ddiffusion.host=test3 -Ddiffusion.url=ws://192.168.54.31:8080,ws://10.0.0.10:8080,ws://10.0.0.18:8080,ws://10.0.0.22:8080 -Ddiffusion.client.nics=192.168.54.21,10.0.0.9,10.0.0.17,10.0.0.21

When the invocation is performed from the same host a local test run is performed. When the invocation is run from a client machine than the distributable is shipped via ssh/scp, a diffusion server launched, the benchmark publisher distributiond eployed and the client run against this distribution.

Note that all test combinations will be run to completion.

2. Run the throughput benchmark on the local host only and run only non conflated variant.

# ant -DconflationMode=NONE -f throughput-test.xml

3. Run the throughput benchmark with server on test3 and client on 'this machine'. Run a subset of tests for all test variants but with a payload size of 1000 bytes only.

# ant -f throughput-test.xml -Ddiffusion.host=test3 -Ddiffusion.url=ws://10.0.0.18:8080 -Ddiffusion.client.nics=10.0.0.17 -Dtest.name.contains=1000b

When launched on test2 will run the server on test3, client on test2 and utilize the solarflare nic for connection. Only the 1000b tests will be run.

Notes
=====

Manual test runs can also be orchestrated through diligent use of sub tasks comprised in the above automated
examples. For example:

Start diffusion on test3 if not already running and deploy benchmark suite:

ant -f throughput-test.xml -Ddiffusion.host=test3 before before-suite start-injector

This can be run from test3 or any other machine from which test3 is visible & accessible (and ssh is configured).

Once a server is running, individual tests can be selected and run from the same or another host. The
appropriate targets in throughput-test.xml are as follows:

  perfTest-1000b-50t
  perfTest-125b-50t
  perfTest-2000b-50t
  perfTest-250b-50t
  perfTest-500b-50t

As Waratek deviates from standard JVM environments at this time with respect to usage of jps to orchestrate
killing running jvm instances in a standard way within ant some manual intervention is required before a test
run at this time. The default diffusion.sh file should be changed to something like:

  #!/bin/sh
  DIR=`dirname $0`
  cd $DIR
  CP=../lib/diffusion.jar
  CP=${CP}:../etc
  CP=${CP}:../data

  /root/waratek-package/target/open/jdk/jre/bin/javad -Xdaemon  -Dcom.waratek.javaagent=-javaagent:../lib/licenceagent.jar=../etc/licence.lic,../etc/publicKeys.store -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=../logs -cp ${CP} com.pushtechnology.diffusion.Diffusion $1

Unless cleanly shutdown Waratek jvm instances leaves behind detritus in /var/lib/javad. So if you start
a cloud vm named 'jvm-1' you should recursively delete any artefacts created by waratek so you can clean
start:

  rm -rf /var/lib/javad/jvm-1

Enjoy!
