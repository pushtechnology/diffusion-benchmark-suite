#Diffusion Benchmark Suite
##Overview
This project is a collection of synthetic benchmarks, or experiments, used to
drive performance of the diffusion product. Our framework consists of:

* Ant scripts used to start/stop the Diffusion server (local/remote)
* And scripts for packaging and deploying publishers (by copying .dar files into
the deploy folder of the server)  

Currently implemented experiments are:

* Throughput experiment:<br>
    A broadcasting publisher is set up, publishing at a uniform rate across a 
    set number of topics. A population of clients subscribes to all topics. The
    experiment can be set up to examine server behaivour for different types of
    load resulting from a growing client population/increase of topics/increase
    of messages/message size. The experiment reports throughput from the clients
    perspective.
* Latency experiment:<br>
    A pong/echo publisher is set up which 'echo's clients messages back to them.
    The experiment allows for controlling the number concurrently pinging
    clients. Clients ping as fast as they can.
* Client churn test (coming soon):<br>
    A population of clients connects and subscribe, clients disconnect randomly
    after a set period of time.

##Building the benchmarks distributable
To build and run the benchmarks it is assumed that you have already installed:

* Ant (>1.8)
* Java JDK (>1.6)
* Diffusion (>4)

You will need to add the JDK and Ant bin folders to your PATH. <br>
You will also need to define DIFFUSION_HOME<br>
At this point you can use the build.xml in the project directory to build the
benchmarks distributable. Invoking __ant__ or __ant dist__ should result
in a packaged __benchmark-server.zip__ file being created in the project dir.<br>
##Deploying the benchmarks
The target machine used for running the benchmarks requires the same
dependencies described above for building (JDK/Ant/Diffusion). It also requires
a valid Diffusion licence file with sufficient allowances to support your test.
Similarly you will need to configure your server to support the required number
of connections and allow incoming connections on the desired ports. See the
diffusion server configuration and tuning manual for detailed instructions.
  
##Running the benchmarks

##Example command line usage of benchmark suite

####1. Run the throughput benchmark with host on machine 'test3' connecting via 4 interfaces on the 192.168.54/24 and 10.0.0/24 networks.

$ ant -f throughput-test.xml -Ddiffusion.host=test3 -Ddiffusion.url=ws://192.168.54.31:8080,ws://10.0.0.10:8080,ws://10.0.0.18:8080,ws://10.0.0.22:8080 -Ddiffusion.client.nics=192.168.54.21,10.0.0.9,10.0.0.17,10.0.0.21

When the invocation is performed from the same host a local test run is performed. When the invocation is run from a client machine than the distributable is shipped via ssh/scp, a diffusion server launched, the benchmark publisher distributiond eployed and the client run against this distribution.

Note that all test combinations will be run to completion.

####2. Run the throughput benchmark on the local host only and run only non conflated variant.

$ ant -DconflationMode=NONE -f throughput-test.xml

####3. Run the throughput benchmark with server on test3 and client on 'this machine'. Run a subset of tests for all test variants but with a payload size of 1000 bytes only.

$ ant -f throughput-test.xml -Ddiffusion.host=test3 -Ddiffusion.url=ws://10.0.0.18:8080 -Ddiffusion.client.nics=10.0.0.17 -Dtest.name.contains=1000b

When launched on test2 will run the server on test3, client on test2 and utilize the solarflare nic for connection. Only the 1000b tests will be run.

##Running/controlling tests manually

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
##Notes

### Notes for IBM JDK
### Notes for Zing
### Notes for Waratek
As Waratek deviates from standard JVM environments at this time with respect to usage of jps to orchestrate
killing running jvm instances in a standard way within ant some manual intervention is required before a test
run at this time. The default diffusion.sh file should be changed to something like:

>  #!/bin/sh<br>
>  DIR=`dirname $0`<br>
>  cd $DIR<br>
>  CP=../lib/diffusion.jar<br>
>  CP=${CP}:../etc<br>
>  CP=${CP}:../data<br>

>  /root/waratek-package/target/open/jdk/jre/bin/javad -Xdaemon  -Dcom.waratek.javaagent=-javaagent:../lib/licenceagent.jar=../etc/licence.lic,../etc/publicKeys.store -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=../logs -cp ${CP} com.pushtechnology.diffusion.Diffusion $1<br>

Unless cleanly shutdown Waratek jvm instances leaves behind detritus in /var/lib/javad. So if you start
a cloud vm named 'jvm-1' you should recursively delete any artefacts created by waratek so you can clean
start:

  rm -rf /var/lib/javad/jvm-1

Enjoy!
