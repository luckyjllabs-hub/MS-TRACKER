"""Crop MoneyLens emerald branding squircle and generate launcher assets."""
from __future__ import annotations

import colorsys
from pathlib import Path

from PIL import Image, ImageDraw

SRC = Path(
    r"C:\Users\jyoti\.cursor\projects\e-MoneyLens\assets"
    r"\c__Users_jyoti_AppData_Roaming_Cursor_User_workspaceStorage_empty-window"
    r"_images_image-2f601bbb-5bdc-4111-9067-99093cc7e9bd.png"
)
RES = Path(r"E:\MoneyLens\app\src\main\res")

# Soft emerald tune (already-green source; mild saturation/value lift only).
SOFT_TUNE = True
SAT_BOOST = 1.04
VAL_BOOST = 1.02
EMERALD_HUE_MIN = 125.0
EMERALD_HUE_MAX = 185.0
MIN_SAT_TO_TUNE = 0.20


def soft_tune_emerald(im: Image.Image) -> Image.Image:
    """Slightly punch already-emerald pixels; leave whites/grays/accents alone."""
    if not SOFT_TUNE:
        return im
    im = im.convert("RGBA")
    px = im.load()
    w, h = im.size
    tuned = 0
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            hh, ss, vv = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
            hue_deg = hh * 360.0
            if ss < MIN_SAT_TO_TUNE:
                continue
            if not (EMERALD_HUE_MIN <= hue_deg <= EMERALD_HUE_MAX):
                continue
            ss = min(1.0, ss * SAT_BOOST)
            vv = min(1.0, vv * VAL_BOOST)
            nr, ng, nb = colorsys.hsv_to_rgb(hh, ss, vv)
            px[x, y] = (int(nr * 255), int(ng * 255), int(nb * 255), a)
            tuned += 1
    print(f"soft-tuned {tuned} emerald pixels (sat×{SAT_BOOST}, val×{VAL_BOOST})")
    return im


def sample_adaptive_bg(im: Image.Image) -> tuple[int, int, int]:
    """Sample solid emerald from left mid of icon for adaptive background."""
    px = im.load()
    w, h = im.size
    samples: list[tuple[int, int, int]] = []
    for y in range(h // 4, 3 * h // 4, 3):
        for x in range(max(8, w // 20), max(9, w // 6), 2):
            r, g, b, a = px[x, y]
            if a < 200:
                continue
            hh, ss, vv = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
            hue_deg = hh * 360.0
            if EMERALD_HUE_MIN <= hue_deg <= EMERALD_HUE_MAX and ss > 0.35 and vv > 0.2:
                samples.append((r, g, b))
    if not samples:
        return (1, 124, 86)  # fallback from sheet sample #017C56
    rs = sorted(s[0] for s in samples)
    gs = sorted(s[1] for s in samples)
    bs = sorted(s[2] for s in samples)
    mid = len(samples) // 2
    return rs[mid], gs[mid], bs[mid]


def rounded(im: Image.Image, radius_ratio: float = 0.22) -> Image.Image:
    im = im.convert("RGBA")
    w, h = im.size
    r = int(w * radius_ratio)
    mask = Image.new("L", (w, h), 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle((0, 0, w - 1, h - 1), radius=r, fill=255)
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    out.paste(im, (0, 0))
    out.putalpha(mask)
    return out


def find_squircle_bbox(img: Image.Image) -> tuple[int, int, int, int]:
    """Crop only the emerald squircle (exclude wordmark + tagline below)."""
    w, h = img.size
    px = img.load()

    # Find white gap under the icon (wordmark starts after).
    in_icon = False
    gap_start = h
    for y in range(h):
        greens = 0
        for x in range(0, w, 2):
            r, g, b, a = px[x, y]
            if a < 180:
                continue
            hh, ss, vv = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
            if EMERALD_HUE_MIN <= hh * 360.0 <= EMERALD_HUE_MAX and ss > 0.25 and vv > 0.25:
                greens += 1
        if greens > 50:
            in_icon = True
        elif in_icon and greens < 5:
            gap_start = y
            break

    # Include a few px of soft shadow under the squircle, stop before wordmark.
    y_limit = min(h, gap_start + 12)

    pts: list[tuple[int, int]] = []
    for y in range(y_limit):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a > 200 and not (r > 245 and g > 245 and b > 245):
                pts.append((x, y))
    if not pts:
        raise RuntimeError("Could not locate MoneyLens squircle in branding sheet")

    bx0 = min(p[0] for p in pts)
    by0 = min(p[1] for p in pts)
    bx1 = max(p[0] for p in pts)
    by1 = max(p[1] for p in pts)
    pad = 2
    bx0 = max(0, bx0 - pad)
    by0 = max(0, by0 - pad)
    bx1 = min(w - 1, bx1 + pad)
    by1 = min(h - 1, by1 + pad)
    print("squircle bbox", bx0, by0, bx1, by1, "gap_start", gap_start)
    return bx0, by0, bx1, by1


def main() -> None:
    img = Image.open(SRC).convert("RGBA")
    w, h = img.size
    print("size", w, h)

    bx0, by0, bx1, by1 = find_squircle_bbox(img)
    side = max(bx1 - bx0, by1 - by0)
    cx = (bx0 + bx1) // 2
    cy = (by0 + by1) // 2
    half = side // 2
    x0 = max(0, cx - half)
    y0 = max(0, cy - half)
    x1 = min(w, x0 + side)
    y1 = min(h, y0 + side)
    # If clipped at bottom into wordmark zone, shift up.
    if y1 > by1 + 8:
        shift = y1 - (by1 + 8)
        y0 = max(0, y0 - shift)
        y1 = y0 + (x1 - x0)

    icon = img.crop((x0, y0, x1, y1)).convert("RGBA")
    s = min(icon.size)
    icon = icon.crop((0, 0, s, s))
    icon = soft_tune_emerald(icon)
    print("icon", icon.size)

    bg_rgb = sample_adaptive_bg(icon)
    print(f"adaptive bg sampled #{bg_rgb[0]:02X}{bg_rgb[1]:02X}{bg_rgb[2]:02X}")

    master = RES / "drawable" / "moneylens_icon_master.png"
    master.parent.mkdir(parents=True, exist_ok=True)
    icon.save(master)

    sizes = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }

    for folder, size in sizes.items():
        out_dir = RES / folder
        out_dir.mkdir(parents=True, exist_ok=True)
        resized = icon.resize((size, size), Image.Resampling.LANCZOS)
        for name in ("ic_launcher.png", "ic_launcher_round.png"):
            path = out_dir / name
            if "round" in name:
                rounded(resized).save(path, optimize=True)
            else:
                resized.save(path, optimize=True)
        for old in ("ic_launcher.webp", "ic_launcher_round.webp"):
            p = out_dir / old
            if p.exists():
                p.unlink()
                print("removed", p)

    fg_size = 432
    fg = Image.new("RGBA", (fg_size, fg_size), (0, 0, 0, 0))
    fitted = icon.resize((fg_size, fg_size), Image.Resampling.LANCZOS)
    fg.paste(fitted, (0, 0), fitted)
    fg_path = RES / "drawable" / "ic_launcher_foreground.png"
    fg.save(fg_path, optimize=True)

    bg = Image.new("RGBA", (fg_size, fg_size), (*bg_rgb, 255))
    bg_path = RES / "drawable" / "ic_launcher_background.png"
    bg.save(bg_path, optimize=True)

    # Prefer PNG adaptive layers; remove stale vector XML if present.
    for stale in (
        RES / "drawable" / "ic_launcher_background.xml",
        RES / "drawable" / "ic_launcher_foreground.xml",
    ):
        if stale.exists():
            stale.unlink()
            print("removed", stale)

    print("done", master, fg_path, bg_path)


if __name__ == "__main__":
    main()
