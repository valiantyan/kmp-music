from __future__ import annotations

import hashlib
import math
import re
from collections import Counter
from typing import Dict, Iterable, List, Mapping

TOKEN_RE = re.compile(r"[\w\u4e00-\u9fff]+", re.UNICODE)


def normalize_text(text: str) -> str:
    return re.sub(r"\s+", " ", (text or "").strip().lower())


def token_set(text: str) -> set[str]:
    return set(TOKEN_RE.findall(normalize_text(text)))


def ngram_vector(text: str, n_min: int = 3, n_max: int = 5, max_features: int = 4096) -> Dict[int, float]:
    s = normalize_text(text)
    if not s:
        return {}
    counts: Counter[int] = Counter()
    padded = f"  {s}  "
    for n in range(n_min, n_max + 1):
        if len(padded) < n:
            continue
        for i in range(0, len(padded) - n + 1):
            gram = padded[i:i+n]
            h = int(hashlib.blake2b(gram.encode("utf-8"), digest_size=4).hexdigest(), 16) % max_features
            counts[h] += 1
    norm = math.sqrt(sum(v * v for v in counts.values())) or 1.0
    return {k: v / norm for k, v in counts.items()}


def cosine_sparse(a: Mapping[int, float], b: Mapping[int, float]) -> float:
    if not a or not b:
        return 0.0
    if len(a) > len(b):
        a, b = b, a
    return max(0.0, min(1.0, sum(v * b.get(k, 0.0) for k, v in a.items())))


def jaccard(a: Iterable[str], b: Iterable[str]) -> float:
    sa, sb = set(a), set(b)
    if not sa or not sb:
        return 0.0
    return len(sa & sb) / len(sa | sb)


def semantic_similarity(a: str, b: str, cfg: dict | None = None) -> float:
    cfg = cfg or {}
    emb_cfg = cfg.get("embedding", {}) if isinstance(cfg, dict) else {}
    n_min = int(emb_cfg.get("ngram_min", 3))
    n_max = int(emb_cfg.get("ngram_max", 5))
    max_features = int(emb_cfg.get("max_features", 4096))
    v1 = ngram_vector(a, n_min=n_min, n_max=n_max, max_features=max_features)
    v2 = ngram_vector(b, n_min=n_min, n_max=n_max, max_features=max_features)
    char_score = cosine_sparse(v1, v2)
    token_score = jaccard(token_set(a), token_set(b))
    return max(char_score, token_score * 0.9)
