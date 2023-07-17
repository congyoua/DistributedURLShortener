#!/bin/bash

# NOTE: Make sure sshd is installed and password-less ssh is set up

# Attempt to kill (SIGTERM) all related processes on every host
for host in `cat hosts`
do
    ssh $host "pkill -f Component.LaunchAdmin"
    ssh $host "pkill -f Component.LaunchLoadBalancer"
    ssh $host "pkill -f Component.LaunchNode"
    ssh $host "pkill -f Component.LaunchDatabase"
done
