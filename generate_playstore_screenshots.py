#!/usr/bin/env python3
"""Generate polished Play Store screenshots for phone, 7-inch tablet, and 10-inch tablet."""

import os
import zipfile
from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageEnhance

ASSETS_DIR = "/home/dhyeb/.cursor/projects/home-dhyeb-AndroidStudioProjects-ScaleFinder/assets"
OUTPUT_DIR = "/home/dhyeb/AndroidStudioProjects/ScaleFinder/playstore_screenshots"

FONT_BOLD = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT_REGULAR = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"

STATUS_BAR_HEIGHT = 24
STATUS_BAR_BG = (14, 22, 41)

SCREENSHOTS = [
    {
        "file": "screenshot_20260223_160133_scale_finder_720-892190ad-72a3-47bc-afcc-5ddd01ff5443.png",
        "title": "Scale Finder",
        "subtitle": "Find the perfect scale for your chord progression",
    },
    {
        "file": "screenshot_20260223_160142_scale_finder_720-6ad252d1-90b9-4e48-b5fb-1142fe4111db.png",
        "title": "Chromatic Tuner",
        "subtitle": "Tune your guitar with precision",
    },
    {
        "file": "screenshot_20260223_160146_scale_finder_720-4f7d71c6-c533-46e0-806e-c0f01bbe8e74.png",
        "title": "Scale Explorer",
        "subtitle": "Explore scales, notes & diatonic chords",
    },
    {
        "file": "screenshot_20260223_160149_scale_finder_720-0376da7b-ba29-426f-8018-2bdad32af639.png",
        "title": "Music Quiz",
        "subtitle": "Test your music theory knowledge",
    },
    {
        "file": "screenshot_20260223_160154_scale_finder_720-76311a04-5fe6-4aa1-a0cc-fe2998882814.png",
        "title": "Audio to Tablature",
        "subtitle": "Transcribe audio into guitar tabs with AI",
    },
    {
        "file": "screenshot_20260223_160159_scale_finder_720-137ed6f1-884d-4de6-9262-374d36f82c82.png",
        "title": "Settings",
        "subtitle": "Customize theme, fretboard & tuning",
    },
    {
        "file": "screenshot_20260223_140953_scale_finder_720-0fb8ca83-a0d7-4f73-9223-5a646943ebeb.png",
        "title": "Quick Presets",
        "subtitle": "Jump-start with popular progressions",
    },
]

DEVICE_CONFIGS = {
    "phone": {"width": 1920, "height": 1080, "folder": "phone_1920x1080"},
    "tablet_7": {"width": 1920, "height": 1080, "folder": "tablet_7inch_1920x1080"},
    "tablet_10": {"width": 2560, "height": 1440, "folder": "tablet_10inch_2560x1440"},
}

GRADIENT_PALETTES = [
    ((45, 20, 80), (90, 40, 140)),
    ((20, 40, 90), (50, 80, 160)),
    ((60, 20, 100), (120, 50, 160)),
    ((25, 50, 80), (60, 100, 150)),
    ((50, 15, 90), (100, 40, 150)),
    ((30, 30, 70), (70, 60, 130)),
    ((40, 25, 85), (80, 50, 145)),
]


def remove_status_bar(img):
    """Paint over the Android status bar with the app background color."""
    result = img.copy()
    draw = ImageDraw.Draw(result)
    draw.rectangle([(0, 0), (result.width - 1, STATUS_BAR_HEIGHT - 1)], fill=STATUS_BAR_BG)
    return result


def create_gradient(width, height, color_top, color_bottom):
    """Create a vertical gradient image."""
    img = Image.new("RGBA", (width, height))
    pixels = img.load()
    for y in range(height):
        ratio = y / height
        smoothed = ratio * ratio * (3 - 2 * ratio)
        r = int(color_top[0] + (color_bottom[0] - color_top[0]) * smoothed)
        g = int(color_top[1] + (color_bottom[1] - color_top[1]) * smoothed)
        b = int(color_top[2] + (color_bottom[2] - color_top[2]) * smoothed)
        for x in range(width):
            pixels[x, y] = (r, g, b, 255)
    return img


def add_rounded_corners(img, radius):
    """Add rounded corners to an image."""
    mask = Image.new("L", img.size, 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle([(0, 0), (img.size[0] - 1, img.size[1] - 1)], radius=radius, fill=255)
    result = img.copy()
    result.putalpha(mask)
    return result


def add_shadow(img, offset=(8, 8), blur_radius=20, shadow_color=(0, 0, 0, 100)):
    """Add a drop shadow behind an image."""
    shadow_size = (
        img.size[0] + abs(offset[0]) + blur_radius * 4,
        img.size[1] + abs(offset[1]) + blur_radius * 4,
    )
    shadow = Image.new("RGBA", shadow_size, (0, 0, 0, 0))
    shadow_layer = Image.new("RGBA", img.size, shadow_color)

    if img.mode == "RGBA":
        shadow_layer.putalpha(img.split()[3])

    paste_x = blur_radius * 2 + max(offset[0], 0)
    paste_y = blur_radius * 2 + max(offset[1], 0)
    shadow.paste(shadow_layer, (paste_x, paste_y))
    shadow = shadow.filter(ImageFilter.GaussianBlur(blur_radius))

    img_x = blur_radius * 2 + max(-offset[0], 0)
    img_y = blur_radius * 2 + max(-offset[1], 0)
    shadow.paste(img, (img_x, img_y), img if img.mode == "RGBA" else None)
    return shadow, (img_x, img_y)


def add_device_frame(screenshot, corner_radius, border_width=3, border_color=(180, 180, 200, 200)):
    """Add a subtle device-like border and rounded corners."""
    rounded = add_rounded_corners(screenshot, corner_radius)
    framed = Image.new("RGBA", screenshot.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(framed)
    draw.rounded_rectangle(
        [(0, 0), (screenshot.size[0] - 1, screenshot.size[1] - 1)],
        radius=corner_radius,
        outline=border_color,
        width=border_width,
    )
    result = Image.alpha_composite(
        Image.new("RGBA", screenshot.size, (0, 0, 0, 0)),
        rounded,
    )
    result = Image.alpha_composite(result, framed)
    return result


def add_subtle_glow(canvas, center_x, center_y, radius, color=(100, 60, 180, 30)):
    """Add a subtle radial glow effect."""
    glow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(glow)
    for i in range(radius, 0, -2):
        alpha = int(color[3] * (i / radius) ** 0.5)
        c = (color[0], color[1], color[2], alpha)
        draw.ellipse(
            [center_x - i, center_y - i, center_x + i, center_y + i],
            fill=c,
        )
    return Image.alpha_composite(canvas, glow)


def sharpen_upscaled(img, factor=1.3):
    """Apply sharpening to counteract upscale blur. Uses UnsharpMask for natural results."""
    sharpened = img.filter(ImageFilter.UnsharpMask(radius=1.5, percent=80, threshold=2))
    enhancer = ImageEnhance.Sharpness(sharpened)
    return enhancer.enhance(factor)


def generate_screenshot(screenshot_info, device_config, palette_idx, output_path):
    """Generate a single polished Play Store screenshot."""
    w, h = device_config["width"], device_config["height"]
    palette = GRADIENT_PALETTES[palette_idx % len(GRADIENT_PALETTES)]

    canvas = create_gradient(w, h, palette[0], palette[1])

    canvas = add_subtle_glow(
        canvas, w // 3, h // 2, min(w, h) // 2,
        (palette[1][0] + 40, palette[1][1] + 20, palette[1][2] + 40, 20),
    )
    canvas = add_subtle_glow(
        canvas, w * 2 // 3, h // 3, min(w, h) // 3,
        (palette[0][0] + 30, palette[0][1] + 30, palette[0][2] + 60, 15),
    )

    title_font_size = int(h * 0.065)
    subtitle_font_size = int(h * 0.035)
    title_font = ImageFont.truetype(FONT_BOLD, title_font_size)
    subtitle_font = ImageFont.truetype(FONT_REGULAR, subtitle_font_size)

    text_area_top = int(h * 0.06)

    draw = ImageDraw.Draw(canvas)
    title = screenshot_info["title"]
    subtitle = screenshot_info["subtitle"]

    title_bbox = draw.textbbox((0, 0), title, font=title_font)
    title_w = title_bbox[2] - title_bbox[0]
    title_x = (w - title_w) // 2
    title_y = text_area_top

    draw.text((title_x + 2, title_y + 2), title, fill=(0, 0, 0, 80), font=title_font)
    draw.text((title_x, title_y), title, fill=(255, 255, 255, 255), font=title_font)

    subtitle_bbox = draw.textbbox((0, 0), subtitle, font=subtitle_font)
    subtitle_w = subtitle_bbox[2] - subtitle_bbox[0]
    subtitle_x = (w - subtitle_w) // 2
    subtitle_y = title_y + title_font_size + int(h * 0.015)

    draw.text((subtitle_x, subtitle_y), subtitle, fill=(200, 200, 220, 200), font=subtitle_font)

    screenshot_top = subtitle_y + subtitle_font_size + int(h * 0.04)
    available_height = h - screenshot_top - int(h * 0.05)
    available_width = int(w * 0.85)

    src_img = Image.open(os.path.join(ASSETS_DIR, screenshot_info["file"])).convert("RGBA")
    src_img = remove_status_bar(src_img)

    src_w, src_h = src_img.size
    scale = min(available_width / src_w, available_height / src_h)
    new_w = int(src_w * scale)
    new_h = int(src_h * scale)

    resized = src_img.resize((new_w, new_h), Image.LANCZOS)
    resized = sharpen_upscaled(resized)

    corner_radius = int(min(new_w, new_h) * 0.025)
    border_width = max(2, int(scale * 1.5))
    framed = add_device_frame(resized, corner_radius, border_width, (140, 120, 180, 180))

    shadow_offset = int(h * 0.008)
    shadow_blur = int(h * 0.02)
    shadowed, (img_offset_x, img_offset_y) = add_shadow(
        framed, (shadow_offset, shadow_offset), shadow_blur, (0, 0, 0, 120)
    )

    paste_x = (w - shadowed.size[0]) // 2
    paste_y = screenshot_top + (available_height - new_h) // 2 - img_offset_y

    canvas.paste(shadowed, (paste_x, paste_y), shadowed)

    accent_y = h - int(h * 0.015)
    accent_width = int(w * 0.12)
    accent_x = (w - accent_width) // 2
    accent_color = (palette[1][0] + 60, palette[1][1] + 30, palette[1][2] + 60, 120)
    draw = ImageDraw.Draw(canvas)
    draw.rounded_rectangle(
        [(accent_x, accent_y), (accent_x + accent_width, accent_y + int(h * 0.005))],
        radius=3,
        fill=accent_color,
    )

    final = canvas.convert("RGB")
    final.save(output_path, "PNG", optimize=False)
    print(f"  -> {output_path}")


def main():
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    for device_name, config in DEVICE_CONFIGS.items():
        device_dir = os.path.join(OUTPUT_DIR, config["folder"])
        os.makedirs(device_dir, exist_ok=True)
        print(f"\nGenerating {device_name} screenshots ({config['width']}x{config['height']})...")

        for idx, ss_info in enumerate(SCREENSHOTS):
            filename = f"{idx + 1:02d}_{ss_info['title'].lower().replace(' ', '_')}.png"
            output_path = os.path.join(device_dir, filename)
            generate_screenshot(ss_info, config, idx, output_path)

    zip_path = os.path.join(OUTPUT_DIR, "playstore_screenshots.zip")
    print(f"\nCreating zip: {zip_path}")
    with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_STORED) as zf:
        for device_name, config in DEVICE_CONFIGS.items():
            device_dir = os.path.join(OUTPUT_DIR, config["folder"])
            for filename in sorted(os.listdir(device_dir)):
                if filename.endswith(".png"):
                    filepath = os.path.join(device_dir, filename)
                    arcname = os.path.join(config["folder"], filename)
                    zf.write(filepath, arcname)
                    print(f"  + {arcname}")

    print(f"\nDone! Zip file: {zip_path}")
    zip_size = os.path.getsize(zip_path) / (1024 * 1024)
    print(f"Zip size: {zip_size:.1f} MB")


if __name__ == "__main__":
    main()
