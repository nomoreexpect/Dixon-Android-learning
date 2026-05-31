from PIL import Image, ImageDraw, ImageFont
import os

res = os.path.join(os.path.dirname(__file__), "app", "src", "main", "res")

densities = {
    "mipmap-mdpi":     48,
    "mipmap-hdpi":     72,
    "mipmap-xhdpi":    96,
    "mipmap-xxhdpi":  144,
    "mipmap-xxxhdpi": 192,
}

def create_icon(size, path, is_round=False):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    top_color    = (30,  64, 175)
    bottom_color = (20, 184, 166)
    for y in range(size):
        ratio = y / size
        r = int(top_color[0]*(1-ratio) + bottom_color[0]*ratio)
        g = int(top_color[1]*(1-ratio) + bottom_color[1]*ratio)
        b = int(top_color[2]*(1-ratio) + bottom_color[2]*ratio)
        draw.line([(0, y), (size, y)], fill=(r, g, b, 255))
    if is_round:
        mask = Image.new("L", (size, size), 0)
        mdraw = ImageDraw.Draw(mask)
        r = size // 4
        mdraw.rounded_rectangle([(0,0),(size-1,size-1)], radius=r, fill=255)
        img.putalpha(mask)
    try:
        font = ImageFont.truetype("arial.ttf", size * 3 // 5)
    except:
        font = ImageFont.load_default()
    letter = "A"
    bbox = draw.textbbox((0, 0), letter, font=font)
    lw = bbox[2] - bbox[0]
    lh = bbox[3] - bbox[1]
    x = (size - lw) // 2 - bbox[0]
    y = (size - lh) // 2 - bbox[1]
    draw.text((x, y), letter, font=font, fill=(255, 255, 255, 255))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path, "PNG")
    sz = os.path.getsize(path) // 1024
    print("  OK: " + path + "  (" + str(sz) + " KB)")

for folder, size in densities.items():
    create_icon(size, os.path.join(res, folder, "ic_launcher.png"), is_round=False)
    create_icon(size, os.path.join(res, folder, "ic_launcher_round.png"), is_round=True)

# adaptive icon foreground / background (108x108 for API 26+)
anydpi = os.path.join(res, "mipmap-anydpi-v26")
os.makedirs(anydpi, exist_ok=True)

for name, color in [("ic_launcher_foreground.png", (0,0,0,0)), ("ic_launcher_background.png", (30,64,175,255))]:
    img = Image.new("RGBA", (108, 108), color)
    if "foreground" in name:
        draw = ImageDraw.Draw(img)
        try:
            font = ImageFont.truetype("arial.ttf", 65)
        except:
            font = ImageFont.load_default()
        letter = "A"
        bbox = draw.textbbox((0, 0), letter, font=font)
        lw = bbox[2] - bbox[0]
        lh = bbox[3] - bbox[1]
        x = (108 - lw) // 2 - bbox[0]
        y = (108 - lh) // 2 - bbox[1]
        draw.text((x, y), letter, font=font, fill=(255, 255, 255, 255))
    img.save(os.path.join(anydpi, name), "PNG")
    print("  OK: " + os.path.join(anydpi, name))

print("\nAll launcher icons generated!")
