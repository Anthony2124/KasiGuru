"""Export the authored app icon. Run from the repository root with Pillow installed.

    python scripts/generate-launcher-icons.py

The source is preserved in design/assets/app-icon.png. Adaptive layers are 108 dp;
the artwork occupies the central 72 dp visible area. Edge extrusion fills the
outer motion margin without enlarging the face or introducing transparent seams.
Android supplies the launcher mask; no rounded corners are baked into the assets.
"""

from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
DENSITIES = {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}


def extrude_edges(image: Image.Image, margin: int) -> Image.Image:
    width, height = image.size
    result = Image.new("RGB", (width + 2 * margin, height + 2 * margin))
    result.paste(image, (margin, margin))
    result.paste(image.crop((0, 0, width, 1)).resize((width, margin)), (margin, 0))
    result.paste(image.crop((0, height - 1, width, height)).resize((width, margin)), (margin, height + margin))
    result.paste(image.crop((0, 0, 1, height)).resize((margin, height)), (0, margin))
    result.paste(image.crop((width - 1, 0, width, height)).resize((margin, height)), (width + margin, margin))
    for x, y in ((0, 0), (width - 1, 0), (0, height - 1), (width - 1, height - 1)):
        left = 0 if x == 0 else width + margin
        top = 0 if y == 0 else height + margin
        result.paste(image.getpixel((x, y)), (left, top, left + margin, top + margin))
    return result


def main() -> None:
    with Image.open(ROOT / "design/assets/app-icon.png") as source:
        if source.width != source.height:
            raise ValueError("Launcher artwork must be square.")
        # Launcher icons need an opaque backing, including the source's translucent green edges.
        background = Image.new("RGBA", source.size, "#067000")
        image = Image.alpha_composite(background, source.convert("RGBA")).convert("RGB")

    for density, scale in DENSITIES.items():
        folder = RES / f"mipmap-{density}"
        folder.mkdir(parents=True, exist_ok=True)
        legacy_size = round(48 * scale)
        image.resize((legacy_size, legacy_size), Image.Resampling.LANCZOS).save(folder / "ic_launcher.png", optimize=True)
        visible_size = round(72 * scale)
        visible = image.resize((visible_size, visible_size), Image.Resampling.LANCZOS)
        extrude_edges(visible, round(18 * scale)).save(folder / "ic_launcher_foreground.png", optimize=True)
        print(f"Exported {density} launcher icons")


if __name__ == "__main__":
    main()
