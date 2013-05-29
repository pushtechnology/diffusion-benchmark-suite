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
To deploy the scripts you'll need to extract the __benchmark-server.zip__ into
a folder named __benchmark-server__.<br>
>    $ unzip benchmark-server.zip -d benchmark-server

##Running the benchmarks
The ant scripts deployed with the application support running the benchmarks
in a variety of configurations. The benchmarks can be run as a suite (similar
to junit test suites) with before/after tasks run before/after every benchmark.
Targets names which start with perfTest are assumed benchmarks. We'll work
through the options by example:

####Run the throughput suite against localhost.
>    $ ant -f throughput-suite.xml<br>

The default target for the throughput-suite.xml is the __perf-suite__ target
which will launch all the tests included. This run will default to testing the
Web Sockets transport with no conflation enabled.<br>
By the end of the run we will expect to have a set of experiment settings and
output files. The settings files are __not over written__ and thus allow for
manual tweaking. If you change the settings and re-run the settings from the
file will be used.

####Run a subset of the throughput suite against localhost.
>    $ ant -f throughput-suite.xml -Dtest.name.contains=125b<br>

This will result in only the __perfTest-125b-50t__ benchmark being run, but
within the framework of a suite so the before and after tasks are executed.

####Run the throughput suite against localhost with different transport/conflation
>    $ ant -f throughput-suite.xml -Ddiffusion.transport=dpt -Dconflation.mode=REPLACE<br>

This will result in a full suite run for the DPT transport using REPLACE conflation.
You can read more about the different Diffusion transports and conflation modes
in the User Manual.

####Run the throughput suite against localhost with different client jvm settings
>   $ ant -f throughput-suite.xml -Dclient.threads=1 -Dclient.jvm.args="-server -Xms128m -Xmx128m"

This will launch the full suite but with client jvm launched with above args
and using a single incoming thread.

####Run the throughput suite against a different machine
>    $ ant -f throughput-test.xml -Ddiffusion.host=test3<br>

If __diffusion.host__ is not localhost the ant scripts will attempt to
start/stop the diffusion server deployed remotely and deploy to it. The
assumption being made that the remote host is setup for running the benchmarks
in the same way the local machine is. The scripts use SSH to launch themselves
on the remote machine.<br>
This sucks a bit and we shall be looking to improve on this mechanism
shortly. For now however you will need to set the __ssh.username__ and
__ssh.username__ properties to allow the scripts ssh access. You'll also need
to have the jsch.jar in your $ANT_HOME/lib.

####Run the throughput benchmark with host on machine 'test3' connecting via 4 interfaces on the 192.168.54/24 and 10.0.0/24 networks.

>    $ ant -f throughput-test.xml -Ddiffusion.host=test3 -Ddiffusion.url=ws://192.168.54.31:8080,ws://10.0.0.10:8080,ws://10.0.0.18:8080,ws://10.0.0.22:8080 -Ddiffusion.client.nics=192.168.54.21,10.0.0.9,10.0.0.17,10.0.0.21 <br>

This will launch the full suite running the host on test3 and using several
urls and client NICs for connectivity. This allows you to mix and match
protocols and test your server using several outgoing/incoming network
interfaces. Note that the list of urls is used in conjunction with the list of
NICs and thus url[i] must be available on nic[i].

##Running/controlling tests manually


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
