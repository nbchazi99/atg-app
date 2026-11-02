"""Builds the wallet marks used in the deposit and payout pickers.

Usage (Pillow required: python -m pip install pillow):
    python tool/build_wallet_logos.py

Sources live in assets/brand/source/ (the files supplied by the desk). EcoCash,
SmileCash and OneMoney arrive on white, so the white is turned into alpha
(the colours are unpremultiplied) and the mark is cropped tight. InnBucks is a
navy tile and is kept opaque. OneMoney's "Mobile" band is cropped away so the
mark sits with the others at the same optical height.
"""

from __future__ import annotations

import os

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "assets", "brand", "source")
OUT = os.path.join(ROOT, "assets", "images", "wallets")

MARKS = {
    "ecocash.png": ("Ecocash.png", False),
    "smilecash.png": ("Smile Cash.png", False),
    "onemoney.png": ("Onemoney.png", False),
    "innbucks.png": ("InnBucks.png", True),
}


def unwhite(im: Image.Image) -> Image.Image:
    """Recover alpha from art that was flattened onto white."""
    im = im.convert("RGBA")
    px = im.load()
    width, height = im.size
    for y in range(height):
        for x in range(width):
            r, g, b, a = px[x, y]
            alpha = 255 - min(r, g, b)
            if alpha <= 3 or a == 0:
                px[x, y] = (0, 0, 0, 0)
                continue
            # Undo the white composite: c = (c - (255 - a)) * 255 / a.
            lift = 255 - alpha
            px[x, y] = (
                max(0, min(255, round((r - lift) * 255 / alpha))),
                max(0, min(255, round((g - lift) * 255 / alpha))),
                max(0, min(255, round((b - lift) * 255 / alpha))),
                alpha,
            )
    return im


def trim(im: Image.Image) -> Image.Image:
    box = im.getbbox()
    return im.crop(box) if box else im


def onemoney_crop(im: Image.Image) -> Image.Image:
    """Cut the solid orange 'Mobile' band off the supplied OneMoney art."""
    im = im.convert("RGBA")
    width, height = im.size
    px = im.load()
    cut = height
    for y in range(int(height * 0.6), height):
        orange = 0
        for x in range(width):
            r, g, b, _ = px[x, y]
            if r > 200 and 80 < g < 180 and b < 90:
                orange += 1
        if orange > width * 0.03:
            cut = max(0, y - 6)
            break
    return im.crop((0, 0, width, cut))


def main() -> None:
    os.makedirs(OUT, exist_ok=True)
    for target, (source, opaque) in MARKS.items():
        path = os.path.join(SRC, source)
        im = Image.open(path)
        if not opaque:
            if target == "onemoney.png":
                im = onemoney_crop(im)
            im = trim(unwhite(im))
        else:
            im = trim(im.convert("RGBA"))
        height = 240 if opaque else 200
        scale = height / im.height
        size = (max(1, round(im.width * scale)), height)
        im = im.resize(size, Image.LANCZOS)
        im.save(os.path.join(OUT, target))
        print(f"{target:14} {size[0]}x{size[1]}")


if __name__ == "__main__":
    main()
