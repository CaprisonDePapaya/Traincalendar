"""Rebuilds the screenshots BoardFlowTest logs to logcat as base64 chunks.

Usage: split_shots.py <logcat dump> <output dir>
Writes <name>.b64 per screenshot (one JPEG, base64-encoded).
"""
import os
import re
import sys

src, out = sys.argv[1], sys.argv[2]
os.makedirs(out, exist_ok=True)
pattern = re.compile(r"DayBoardShot: (\S+) (\d+)/(\d+) (\S+)")
chunks, totals = {}, {}
with open(src, errors="replace") as f:
    for line in f:
        m = pattern.search(line)
        if m:
            name, i, n, data = m.group(1), int(m.group(2)), int(m.group(3)), m.group(4)
            chunks.setdefault(name, {})[i] = data
            totals[name] = n
for name, parts in sorted(chunks.items()):
    n = totals[name]
    if len(parts) != n:
        print(f"{name}: missing chunks ({len(parts)}/{n})")
        continue
    with open(os.path.join(out, name + ".b64"), "w") as f:
        f.write("".join(parts[i] for i in range(1, n + 1)))
    print(f"{name}: ok")
