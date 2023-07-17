# URL Shortener

## Web browser access

The URL shortener system can be accessed through a web browser:
- Proxy server (port 8080) for creating and accessing shorts
- Admin tool (port 8800) for adding, removing, monitoring, and recovering
servers

## Setup

1. Make sure the `hosts` file contains the hostnames of the machines you
are going to be using. Each hostname should be on a separate line.

2. Make sure the `config` file contains `hostname:port` pairs under the
appropriate heading. The headings are:`LoadBalancers:`, `Nodes:`, and
`Databases:` (must be in that order). Each heading and pair should be on a
separate line. The port number of load balancers, nodes, and databases
must be 8080, 8888, and 7777, respectively.

3. Run the bash script `setup.sh` to set up the database files in the
`/virtual` directory. Make sure to run it in the same directory it is
located in.

## Launch

1. Run the bash script `launch.sh` to launch the URL shortener system.
Make sure to run it in the same directory it is located in.

## Shutdown

1. Run the bash script `shutdown.sh` to shut down the URL shortener system.
Make sure to run it in the same directory it is located in.
