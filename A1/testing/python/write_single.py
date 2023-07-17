import argparse
import random
import string
import subprocess


def send(requests: int) -> None:
    for _ in range(requests):
        short = ''
        long = 'qweqwe'
        request = 'http://localhost:8012/?short=' + short + '&long=' + long
        subprocess.call(['curl', '-X', 'PUT', request], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="Send random PUT requests from 1 user.")
    parser.add_argument('requests', type=int, help="the number of requests to send")
    args = parser.parse_args()

    send(args.requests)
