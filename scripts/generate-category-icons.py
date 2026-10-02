"""Cut the dictionary's category icons out of their white backgrounds.

Each image in design/assets/categories/source/ is one glossy icon on white. This removes the white,
trims the icon, centres it on a square with a small even margin, and writes
app/src/main/res/drawable-nodpi/category_<slug>.webp.

As in generate-badge-art.py, the icon's own silhouette stays fully opaque (so the white of a cloud or
a chat bubble is never thinned), and only a thin rim around it is un-mixed from white so the edge
stays anti-aliased.

Usage (needs numpy, scipy and Pillow):
    python scripts/generate-category-icons.py            # writes the drawables
    python scripts/generate-category-icons.py --preview  # also writes build/category-preview.png
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image
from scipy import ndimage as ndi

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "design" / "assets" / "categories" / "source"
OUT = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"

# Drawable slug -> source image. The slugs are what CategoryMetaData.kt references.
ICONS = {
    "greetings": "waving_hand_with_chat_bubble.png",
    "food": "chibi_rice_bowl_with_spoon.png",
    "animals": "golden_paw_print_sticker.png",
    "health": "heart_medical_cross_icon.png",
    "numbers": "blue_twin_bell_alarm_clock.png",
    "weather": "sun_behind_puffy_cloud.png",
    "emotions": "laughing_emoji_with_heart.png",
    "house": "cartoon_cottage_icon.png",
    "nature": "cartoon_sprout_icon.png",
    "family": "family_group_avatar.png",
    "colors": "geometric_shape_trio.png",
    "occupations": "hammer_and_pencil_icon.png",
    "general": "blue_open_book_icon.png",  # any category the registry does not know
}

SIZE = 240          # output px: 60dp at 4x, 80dp at xxhdpi
MARGIN = 0.04       # share of the square left clear on every side
INK = 14            # distance from white (0-255) that counts as part of the icon
STRONG = 45         # distance from white that marks the icon's outline and fills
MIN_PART = 150      # px; outlined parts smaller than this are paper noise
RIM = 3             # px of anti-aliased edge kept translucent around the silhouette
MIN_ALPHA = 0.035


def unmix_white(rgb):
    """Colour-to-alpha against pure white: returns straight RGB and alpha in 0-1."""
    alpha = (255.0 - rgb.min(axis=2)) / 255.0
    safe = np.maximum(alpha, 1e-6)[..., None]
    colour = (rgb - 255.0 * (1.0 - alpha[..., None])) / safe
    return np.clip(colour, 0, 255), alpha


def cut(path):
    rgb = np.asarray(Image.open(path).convert("RGB")).astype(np.float64)
    strength = 255.0 - rgb.min(axis=2)
    strong = strength > STRONG
    labels, n = ndi.label(strong)
    sizes = ndi.sum(strong, labels, range(1, n + 1))
    # Every outlined part, not only the largest: the greeting's chat bubble and motion marks, the
    # family's three people and the three shapes are separate pieces of one icon.
    parts = np.isin(labels, [i + 1 for i, s in enumerate(sizes) if s >= MIN_PART])
    solid = ndi.binary_erosion(ndi.binary_fill_holes(parts), iterations=2)
    keep = ndi.binary_dilation(solid, iterations=RIM) & (strength > INK)

    colour, alpha = unmix_white(rgb)
    alpha[alpha < MIN_ALPHA] = 0.0
    alpha[~keep] = 0.0
    alpha[solid] = 1.0
    colour[solid] = rgb[solid]

    ys, xs = np.nonzero(alpha > 0)
    top, bottom, left, right = ys.min(), ys.max() + 1, xs.min(), xs.max() + 1
    side = int(np.ceil(max(bottom - top, right - left) / (1 - 2 * MARGIN)))
    canvas = np.zeros((side, side, 4), dtype=np.float64)
    oy, ox = (side - (bottom - top)) // 2, (side - (right - left)) // 2
    canvas[oy:oy + bottom - top, ox:ox + right - left, :3] = colour[top:bottom, left:right]
    canvas[oy:oy + bottom - top, ox:ox + right - left, 3] = alpha[top:bottom, left:right] * 255.0
    image = Image.fromarray(np.clip(canvas, 0, 255).astype(np.uint8), "RGBA")
    # Resample premultiplied, so the transparent white cannot bleed into the rim.
    return image.convert("RGBa").resize((SIZE, SIZE), Image.LANCZOS).convert("RGBA")


def main():
    missing = [name for name in ICONS.values() if not (SOURCE / name).exists()]
    if missing:
        raise SystemExit(f"Missing icons in {SOURCE}: {', '.join(missing)}")
    OUT.mkdir(parents=True, exist_ok=True)
    written = []
    for slug, name in ICONS.items():
        image = cut(SOURCE / name)
        path = OUT / f"category_{slug}.webp"
        image.save(path, "WEBP", quality=90, method=6)
        written.append((slug, image, path.stat().st_size))
    total = sum(size for *_, size in written)
    print(f"Wrote {len(written)} icons to {OUT.relative_to(ROOT)} ({total / 1024:.0f} KB)")

    if "--preview" in sys.argv:
        cell = 160
        sheet = Image.new("RGBA", (cell * len(written), cell * 2), (0, 0, 0, 0))
        for i, (_, image, _) in enumerate(written):
            for row, ground in enumerate(((18, 26, 22, 255), (244, 241, 232, 255))):
                tile = Image.new("RGBA", (cell, cell), ground)
                tile.alpha_composite(image.resize((cell, cell), Image.LANCZOS))
                sheet.paste(tile, (i * cell, row * cell))
        preview = ROOT / "build" / "category-preview.png"
        preview.parent.mkdir(exist_ok=True)
        sheet.save(preview)
        print(f"Preview: {preview.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
