#!/bin/bash

./ab-graph.sh -u http://localhost:8012/0000000 -n 1000 -c 10 -k
./ab-graph.sh -u http://localhost:8012/0000000 -n 10000 -c 50 -k