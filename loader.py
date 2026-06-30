"""
loader.py — download ONE Wikipedia parquet file from Hugging Face, then feed
every article in it into nami's /ingest endpoint.

The download happens once (one ~250-300MB file). After that, articles are read
from the LOCAL file — no per-article network — so the load speed reflects YOUR
app's indexing throughput, not Hugging Face's network.

Run with the venv's Python (Spring app must already be running):
    .venv/bin/python loader.py
"""

import time
import requests
import pyarrow.parquet as pq
from huggingface_hub import hf_hub_download

# ---- settings ---------------------------------------------------------------
INGEST_URL = "http://localhost:8080/ingest"
REPO = "wikimedia/wikipedia"
PARQUET_FILE = "20231101.en/train-00000-of-00041.parquet"   # one file, ~155k articles
# -----------------------------------------------------------------------------

# 1. Download the one parquet file to the local HF cache (one bulk fetch).
#    If it's already downloaded, this returns instantly from cache.
print("Downloading parquet file (one-time, ~250-300MB)...")
local_path = hf_hub_download(repo_id=REPO, filename=PARQUET_FILE, repo_type="dataset")
print(f"Local file: {local_path}")

# 2. Open it. parquet stores data in row-groups; we read group by group so we
#    never load the whole file into memory at once.
parquet = pq.ParquetFile(local_path)
total = parquet.metadata.num_rows
print(f"Articles in file: {total}")

stored = 0
skipped = 0
errors = 0
processed = 0
start = time.time()

# 3. Stream through the local file and POST each article.
for batch in parquet.iter_batches(batch_size=500, columns=["title", "url", "text"]):
    rows = batch.to_pylist()
    for article in rows:
        payload = {
            "title": article["title"],
            "url": article["url"],
            "text": article["text"],
        }
        try:
            resp = requests.post(INGEST_URL, json=payload, timeout=30)
            if resp.status_code == 200:
                stored += 1
            elif resp.status_code == 409:
                skipped += 1
            else:
                errors += 1
        except requests.RequestException as e:
            errors += 1
            print(f"  request failed: {e}")

        processed += 1
        if processed % 1000 == 0:
            elapsed = time.time() - start
            rate = processed / elapsed if elapsed > 0 else 0
            print(f"  {processed}/{total} processed  ({rate:.1f} articles/sec)")

elapsed = time.time() - start
rate = stored / elapsed if elapsed > 0 else 0
print("\n--- done ---")
print(f"stored:  {stored}")
print(f"skipped: {skipped}")
print(f"errors:  {errors}")
print(f"time:    {elapsed:.1f}s")
print(f"rate:    {rate:.1f} articles/sec")
