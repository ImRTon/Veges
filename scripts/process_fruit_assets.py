from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_SOURCE = ROOT / "tmp/imagegen/fruit-alpha"
DEFAULT_OUTPUT = ROOT / "data/src/main/assets/illustrations/catalog"
MAX_ASSET_BYTES = 128 * 1024


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Resize transparent fruit PNG sources into app-ready WebP assets.",
    )
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--max-edge", type=int, default=512)
    parser.add_argument("--quality", type=int, default=80)
    parser.add_argument(
        "--validate-only",
        action="store_true",
        help="Validate already-compressed outputs without re-encoding them.",
    )
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    sources = sorted(args.source.glob("fruit-*.png"))
    if not sources:
        raise ValueError(f"No fruit PNG sources found under {args.source}")
    args.output.mkdir(parents=True, exist_ok=True)

    assets: list[dict] = []
    hashes: set[str] = set()
    for source in sources:
        destination = args.output / f"{source.stem}.webp"
        with Image.open(source) as opened:
            image = opened.convert("RGBA")
            if image.getbbox() is None:
                raise ValueError(f"Source has no visible subject: {source.name}")
            source_corners = (
                image.getpixel((0, 0))[3],
                image.getpixel((image.width - 1, 0))[3],
                image.getpixel((0, image.height - 1))[3],
                image.getpixel((image.width - 1, image.height - 1))[3],
            )
            if any(source_corners):
                raise ValueError(f"Source corners are not transparent: {source.name}")
            if not args.validate_only:
                image.thumbnail(
                    (args.max_edge, args.max_edge),
                    Image.Resampling.LANCZOS,
                )
                image.save(
                    destination,
                    "WEBP",
                    quality=args.quality,
                    method=6,
                    exact=True,
                )

        if not destination.exists():
            raise ValueError(f"Compressed asset is missing: {destination.name}")

        data = destination.read_bytes()
        digest = hashlib.sha256(data).hexdigest()
        if digest in hashes:
            raise ValueError(f"Duplicate compressed image bytes: {destination.name}")
        hashes.add(digest)
        if len(data) > MAX_ASSET_BYTES:
            raise ValueError(
                f"Compressed asset exceeds {MAX_ASSET_BYTES} bytes: "
                f"{destination.name} ({len(data)} bytes)",
            )

        with Image.open(destination) as compressed:
            rgba = compressed.convert("RGBA")
            corners = (
                rgba.getpixel((0, 0))[3],
                rgba.getpixel((rgba.width - 1, 0))[3],
                rgba.getpixel((0, rgba.height - 1))[3],
                rgba.getpixel((rgba.width - 1, rgba.height - 1))[3],
            )
            if compressed.format != "WEBP":
                raise ValueError(f"Unexpected format: {destination.name}")
            if any(corners):
                raise ValueError(
                    f"Compressed asset corners are not transparent: {destination.name}",
                )
            if max(compressed.size) > args.max_edge:
                raise ValueError(f"Compressed asset exceeds edge limit: {destination.name}")

        assets.append(
            {
                "asset": destination.name,
                "bytes": len(data),
                "sha256": digest,
                "width": rgba.width,
                "height": rgba.height,
                "transparentCorners": True,
            },
        )

    summary = {
        "assetCount": len(assets),
        "totalBytes": sum(asset["bytes"] for asset in assets),
        "maxBytes": max(asset["bytes"] for asset in assets),
        "maxEdge": args.max_edge,
        "quality": args.quality,
        "uniqueHashes": len(hashes) == len(assets),
        "assets": assets,
    }
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
