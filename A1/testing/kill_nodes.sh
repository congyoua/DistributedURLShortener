#!/bin/bash

for host in `cat ../hosts`
do
    ssh $host "pkill -f Component.LaunchNode"
done
