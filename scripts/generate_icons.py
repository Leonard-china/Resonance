"""Generate Android launcher and Windows application icons from branding masters."""

from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
BRANDING = ROOT / "assets" / "branding"
ANDROID_RES = ROOT / "composeApp" / "src" / "androidMain" / "res"
DESKTOP_RES = ROOT / "composeApp" / "src" / "desktopMain" / "resources"


def resized(image: Image.Image, size: int) -> Image.Image:
    return image.resize((size, size), Image.Resampling.LANCZOS)


def save_android_icons(master: Image.Image, foreground: Image.Image) -> None:
    densities = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }
    for directory, size in densities.items():
        destination = ANDROID_RES / directory
        destination.mkdir(parents=True, exist_ok=True)
        square = resized(master, size).convert("RGBA")
        square.save(destination / "ic_launcher.png", optimize=True)

        circle_mask = Image.new("L", (size, size), 0)
        ImageDraw.Draw(circle_mask).ellipse((0, 0, size - 1, size - 1), fill=255)
        round_icon = square.copy()
        round_icon.putalpha(circle_mask)
        round_icon.save(destination / "ic_launcher_round.png", optimize=True)

    adaptive_size = 432
    alpha_box = foreground.getchannel("A").getbbox()
    if alpha_box is None:
        raise ValueError("Foreground master has no visible pixels")
    symbol = foreground.crop(alpha_box)
    # Keep the visible mark inside the adaptive-icon safe zone while avoiding
    # the undersized appearance common with double-inset launcher assets.
    max_symbol = round(adaptive_size * 0.68)
    scale = min(max_symbol / symbol.width, max_symbol / symbol.height)
    symbol = symbol.resize(
        (max(1, round(symbol.width * scale)), max(1, round(symbol.height * scale))),
        Image.Resampling.LANCZOS,
    )
    adaptive = Image.new("RGBA", (adaptive_size, adaptive_size), (0, 0, 0, 0))
    adaptive.alpha_composite(
        symbol,
        ((adaptive_size - symbol.width) // 2, (adaptive_size - symbol.height) // 2),
    )
    destination = ANDROID_RES / "drawable-nodpi"
    destination.mkdir(parents=True, exist_ok=True)
    adaptive.save(destination / "ic_launcher_foreground.png", optimize=True)


def save_windows_icons(master: Image.Image) -> None:
    DESKTOP_RES.mkdir(parents=True, exist_ok=True)
    size = 1024
    icon = resized(master, size).convert("RGBA")
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        (0, 0, size - 1, size - 1),
        radius=round(size * 0.22),
        fill=255,
    )
    icon.putalpha(mask)
    resized(icon, 512).save(DESKTOP_RES / "resonance-icon.png", optimize=True)
    icon.save(
        DESKTOP_RES / "resonance.ico",
        format="ICO",
        sizes=[(16, 16), (20, 20), (24, 24), (32, 32), (40, 40), (48, 48), (64, 64), (128, 128), (256, 256)],
    )


def main() -> None:
    master = Image.open(BRANDING / "resonance-icon-master.png").convert("RGB")
    foreground = Image.open(BRANDING / "resonance-icon-foreground.png").convert("RGBA")
    if master.width != master.height or foreground.width != foreground.height:
        raise ValueError("Icon masters must be square")
    save_android_icons(master, foreground)
    save_windows_icons(master)


if __name__ == "__main__":
    main()
