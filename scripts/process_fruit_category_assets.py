#!/usr/bin/env python3
"""Resize and compress generated fruit category banners for the Android app."""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image, ImageDraw, ImageOps


ASSET_NAMES = (
    "fruit-citrus",
    "fruit-melon",
    "fruit-tropical",
    "fruit-orchard",
    "fruit-stone",
    "fruit-berry",
    "fruit-taiwan",
)
OUTPUT_SIZE = (768, 512)
MAX_BYTES = 96 * 1024


def process(source: Path, target: Path) -> int:
    with Image.open(source) as image:
        image = ImageOps.exif_transpose(image).convert("RGB")
        image = ImageOps.fit(
            image,
            OUTPUT_SIZE,
            method=Image.Resampling.LANCZOS,
            centering=(0.5, 0.5),
        )
        for quality in range(80, 59, -2):
            image.save(target, "WEBP", quality=quality, method=6)
            if target.stat().st_size <= MAX_BYTES:
                break

    with Image.open(target) as verified:
        if verified.format != "WEBP" or verified.size != OUTPUT_SIZE:
            raise ValueError(f"Invalid output: {target}")
    if target.stat().st_size > MAX_BYTES:
        raise ValueError(f"Asset exceeds {MAX_BYTES} bytes: {target}")
    return target.stat().st_size


def contact_sheet(output_dir: Path, target: Path) -> None:
    thumb_size = (384, 256)
    label_height = 28
    columns = 2
    rows = (len(ASSET_NAMES) + columns - 1) // columns
    sheet = Image.new(
        "RGB",
        (columns * thumb_size[0], rows * (thumb_size[1] + label_height)),
        "#101815",
    )
    draw = ImageDraw.Draw(sheet)
    for index, name in enumerate(ASSET_NAMES):
        with Image.open(output_dir / f"{name}.webp") as image:
            thumb = image.convert("RGB").resize(thumb_size, Image.Resampling.LANCZOS)
        x = (index % columns) * thumb_size[0]
        y = (index // columns) * (thumb_size[1] + label_height)
        sheet.paste(thumb, (x, y))
        draw.text((x + 8, y + thumb_size[1] + 6), name, fill="white")
    target.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(target, "JPEG", quality=88, optimize=True)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-dir", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--contact-sheet", type=Path)
    args = parser.parse_args()

    args.output_dir.mkdir(parents=True, exist_ok=True)
    total = 0
    for name in ASSET_NAMES:
        source = args.source_dir / f"{name}.png"
        target = args.output_dir / f"{name}.webp"
        if not source.is_file():
            raise FileNotFoundError(source)
        size = process(source, target)
        total += size
        print(f"{target.name}: {size} bytes")

    if args.contact_sheet:
        contact_sheet(args.output_dir, args.contact_sheet)
        print(f"Contact sheet: {args.contact_sheet}")
    print(f"Total: {total} bytes")


if __name__ == "__main__":
    main()
