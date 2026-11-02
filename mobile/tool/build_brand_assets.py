"""Builds the ATG app icon, launch screens and in-app mark from the supplied logo.

Usage (Pillow required: python -m pip install pillow):
    python tool/build_brand_assets.py            # uses assets/brand/atg-bull-logo.png
    python tool/build_brand_assets.py other.png  # or another square logo

The source art is a black canvas with a gold bull over green chart bars, a gold
"ACCURATE TRADING GROUP" wordmark and a green tagline. The mark keeps its colours;
over black it is identical to the source, so the alpha is recovered per pixel
(alpha = max channel, colour unpremultiplied) and the art works on cream too.
"""

from __future__ import annotations

import json
import os
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEFAULT_SRC = os.path.join(ROOT, "assets", "brand", "atg-bull-logo.png")

# Bands measured on the 1254x1254 source.
BULL = (300, 232, 1002, 774)  # bull + green base bar
LOCKUP = (40, 225, 1214, 980)  # mark + wordmark + tagline

ICON_BG = (10, 10, 10, 255)


def cutout(src: Image.Image, box: tuple[int, int, int, int]) -> Image.Image:
    """Returns the crop with black turned into transparency, colours preserved."""
    crop = src.crop(box).convert("RGBA")
    out = Image.new("RGBA", crop.size)
    src_px = crop.load()
    out_px = out.load()
    for y in range(crop.height):
        for x in range(crop.width):
            r, g, b, _ = src_px[x, y]
            alpha = max(r, g, b)
            if alpha == 0:
                out_px[x, y] = (0, 0, 0, 0)
                continue
            scale = 255.0 / alpha
            out_px[x, y] = (
                min(255, round(r * scale)),
                min(255, round(g * scale)),
                min(255, round(b * scale)),
                alpha,
            )
    return out


def fit(image: Image.Image, width: int) -> Image.Image:
    height = max(1, round(image.height * width / image.width))
    return image.resize((width, height), Image.LANCZOS)


def icon_tile(mark: Image.Image, side: int, mark_ratio: float, radius_ratio: float = 0.0) -> Image.Image:
    tile = Image.new("RGBA", (side, side), ICON_BG)
    inner = fit(mark, max(1, round(side * mark_ratio)))
    tile.alpha_composite(inner, ((side - inner.width) // 2, (side - inner.height) // 2))
    if radius_ratio:
        from PIL import ImageDraw

        mask = Image.new("L", (side, side), 0)
        ImageDraw.Draw(mask).rounded_rectangle(
            (0, 0, side - 1, side - 1), radius=round(side * radius_ratio), fill=255
        )
        tile.putalpha(mask)
    return tile


def save(image: Image.Image, path: str) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    image.save(path, optimize=True)


def main() -> None:
    src_path = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_SRC
    src = Image.open(src_path).convert("RGB")
    mark = cutout(src, BULL)
    lockup = cutout(src, LOCKUP)

    # Flutter asset used by AtgLogo and the animated splash.
    canvas = Image.new("RGBA", (640, 640), (0, 0, 0, 0))
    inner = fit(mark, 620)
    canvas.alpha_composite(inner, ((640 - inner.width) // 2, (640 - inner.height) // 2))
    save(canvas, os.path.join(ROOT, "assets", "images", "atg_mark.png"))

    # Android launcher icons (legacy squares) and adaptive foregrounds.
    densities = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}
    res = os.path.join(ROOT, "android", "app", "src", "main", "res")
    for name, scale in densities.items():
        side = round(48 * scale)
        save(
            icon_tile(mark, side, 0.74).convert("RGB"),
            os.path.join(res, f"mipmap-{name}", "ic_launcher.png"),
        )
        fg = round(108 * scale)
        foreground = Image.new("RGBA", (fg, fg), (0, 0, 0, 0))
        inner = fit(mark, round(fg * 0.62))
        foreground.alpha_composite(inner, ((fg - inner.width) // 2, (fg - inner.height) // 2))
        save(foreground, os.path.join(res, f"mipmap-{name}", "ic_launcher_foreground.png"))

        splash = round(300 * scale)
        save(fit(lockup, splash), os.path.join(res, f"drawable-{name}", "splash_logo.png"))

    # iOS app icons, straight from the icon appiconset manifest.
    appicon_dir = os.path.join(ROOT, "ios", "Runner", "Assets.xcassets", "AppIcon.appiconset")
    manifest = json.load(open(os.path.join(appicon_dir, "Contents.json"), encoding="utf-8"))
    for entry in manifest["images"]:
        filename = entry.get("filename")
        if not filename:
            continue
        points = float(entry["size"].split("x")[0])
        scale = int(entry["scale"].rstrip("x"))
        px = round(points * scale)
        save(icon_tile(mark, px, 0.74).convert("RGB"), os.path.join(appicon_dir, filename))

    # iOS launch image (universal 1x/2x/3x), centred on black by the storyboard.
    launch_dir = os.path.join(ROOT, "ios", "Runner", "Assets.xcassets", "LaunchImage.imageset")
    for scale, suffix in ((1, ""), (2, "@2x"), (3, "@3x")):
        save(fit(lockup, 300 * scale), os.path.join(launch_dir, f"LaunchImage{suffix}.png"))

    print("brand assets written")


if __name__ == "__main__":
    main()
