#Diffusion Benchmark Suite
##Overview
This project is a collection of synthetic benchmarks, or *experiments*, used to
drive performance of the [Diffusion](http://docs.pushtechnology.com/) product.

Our framework consists of:

* Ant scripts used to start/stop the Diffusion server (local/remote)
* Ant scripts for packaging and deploying publishers (by copying `.dar` files into
the deploy folder of the server)
* Soft cushions!  

### Minimum Diffusion version

This version of the benchmark suite supports Diffusion 5.1.0 and higher only.


Currently implemented experiments are:

#### Throughput

A broadcasting publisher is set up, publishing at a uniform rate across a set
number of topics. A population of clients subscribes to all topics.
    
The experiment can be set up to examine server behavior for different types of
load resulting from a growing client population / increase of topics /
increase of messages / message size. The experiment reports throughput from the clients' perspective.
    
#### Latency

A pong/echo publisher is set up which "echo"s clients messages back to them.
The experiment allows the number concurrently pinging clients to be
controlled. Clients ping as fast as they can. The ping service can either
respond to the particular client or broadcast on the ping topic.
    
#### Control Client Latency

A client is connected and uses Control features to set up an Echo topic as
described above. We connect clients to the server and measure response time
(Client <-> Server <-> Control Client). The Control Client must connect to a
Connector that supports the client type UCI. It does not require a publisher
to be configured.

This experiment uses the MessagingControl feature in the
control client to listen for and send messages. The MessagingControl feature
will respond to the control client with a callback to indicate the success or
failure of the message. To do this a finite amount of space is allocated to
track the outstanding messages. To prevent this space being completely
consumed you need to configure an additional setting on the server
`diffusion.timeout.tick=-1`. This can be done by adding it as a `-D` flag in
the `diffusion.sh` file of the server you are running the experiment against.

#### Control Client Throughput + Latency

A client is connected and uses Control features to set up a topic tree
similar to the one used in the throughput experiment. Latency is measured
from Control Client client to clients (Control Client -> Server -> Client). The
Control Client must connect to a Connector that supports the client type UCI.
It does not require a publisher to be configured.

##Building the benchmarks distributable
To build and run the benchmarks it is assumed that you have the following installed:

* Ant (>1.8)
* Java JDK (>1.7)
* Diffusion (>5)

You will need to add the JDK and Ant `bin` folders to your `PATH`. You will
also need to define `DIFFUSION_HOME` to point to the Diffusion installation.
On Unix-like systems it is possible to use the `env` command to setup a
temporary environment.

At this point you can use the `build.xml` in the project directory to build the
benchmarks distributable. Invoking `ant` or `ant dist` should result
in a packaged `benchmark-server.zip` file being created in the project directory.

##Deploying the benchmarks
The target machine used for running the benchmarks has the same
dependency requirements described above for building (JDK/Ant/Diffusion). It also requires a valid Diffusion licence file with sufficient allowances to support your test.
You will need to configure your server to support the required number
of connections and allow incoming connections on the desired ports. See the
Diffusion server configuration and tuning manual for detailed instructions.
To deploy the scripts you'll need to extract the `benchmark-server.zip` into
a folder named `benchmark-server`.

    $ unzip benchmark-server.zip -d benchmark-server

##Before you run the benchmarks!!!
The benchmarks are configured for running on tuned commodity servers, not your
average laptop. This is particularly true when running both server and client
on the same host. To run the benchmarks in your IDE refer to the relevant section
below.

The server on which the benchmarks are to be run should be configured to allow
large numbers of connections. On Unix, add file descriptors as follows:

    ulimit -n 100000

To persist the changes after a restart edit `/etc/security/limits.conf` (or `/etc/limits.conf`) and add the following lines:

    *                hard    nofile          100000
    *                soft    nofile          100000

If you run you client process via a single IP you may exhaust the IP range (by
opening too many local connections, each client requires a local port). To
avoid, either access your server via multiple addresses (explained below)
or extend the IP range as described [here](http://stackoverflow.com/questions/6145108/problem-running-into-java-net-bindexception-cannot-assign-requested-address).

###Tuning your Diffusion installation

The default Diffusion configuration is not optimized for running benchmarks. You may want to consider making the following changes in order to achieve maximum performance in a server environment.

Modify your Diffusion start script (`$DIFFUSION_HOME/bin/diffusion.sh`) to include the following:

    numactl -N 1 -m 1 java -server -Xms4g -Xmx4g
    
This pins the server process to one CPU socket and increases the default amount of memory available.

Modify `$DIFFUSION_HOME/etc/Server.xml` changing:

    multiplexers/multiplexer-definition/size    -> 12
    thread-pools/thread-pool-definition/core-size -> 2
    thread-pools/thread-pool-definition/max-size -> 2
    thread-pools/thread-pool-definition/queue-size -> 200000

This optimizes the number of inbound threads and associated queues to cope with a large number of clients.

Run the benchmark scripts, pinning the client process to a CPU socket, so for example:

    numactl -N 0 -m 0 ant -f throughput-suite.xml -Dtest.name.contains=125
    
Note that you are pinning the client to the other CPU socket here (hence the 0 argument rather than 1).
  
##Running the benchmarks
The Ant scripts deployed with the application support running the benchmarks
in a variety of configurations. The benchmarks can be run as a suite (similar
to JUnit test suites) with before/after tasks run before/after every benchmark.
Target names which start with *perfTest* are assumed benchmarks.

We'll work through the options by example. First, open a terminal and change to the `benchmark-server` directory.

####Run the throughput suite against localhost

    $ ant -f throughput-suite.xml

The default target for the `throughput-suite.xml` is the __perf-suite__ target
which will launch all the tests included. This run will default to testing the
Web Sockets transport with no conflation enabled.

The suite contains many tests. If your trying this on laptop, it may take about 30 minutes to complete.

By the end of the run we will expect to have a set of experiment settings and
output files. The settings files are __not over written__ and thus allow for
manual tweaking. If you change the settings and re-run the settings from the
file will be used.

####Run a subset of the throughput suite against localhost

    $ ant -f throughput-suite.xml -Dtest.name.contains=125b

This will result in only the __perfTest-125b-50t__ benchmark being run, but
within the framework of a suite so the before and after tasks are executed.

####Run the throughput suite against localhost with different transport/conflation

    $ ant -f throughput-suite.xml -Ddiffusion.transport=dpt -Dconflation.mode=REPLACE

This will result in a full suite run for the DPT transport using `REPLACE` conflation.
You can read more about the different Diffusion transports and conflation modes
in the User Manual.

####Run the throughput suite against localhost with different client jvm settings

    $ ant -f throughput-suite.xml -Dclient.threads=1 -Dclient.jvm.args="-server -Xms128m -Xmx128m"

This will launch the full suite but with client JVM launched with above arguments
and using a single incoming thread.

####Run the throughput suite against a different machine

    $ ant -f throughput-test.xml -Ddiffusion.host=test3

If `diffusion.host` is not `localhost` the Ant scripts will attempt to
start/stop the diffusion server deployed remotely and deploy to it. The
assumption being made that the remote host is setup for running the benchmarks
in the same way the local machine is. The scripts use SSH to launch themselves
on the remote machine.

This sucks a bit and we shall be looking to improve on this mechanism
shortly. For now however you will need to set the `ssh.username` and
`ssh.password` properties to allow the scripts SSH access. You'll also need
to have the `jsch.jar` in your `$ANT_HOME/lib`.

####Run the throughput benchmark with host on machine 'test3' connecting via 4 interfaces on the 192.168.54/24 and 10.0.0/24 networks.

    $ ant -f throughput-test.xml -Ddiffusion.host=test3 -Ddiffusion.url=ws://192.168.54.31:8080,ws://10.0.0.10:8080,ws://10.0.0.18:8080,ws://10.0.0.22:8080 -Ddiffusion.client.nics=192.168.54.21,10.0.0.9,10.0.0.17,10.0.0.21 <br>

This will launch the full suite running the host on `test3` and using several
urls and client NICs for connectivity. This allows you to mix and match
protocols and test your server using several outgoing/incoming network
interfaces. Note that the list of URLs is used in conjunction with the list of
NICs and thus url[i] must be available on nic[i].

##Running/controlling tests manually

##Notes
### Notes for running in Eclipse
If you have the Diffusion server installed locally on your developer box and
wish to run these benchmarks from Eclipse you can install the Diffusion Eclipse
Plugin and do just that! Use the `RunPublishers.launch` (right click, run as)
launcher to start the service and create a launcher/use an existing one for your
selected experiment.

Note that this is intended mostly as a means to validate your code, not as a
benchmarking environment for best results. 

##Working with the code
###Checkstyle
In the `fomatter` directory there is a checkstyle style file `sun_checks.xml`.
That the code has been developed against this can be integrated into your
development process to ensure that the style is consistent through out the
suite. Worth noting is that you may need to change the line ending required
at the end of the file, its correctness will depend on how you checkout the
source code.

Enjoy!
