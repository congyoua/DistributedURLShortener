import argparse
import os
import subprocess
import time
if __name__ == '__main__':
    start = time.time()
    parser = argparse.ArgumentParser(description="Send random PUT requests from multiple users (hosts).")
    parser.add_argument('requests', type=int, help="the number of requests to send")
    args = parser.parse_args()

    hosts = []
    with open('hosts') as file:
        for line in file:
            hosts.append(line.rstrip())

    cmd = f'python3 {os.getcwd()}/write_single.py {args.requests}'
    for host in hosts:
        subprocess.Popen(f'ssh {host} {cmd}', shell=True).communicate()
    end = time.time()
    print("\nWrite_multi_hosts\n# of Requests: ",args.requests,"\ntime: ",(end - start),"\nrequests per sec: ",args.requests/(end - start))