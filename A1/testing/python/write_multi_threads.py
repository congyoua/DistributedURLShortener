import argparse
import random
import string
import subprocess
import threading


def send(requests: int) -> None:
    for _ in range(requests):
        short = ''.join(random.choices(string.ascii_uppercase + string.ascii_lowercase + string.digits, k=10))
        long = 'https://' \
               + ''.join(random.choices(string.ascii_uppercase + string.ascii_lowercase + string.digits, k=10)) \
               + '.com'
        request = 'http://localhost:8012/?short=' + short + '&long=' + long
        subprocess.call(['curl', '-X', 'PUT', request], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="Send random PUT requests from multiple users (threads).")
    parser.add_argument('requests', type=int, help="the number of requests to send")
    parser.add_argument('users', type=int, help="the number of users")
    args = parser.parse_args()

    threads = []
    for _ in range(args.users):
        t = threading.Thread(target=send, args=(args.requests,))
        t.start()
        threads.append(t)
    for t in threads:
        t.join()
