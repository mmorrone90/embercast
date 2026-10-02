#!/usr/bin/env python3
"""Send a local Annex B H.264 sample through the Android phone mirror protocol."""

import argparse
import re
import socket
import struct
import time


def access_units(data):
    starts = [match.start() for match in re.finditer(b"\x00\x00\x00\x01", data)]
    current = bytearray()
    for index, start in enumerate(starts):
        end = starts[index + 1] if index + 1 < len(starts) else len(data)
        unit = data[start:end]
        if len(unit) < 5:
            continue
        if unit[4] & 31 == 9 and current:
            yield bytes(current)
            current.clear()
        current.extend(unit)
    if current:
        yield bytes(current)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("sample", help="Annex B H.264 file with access unit delimiters")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=7102)
    parser.add_argument("--width", type=int, default=640)
    parser.add_argument("--height", type=int, default=360)
    parser.add_argument("--fps", type=int, default=20)
    args = parser.parse_args()
    frames = list(access_units(open(args.sample, "rb").read()))
    with socket.create_connection((args.host, args.port), timeout=60) as connection:
        connection.sendall(struct.pack(">iii", 0x464D5031, args.width, args.height))
        print("Waiting for approval on the TV", flush=True)
        approval = connection.recv(1)
        if approval != b"\x01":
            raise SystemExit("TV denied or did not answer")
        for frame in frames:
            connection.sendall(struct.pack(">iq", len(frame), time.monotonic_ns()) + frame)
            time.sleep(1 / args.fps)
        print(f"Sent {len(frames)} video frames", flush=True)
        time.sleep(2)


if __name__ == "__main__":
    main()
