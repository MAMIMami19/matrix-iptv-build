#!/usr/bin/env python3
"""Showmax+ virtual addon generator: TMDB -> Stremio catalog JSONs (static).
Env: TMDB_API_KEY (free v3 key). Output dir: ./out (manifest.json + catalog/<type>/<id>.json).
No third-party deps; stdlib only so the GitHub Action needs nothing installed."""
import json, os, sys, urllib.request

API = "https://api.themoviedb.org/3"
KEY = os.environ.get("TMDB_API_KEY", "")
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "out")
IMG = "https://image.tmdb.org/t/p/w500"

def get(path, **params):
    params["api_key"] = KEY
    qs = "&".join(f"{k}={v}" for k, v in params.items())
    with urllib.request.urlopen(f"{API}{path}?{qs}", timeout=30) as r:
        return json.load(r)

def meta(item, mtype):
    poster = item.get("poster_path")
    return {
        "id": f"tmdb:{item['id']}",
        "type": mtype,
        "name": item.get("title") or item.get("name") or "",
        "poster": f"{IMG}{poster}" if poster else None,
        "description": item.get("overview") or "",
        "genres": [],
        "releaseInfo": (item.get("release_date") or item.get("first_air_date") or "")[:4],
    }

def catalog(cid, name, mtype, items):
    return {"id": cid, "type": mtype, "name": name,
            "metas": [meta(i, mtype) for i in items if i.get("poster_path")]}

# Network ids (TMDB) — confident: Netflix 213, HBO 49, Apple 2552, Disney 2739, Paramount 4330, STARZ 1077, Prime 1024. VERIFY before first run: Showmax 3903?, OSN 5611?, myCANAL 1511?, Shahid/MBC (search TMDB). Wrong ids simply yield empty results (safe).
NETWORKS = {"netflix": 213, "apple": 2552, "hbo": 49, "paramount": 4330,
            "disney": 2739, "starz": 1077, "prime": 1024, "showmax": 3903, "osn": 5611, "canal": 1511,
            # Disney+ hubs (ride inside the Disney theme): Hulu + National Geographic
            "hulu": 453, "natgeo": 2058}

def main():
    if not KEY:
        print("TMDB_API_KEY missing", file=sys.stderr); sys.exit(1)
    os.makedirs(f"{OUT}/catalog/movie", exist_ok=True)
    os.makedirs(f"{OUT}/catalog/series", exist_ok=True)
    cats, metas = [], {}
    def add(cid, name, mtype, items):
        c = catalog(cid, name, mtype, items)
        cats.append({"type": mtype, "id": cid, "name": name, "extra": []})
        with open(f"{OUT}/catalog/{mtype}/{cid}.json", "w") as f:
            json.dump({"metas": c["metas"]}, f)
    # Top 10 by country (DZ default + a small set; chips in-app come from these ids)
    for country in ["DZ", "US", "FR", "EG", "SA"]:
        for mtype, kind in [("movie", "movie"), ("series", "tv")]:
            d = get(f"/trending/{kind}/week", region=country)
            add(f"top10_{mtype}_{country.lower()}", f"Top 10 {mtype} {country}", mtype, d["results"][:10])
    # Reality TV + True Crime (genre ids: reality 10764, crime 80; crime-only filter for true crime)
    d = get("/discover/tv", with_genres="10764", sort_by="popularity.desc")
    add("reality_tv", "Télé-réalité", "series", d["results"][:30])
    d = get("/discover/tv", with_genres="80", sort_by="popularity.desc")
    add("true_crime", "True Crime", "series", d["results"][:30])
    d = get("/movie/now_playing", region="DZ")
    add("now_showing", "يُعرض الآن", "movie", d["results"][:30])
    # Platform originals (network-filtered)
    for plat, nid in NETWORKS.items():
        for mtype, kind in [("movie", "movie"), ("series", "tv")]:
            d = get(f"/discover/{kind}", with_networks=str(nid), sort_by="popularity.desc")
            if d["results"]:
                add(f"originals_{plat}_{mtype}", f"{plat} Originals", mtype, d["results"][:30])
    manifest = {
        "id": "showmax.plus.sections", "version": "1.0.0", "name": "Showmax+ Sections",
        "description": "Built-in home sections for Showmax+ (Top 10, reality, true crime, now showing, platform originals).",
        "resources": [{"name": "catalog", "types": ["movie", "series"]}],
        "types": ["movie", "series"], "catalogs": cats, "idPrefixes": ["tmdb:"],
    }
    with open(f"{OUT}/manifest.json", "w") as f:
        json.dump(manifest, f)
    print(f"wrote {len(cats)} catalogs")

if __name__ == "__main__":
    main()
