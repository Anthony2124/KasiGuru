"""Slice the six-tier badge boards into one transparent image per family and tier.

Each board in design/assets/badges/source/ is a 3x2 grid on white: Beginner, Learner, Achiever on the
top row, Expert, Master, Legend below. This finds the gutters between the six badges, removes the
white backdrop, and writes app/src/main/res/drawable-nodpi/badge_<family>_<tier 1-6>.webp.

Pixels inside each badge's silhouette stay fully opaque, so the white speech bubbles and book pages
inside the frames are never thinned. Its anti-aliased rim and the sparkles around it are un-mixed
from white ("colour to alpha"), so their edges stay soft. The faint haze painted around the Legend
tier is dropped: on the app's dark ground it reads as smudges, not glow.

Every badge is cropped to the same square, centred on its silhouette, so the frames sit at one scale
from tier to tier and family to family: the Legend wings are wider because the art is wider.

Usage (needs numpy, scipy and Pillow):
    python scripts/generate-badge-art.py            # writes the drawables
    python scripts/generate-badge-art.py --preview  # also writes a contact sheet to build/badge-preview.png
"""
import sys
from pathlib import Path

import numpy as np
from PIL import Image
from scipy import ndimage as ndi

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "design" / "assets" / "badges" / "source"
OUT = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"

# Family id -> board. Mapped by what the art shows (decided with the user on 2026-10-02): the mountain
# trail is the Lesson Pathfinder, and the board saved as "Lesson Pathfinder" (arrows around a checked
# book) is the Review Keeper. Game Adventurer has no board of its own; the app reuses Mode Explorer's.
BOARDS = {
    "word_explorer": "word_explorer.png",
    "lesson_pathfinder": "six_mountain_achievement_badges.png",
    "review_keeper": "lesson_pathfinder.png",
    "consistent_learner": "consistent_learner.png",
    "mode_explorer": "game_mode_explorer.png",
    "precision_player": "precision_player.png",
    "story_reader": "story_reader.png",
    "category_scholar": "category_scholar.png",
    "community_contributor": "community_contributor.png",
    "journey_rank": "journey_rank.png",
}

SIZE = 288          # output px: 72dp at 4x, and above 88dp at xxhdpi
INK = 14            # distance from white (0-255) that counts as part of a badge
MIN_ALPHA = 0.035   # fainter than this is paper noise, not glow
# Distance from white that marks the frame itself rather than its glow, per tier. Silver and the
# Expert wings are pale, so 45; the Legend's crown points behind its frame are painted pale gold and
# would be filled in as opaque cream, so its frame is found at 80.
STRONG = (45, 45, 45, 45, 45, 80)
RIM = 3             # px of anti-aliased edge kept translucent around each frame
SPARKLE_AREA = 2500 # px; marks smaller than this outside the frame may be sparkles...
SPARKLE_PEAK = 100  # ...if their outline is this far from white. The Legend haze peaks near 50.


def gutter(profile, near, span=90):
    """Index of the emptiest column/row within +-span of `near`."""
    lo, hi = max(0, near - span), min(len(profile), near + span)
    return lo + int(np.argmin(profile[lo:hi]))


def cells(ink):
    h, w = ink.shape
    row_split = gutter(ink.sum(axis=1), h // 2)
    for top, bottom in ((0, row_split), (row_split, h)):
        cols = ink[top:bottom].sum(axis=0)
        a, b = gutter(cols, w // 3), gutter(cols, 2 * w // 3)
        for left, right in ((0, a), (a, b), (b, w)):
            yield top, bottom, left, right


def unmix_white(rgb):
    """Colour-to-alpha against pure white: returns straight RGB and alpha in 0-1."""
    alpha = (255.0 - rgb.min(axis=2)) / 255.0
    safe = np.maximum(alpha, 1e-6)[..., None]
    colour = (rgb - 255.0 * (1.0 - alpha[..., None])) / safe
    return np.clip(colour, 0, 255), alpha


def cut(board_path):
    rgb = np.asarray(Image.open(board_path).convert("RGB")).astype(np.float64)
    ink = (255.0 - rgb.min(axis=2)) > INK
    badges = []
    for tier, (top, bottom, left, right) in enumerate(cells(ink)):
        part = rgb[top:bottom, left:right]
        part_ink = ink[top:bottom, left:right]
        # The silhouette comes from the strongly coloured frame only. The Legend glow is pale but
        # touches the frame, so a looser threshold would fill it in as opaque cream.
        strong = (255.0 - part.min(axis=2)) > STRONG[tier]
        labels, n = ndi.label(strong)
        if n == 0:
            raise SystemExit(f"{board_path.name}: an empty cell at {left},{top}")
        sizes = ndi.sum(strong, labels, range(1, n + 1))
        body = labels == (int(np.argmax(sizes)) + 1)
        solid = ndi.binary_erosion(ndi.binary_fill_holes(body), iterations=2)
        # Keep the badge, a soft rim around it, and the distinct sparkles. Drop the faint haze the
        # Legend tier was painted with: it was drawn for white paper, and on the app's dark ground
        # it reads as cream smudges rather than glow. Sparkles are told from haze by strength.
        strength = 255.0 - part.min(axis=2)
        keep = ndi.binary_dilation(solid, iterations=RIM)
        marks, m = ndi.label(part_ink & ~keep)
        for i in range(1, m + 1):
            mark = marks == i
            if mark.sum() < SPARKLE_AREA and strength[mark].max() > SPARKLE_PEAK:
                keep |= ndi.binary_dilation(mark, iterations=1)
                # A sparkle's white heart would unmix to clear and leave a hollow outline.
                solid |= ndi.binary_fill_holes(mark) & ~mark

        colour, alpha = unmix_white(part)
        alpha[alpha < MIN_ALPHA] = 0.0
        alpha[~keep] = 0.0
        alpha[solid] = 1.0
        colour[solid] = part[solid]

        ys, xs = np.nonzero(body)
        centre = ((ys.min() + ys.max()) / 2.0, (xs.min() + xs.max()) / 2.0)
        ys, xs = np.nonzero(alpha > 0)
        reach = max(abs(ys - centre[0]).max(), abs(xs - centre[1]).max())
        badges.append((colour, alpha, centre, reach))
    return badges


def square(colour, alpha, centre, side):
    """A side x side transparent canvas with the badge's silhouette centre at its middle."""
    half = side / 2.0
    canvas = np.zeros((side, side, 4), dtype=np.float64)
    h, w = alpha.shape
    oy, ox = int(round(centre[0] - half)), int(round(centre[1] - half))
    sy0, sx0 = max(0, oy), max(0, ox)
    sy1, sx1 = min(h, oy + side), min(w, ox + side)
    canvas[sy0 - oy:sy1 - oy, sx0 - ox:sx1 - ox, :3] = colour[sy0:sy1, sx0:sx1]
    canvas[sy0 - oy:sy1 - oy, sx0 - ox:sx1 - ox, 3] = alpha[sy0:sy1, sx0:sx1] * 255.0
    image = Image.fromarray(np.clip(canvas, 0, 255).astype(np.uint8), "RGBA")
    # Resample premultiplied, so the transparent white around each badge cannot bleed into its rim.
    return image.convert("RGBa").resize((SIZE, SIZE), Image.LANCZOS).convert("RGBA")


def main():
    missing = [name for name in BOARDS.values() if not (SOURCE / name).exists()]
    if missing:
        raise SystemExit(f"Missing boards in {SOURCE}: {', '.join(missing)}")
    sliced = {family: cut(SOURCE / board) for family, board in BOARDS.items()}
    # One square for every badge, sized to the widest so nothing is clipped: a shared scale.
    side = int(np.ceil(max(reach for badges in sliced.values() for *_, reach in badges) * 2 + 8))
    OUT.mkdir(parents=True, exist_ok=True)
    written = []
    for family, badges in sliced.items():
        for tier, (colour, alpha, centre, _) in enumerate(badges, start=1):
            image = square(colour, alpha, centre, side)
            path = OUT / f"badge_{family}_{tier}.webp"
            image.save(path, "WEBP", quality=90, method=6)
            written.append((family, tier, image))
    total = sum((OUT / f"badge_{f}_{t}.webp").stat().st_size for f, t, _ in written)
    print(f"Wrote {len(written)} badges to {OUT.relative_to(ROOT)} ({total / 1024:.0f} KB), crop {side}px")

    if "--preview" in sys.argv:
        cell = SIZE // 2
        sheet = Image.new("RGBA", (cell * 12, cell * len(BOARDS)), (0, 0, 0, 0))
        for i, (family, tier, image) in enumerate(written):
            row, col = divmod(i, 6)
            for half, ground in ((0, (18, 26, 22, 255)), (6, (244, 241, 232, 255))):
                tile = Image.new("RGBA", (cell, cell), ground)
                tile.alpha_composite(image.resize((cell, cell), Image.LANCZOS))
                sheet.paste(tile, ((col + half) * cell, row * cell))
        preview = ROOT / "build" / "badge-preview.png"
        preview.parent.mkdir(exist_ok=True)
        sheet.save(preview)
        print(f"Preview: {preview.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
