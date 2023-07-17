import argparse
import os
import subprocess

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="Send random GET requests from multiple users (hosts).")
    parser.add_argument('requests', type=int, help="the number of requests to send")
    args = parser.parse_args()

    hosts = []
    with open('hosts') as file:
        for line in file:
            hosts.append(line.rstrip())

    cmd = f'python3 {os.getcwd()}/read_single.py {args.requests}'
    for host in hosts:
        subprocess.Popen(f'ssh {host} {cmd}', shell=True).communicate()
