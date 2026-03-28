import re
import time
import numpy as np
import requests
from bs4 import BeautifulSoup
from sentence_transformers import SentenceTransformer

SOURCES = [
    {
        "title": "Color Theory for the Color-Blind",
        "url": "https://www.digital-web.com/articles/color_theory_for_the_colorblind/",
    },
    {
        "title": "DaltonLens: Understanding LMS Color Blindness Simulations",
        "url": "https://daltonlens.org/understanding-cvd-simulation/",
    },
    {
        "title": "DaltonLens: Review of Open Source Color Blindness Simulations",
        "url": "https://daltonlens.org/opensource-cvd-simulation/",
    },
    {
        "title": "DaltonLens: Color Blindness Simulator Reference",
        "url": "https://daltonlens.org/colorblindness-simulator",
    },
]

CHUNK_SIZE    = 150
CHUNK_OVERLAP = 30
EMBED_MODEL   = "all-MiniLM-L6-v2"
TOP_K         = 4

_chunks: list[dict] = []
_embeddings: np.ndarray | None = None
_model: SentenceTransformer | None = None

def _fetch_text(url: str) -> str:
    try:
        resp = requests.get(url, timeout=15, headers={"User-Agent": "Mozilla/5.0"})
        resp.raise_for_status()
        soup = BeautifulSoup(resp.text, "html.parser")
        for tag in soup(["script", "style", "nav", "footer", "header"]):
            tag.decompose()
        text = soup.get_text(separator=" ")
        return re.sub(r"\s+", " ", text).strip()
    except Exception as e:
        print(f"[rag] Failed to fetch {url}: {e}")
        return ""


def _chunk_text(text: str, title: str) -> list[dict]:
    words = text.split()
    chunks, start = [], 0
    while start < len(words):
        end = min(start + CHUNK_SIZE, len(words))
        chunks.append({"text": " ".join(words[start:end]), "source": title})
        if end == len(words):
            break
        start += CHUNK_SIZE - CHUNK_OVERLAP
    return chunks

def _cosine_similarity(query_vec: np.ndarray, matrix: np.ndarray) -> np.ndarray:
    q = query_vec / (np.linalg.norm(query_vec) + 1e-10)
    m = matrix / (np.linalg.norm(matrix, axis=1, keepdims=True) + 1e-10)
    return m @ q

def build_index() -> None:
    global _chunks, _embeddings, _model

    print("[rag] Loading embedding model (downloads once, ~90MB) ...")
    _model = SentenceTransformer(EMBED_MODEL)
    print("[rag] Model loaded.")

    all_chunks: list[dict] = []
    for source in SOURCES:
        print(f"[rag] Fetching: {source['title']} ...")
        text = _fetch_text(source["url"])
        if text:
            all_chunks.extend(_chunk_text(text, source["title"]))
        time.sleep(0.5)

    if not all_chunks:
        print("[rag] No documents fetched — index is empty.")
        return

    print(f"[rag] Embedding {len(all_chunks)} chunks locally ...")
    texts = [c["text"] for c in all_chunks]
    _embeddings = _model.encode(texts, show_progress_bar=True, convert_to_numpy=True)
    _chunks = all_chunks
    print(f"[rag] Index ready: {len(_chunks)} chunks, dim={_embeddings.shape[1]}")

def retrieve(query: str) -> str:
    if _embeddings is None or not _chunks:
        return "Knowledge base not available — index was not built."

    query_vec = _model.encode([query], convert_to_numpy=True)[0]
    scores = _cosine_similarity(query_vec, _embeddings)
    top_indices = np.argsort(scores)[::-1][:TOP_K]

    passages = []
    for idx in top_indices:
        chunk = _chunks[int(idx)]
        score = scores[int(idx)]
        passages.append(f"[{chunk['source']} | relevance: {score:.2f}]\n{chunk['text']}")

    return "\n\n---\n\n".join(passages)

build_index()