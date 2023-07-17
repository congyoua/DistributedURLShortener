#!/bin/bash

# NOTE: Make sure sshd is installed and password-less ssh is set up

class_path="$PWD/out/production/A1:$PWD/resources/sqlite-jdbc-3.39.3.0.jar"
java_file="Component.LaunchAdmin"

# Launch admin on the local machine
java -cp $class_path $java_file &

# Launch appropriate processes on the corresponding hosts
for line in `cat config`
do
    if [ $line = "LoadBalancers:" ]; then
        java_file="Component.LaunchLoadBalancer"
    elif [ $line = "Nodes:" ]; then
        java_file="Component.LaunchNode"
    elif [ $line = "Databases:" ]; then
        java_file="Component.LaunchDatabase"
    else
        host=${line%:*}
        ssh $host "cd $PWD; nohup java -cp $class_path $java_file" &
    fi
done
