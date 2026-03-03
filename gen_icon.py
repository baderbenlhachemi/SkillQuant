"""
SkillQuant Icon — Final
Concept: Bold glowing diamond (electric teal) with white upward arrow inside.
Diamond = value/opportunity. Arrow = growth. Clean, modern, one-idea icon.
Background: near-black with subtle teal center glow.
Rendered 8x supersampled + LANCZOS for perfect crispness at all sizes.
"""
from PIL import Image, ImageDraw, ImageFilter
import math, os

OUT = r"C:\Users\bader\AndroidStudioProjects\SkillQuant\androidApp\src\main\res"
SIZES = {
    "mipmap-mdpi":    48,
    "mipmap-hdpi":    72,
    "mipmap-xhdpi":   96,
    "mipmap-xxhdpi":  144,
    "mipmap-xxxhdpi": 192,
}

def draw_icon(size):
    R = 6                    # supersampling factor (6x = great quality, fast)
    S = size * R
    D = 192.0                # design space units

    def sc(v): return v * S / D

    # ── Background: near-black, fast row-by-row ──────────────────────────
    img = Image.new("RGBA", (S, S), (13, 13, 18, 255))
    d   = ImageDraw.Draw(img)

    # Teal radial center glow using concentric ellipses (fast, no pixel loop)
    cx_bg, cy_bg = S // 2, S // 2
    for r, alpha in [
        (int(sc(90)), 8),
        (int(sc(68)), 14),
        (int(sc(48)), 22),
        (int(sc(30)), 16),
    ]:
        glow_img = Image.new("RGBA", (S, S), (0,0,0,0))
        ImageDraw.Draw(glow_img).ellipse(
            [cx_bg-r, cy_bg-r, cx_bg+r, cy_bg+r],
            fill=(0, 200, 230, alpha)
        )
        blurred = glow_img.filter(ImageFilter.GaussianBlur(radius=r//3))
        img = Image.alpha_composite(img, blurred)

    d = ImageDraw.Draw(img)

    # ── Diamond vertices ─────────────────────────────────────────────────
    cx  = sc(96)
    cy  = sc(98)
    hs  = sc(56)              # half-size of diamond

    top   = (cx,      cy - hs)
    right = (cx + hs, cy)
    bot   = (cx,      cy + hs)
    left  = (cx - hs, cy)
    diamond = [top, right, bot, left]

    # ── Diamond glow (blurred soft halo around the diamond edges) ────────
    glow_layer = Image.new("RGBA", (S, S), (0,0,0,0))
    gd = ImageDraw.Draw(glow_layer)
    for extra, alpha in [(sc(14), 12), (sc(9), 25), (sc(5), 45), (sc(2), 70)]:
        e = hs + extra
        gd.polygon(
            [(cx, cy-e),(cx+e, cy),(cx, cy+e),(cx-e, cy)],
            outline=(0, 229, 255, alpha),
            fill=(0, 229, 255, alpha // 10)
        )
    glow_blur = glow_layer.filter(ImageFilter.GaussianBlur(radius=sc(5)))
    img = Image.alpha_composite(img, glow_blur)
    d = ImageDraw.Draw(img)

    # ── Diamond stroke (crisp teal outline) ──────────────────────────────
    lw_glow = max(4, int(sc(10)))
    lw_main = max(2, int(sc(6)))
    for i in range(4):
        p1, p2 = diamond[i], diamond[(i+1) % 4]
        d.line([p1, p2], fill=(0, 229, 255, 55), width=lw_glow)
        d.line([p1, p2], fill=(0, 229, 255, 255), width=lw_main)

    # ── Rising sparkline inside diamond ──────────────────────────────────
    # 4 points going bottom-left → top-right with one realistic dip
    # Contained within ~65% of the diamond interior
    margin = hs * 0.62
    spark_pts = [
        (cx - margin * 0.90,  cy + margin * 0.55),   # p0 — start low-left
        (cx - margin * 0.28,  cy + margin * 0.05),   # p1 — rise
        (cx + margin * 0.18,  cy + margin * 0.28),   # p2 — small dip (realism)
        (cx + margin * 0.88,  cy - margin * 0.70),   # p3 — peak top-right
    ]

    lw_spark = max(2, int(sc(5.5)))

    # Filled area under sparkline (faint teal gradient effect via polygon)
    # Close path back along bottom of diamond interior
    fill_pts = list(spark_pts) + [
        (cx + margin * 0.88,  cy + margin * 0.55),
        (cx - margin * 0.90,  cy + margin * 0.55),
    ]
    fill_layer = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(fill_layer).polygon(fill_pts, fill=(0, 200, 230, 38))
    img = Image.alpha_composite(img, fill_layer)
    d = ImageDraw.Draw(img)

    # Glow pass on line
    d.line(spark_pts, fill=(0, 229, 255, 55), width=lw_spark + int(sc(6)), joint="curve")
    # Main white line
    d.line(spark_pts, fill=(255, 255, 255, 235), width=lw_spark, joint="curve")

    # Small white dots at each data point
    for i, pt in enumerate(spark_pts[:-1]):
        r_dot = max(2, int(sc(4.5)))
        d.ellipse([pt[0]-r_dot, pt[1]-r_dot, pt[0]+r_dot, pt[1]+r_dot],
                  fill=(255, 255, 255, 180))

    # ── Glowing peak dot at top-right of sparkline ────────────────────────
    pk = spark_pts[-1]
    for r2, col, a in [
        (int(sc(14)), (0, 229, 255), 25),
        (int(sc(9)),  (0, 229, 255), 55),
        (int(sc(5.5)),(80, 240, 255), 140),
        (int(sc(3)),  (255, 255, 255), 255),
    ]:
        d.ellipse([pk[0]-r2, pk[1]-r2, pk[0]+r2, pk[1]+r2], fill=col+(a,))

    # ── Fine corner caps on diamond ───────────────────────────────────────
    cap_r = lw_main // 2 + 1
    for pt in diamond:
        d.ellipse([pt[0]-cap_r, pt[1]-cap_r, pt[0]+cap_r, pt[1]+cap_r],
                  fill=(0, 229, 255, 255))

    # ── Downsample ────────────────────────────────────────────────────────
    return img.resize((size, size), Image.LANCZOS)

def make_round(img):
    sz = img.size[0]
    mask = Image.new("L", (sz, sz), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, sz-1, sz-1], fill=255)
    out = img.copy()
    out.putalpha(mask)
    return out

for folder, px in SIZES.items():
    path = os.path.join(OUT, folder)
    os.makedirs(path, exist_ok=True)
    icon = draw_icon(px)
    icon.save(os.path.join(path, "ic_launcher.png"))
    make_round(icon).save(os.path.join(path, "ic_launcher_round.png"))
    print(f"  OK {folder}  {px}x{px}")

preview = draw_icon(512)
preview.save(r"C:\Users\bader\AndroidStudioProjects\SkillQuant\icon_preview.png")
print("Preview saved.")
print("Done!")
