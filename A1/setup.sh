#!/bin/bash

# NOTE: Make sure sshd is installed and password-less ssh is set up

# Create database file on hosts
for host in `cat hosts`
do
    ssh $host "cd $PWD; rm -rf /virtual/$USER; mkdir /virtual/$USER; sqlite3 /virtual/$USER/url.db < schema.sql"
done
