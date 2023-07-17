import argparse
import random
import string
import subprocess


def send(requests: int) -> None:
    for _ in range(requests):
        short = ''.join(random.choices(string.ascii_uppercase + string.ascii_lowercase + string.digits, k=10))
        request = 'http://localhost:8080/' + short
        subprocess.call(['curl', '-X', 'GET', request], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="Send random GET requests from 1 user.")
    parser.add_argument('requests', type=int, help="the number of requests to send")
    args = parser.parse_args()

    send(args.requests)
