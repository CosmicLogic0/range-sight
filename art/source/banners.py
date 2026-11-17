"""Range Sight page art: header.png, donate.png, features/ranges.png.
Theme: night navy background, iso ground grid, glowing dashed range outlines in the 6 range colors.
Run: python3 banners.py <art dir>"""
import math, sys, os
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ART = sys.argv[1]
S = 2  # supersample
BG = (17, 20, 38)
GRID = (30, 35, 62)
WHITE = (240, 244, 255)
MUTED = (160, 170, 205)
COLS = {
    'beacon': (85, 255, 255), 'conduit': (60, 140, 255), 'spawner': (255, 80, 80),
    'raid': (255, 153, 51), 'sculk': (30, 217, 200), 'spawn': (255, 225, 77)}
FB = '/usr/share/fonts/truetype/google-fonts/Poppins-Bold.ttf'
FR = '/usr/share/fonts/opentype/inter/Inter-Regular.otf'
FS = '/usr/share/fonts/opentype/inter/Inter-SemiBold.otf'
if not os.path.exists(FS):
    FS = FB


def font(p, size):
    return ImageFont.truetype(p, int(size * S))


def iso(cx, cy, x, z, k=0.5):
    return (cx + (x - z) * 0.866, cy + (x + z) * k)


def background(w, h, cx, cy, rings):
    im = Image.new('RGBA', (w, h), BG + (255,))
    # soft vignette glow behind the rings
    glow = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse([cx - 420 * S, cy - 200 * S, cx + 420 * S, cy + 200 * S], fill=(40, 60, 120, 110))
    im = Image.alpha_composite(im, glow.filter(ImageFilter.GaussianBlur(90 * S)))
    d = ImageDraw.Draw(im)
    n = 30
    st = 40 * S
    for i in range(-n, n + 1):
        d.line([iso(cx, cy, i * st, -n * st), iso(cx, cy, i * st, n * st)], fill=GRID + (255,), width=S)
        d.line([iso(cx, cy, -n * st, i * st), iso(cx, cy, n * st, i * st)], fill=GRID + (255,), width=S)
    lay = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    ld = ImageDraw.Draw(lay)
    for r, col in rings:
        r *= S
        pts = [iso(cx, cy, -r, -r), iso(cx, cy, r, -r), iso(cx, cy, r, r), iso(cx, cy, -r, r)]
        for a, b in zip(pts, pts[1:] + pts[:1]):
            m = 18
            for k in range(0, m, 2):
                p = (a[0] + (b[0] - a[0]) * k / m, a[1] + (b[1] - a[1]) * k / m)
                q = (a[0] + (b[0] - a[0]) * (k + 1) / m, a[1] + (b[1] - a[1]) * (k + 1) / m)
                ld.line([p, q], fill=col + (255,), width=4 * S)
    im = Image.alpha_composite(im, lay.filter(ImageFilter.GaussianBlur(10 * S)))
    im = Image.alpha_composite(im, lay)
    return im


def shadow_text(im, xy, text, f, fill, anchor='la', blur=8):
    lay = Image.new('RGBA', im.size, (0, 0, 0, 0))
    ImageDraw.Draw(lay).text(xy, text, font=f, fill=(0, 0, 0, 200), anchor=anchor)
    im = Image.alpha_composite(im, lay.filter(ImageFilter.GaussianBlur(blur * S)))
    ImageDraw.Draw(im).text(xy, text, font=f, fill=fill, anchor=anchor)
    return im


def panel(im, box, alpha=200):
    lay = Image.new('RGBA', im.size, (0, 0, 0, 0))
    ImageDraw.Draw(lay).rounded_rectangle(box, radius=24 * S, fill=BG + (alpha,), outline=(70, 82, 130, 255), width=2 * S)
    return Image.alpha_composite(im, lay)


def finish(im, path):
    w, h = im.size
    im = im.resize((w // S, h // S), Image.LANCZOS).convert('RGB')
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path, optimize=True)
    print(path, im.size)


# ---------------------------------------------------------------- header
def header():
    W, H = 1600 * S, 500 * S
    im = background(W, H, 1330 * S, 290 * S,
                    [(330, COLS['spawn']), (270, COLS['raid']), (210, COLS['spawner']), (150, COLS['beacon'])])
    # fade the outlines out behind the text (left side)
    fade = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    fd = ImageDraw.Draw(fade)
    for x in range(0, W, S):
        t = (x - 1080 * S) / (300 * S)
        a = 238 if t <= 0 else int(238 * max(0.0, 1 - t))
        fd.line([(x, 0), (x, H)], fill=BG + (a,), width=S)
    im = Image.alpha_composite(im, fade)
    # round the corners
    mask = Image.new('L', (W, H), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, W - 1, H - 1], radius=36 * S, fill=255)
    im.putalpha(mask)
    icon = Image.open(os.path.join(ART, 'icon.png')).convert('RGBA').resize((300 * S, 300 * S), Image.LANCZOS)
    sh = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(sh).rounded_rectangle([90 * S, 108 * S, 390 * S, 408 * S], radius=60 * S, fill=(0, 0, 0, 170))
    im = Image.alpha_composite(im, sh.filter(ImageFilter.GaussianBlur(16 * S)))
    im.alpha_composite(icon, (80 * S, 95 * S))
    im = shadow_text(im, (440 * S, 150 * S), 'Range Sight', font(FB, 104), WHITE, 'ls')
    im = shadow_text(im, (446 * S, 220 * S), 'See the invisible ranges in your world.', font(FS, 38), (200, 230, 255), 'ls')
    d = ImageDraw.Draw(im)
    chips = [('Beacons', 'beacon'), ('Conduits', 'conduit'), ('Spawners', 'spawner'),
             ('Raids', 'raid'), ('Sculk', 'sculk'), ('Spawn chunks', 'spawn')]
    f = font(FS, 27)
    x, y = 446 * S, 268 * S
    for i, (label, key) in enumerate(chips):
        tw = d.textlength(label, font=f)
        bw = tw + 60 * S
        if x + bw > 1540 * S:
            x, y = 446 * S, y + 66 * S
        col = COLS[key]
        d.rounded_rectangle([x, y, x + bw, y + 50 * S], radius=25 * S, fill=(24, 28, 52, 235), outline=col + (255,), width=3 * S)
        d.ellipse([x + 18 * S, y + 17 * S, x + 34 * S, y + 33 * S], fill=col + (255,))
        d.text((x + 44 * S, y + 25 * S), label, font=f, fill=WHITE, anchor='lm')
        x += bw + 14 * S
    d.text((446 * S, 440 * S), 'Forge  ·  NeoForge  ·  Paper plugin  ·  1.12.2 to 26.3', font=font(FS, 26), fill=MUTED, anchor='ls')
    finish(im, os.path.join(ART, 'header.png'))


# ---------------------------------------------------------------- donate
def heart_points(cx, cy, r, n=200):
    pts = []
    for i in range(n):
        t = 2 * math.pi * i / n
        x = 16 * math.sin(t) ** 3
        y = 13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t)
        pts.append((cx + x * r / 16, cy - y * r / 16))
    return pts


def donate():
    W, H = 1600 * S, 420 * S
    im = background(W, H, 230 * S, 230 * S, [(230, COLS['spawner']), (170, COLS['raid']), (110, COLS['spawn'])])
    mask = Image.new('L', (W, H), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, W - 1, H - 1], radius=36 * S, fill=255)
    im.putalpha(mask)
    # glowing outlined heart, drawn like a range outline
    hp = heart_points(230 * S, 200 * S, 120 * S)
    lay = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    ld = ImageDraw.Draw(lay)
    ld.polygon(hp, fill=(255, 80, 110, 255))
    im = Image.alpha_composite(im, lay.filter(ImageFilter.GaussianBlur(26 * S)))
    d = ImageDraw.Draw(im)
    d.polygon(hp, fill=(232, 52, 92, 255))
    inner = heart_points(230 * S, 196 * S, 92 * S)
    d.polygon(inner, fill=(255, 92, 124, 255))
    d.line(hp + [hp[0]], fill=(255, 190, 205, 255), width=5 * S, joint='curve')
    d.ellipse([170 * S, 130 * S, 205 * S, 160 * S], fill=(255, 220, 230, 200))
    im = panel(im, [440 * S, 50 * S, 1550 * S, 370 * S], 215)
    im = shadow_text(im, (490 * S, 128 * S), 'Help keep this mod alive', font(FB, 64), WHITE, 'ls')
    d = ImageDraw.Draw(im)
    f = font(FR, 29)
    lines = ['Range Sight is free. Donations pay for new Minecraft versions,',
             'new features and more invisible ranges to show.']
    for i, ln in enumerate(lines):
        d.text((492 * S, 182 * S + i * 40 * S), ln, font=f, fill=(205, 214, 240), anchor='ls')
    bx, by = 492 * S, 262 * S
    d.rounded_rectangle([bx, by, bx + 420 * S, by + 74 * S], radius=37 * S, fill=(255, 196, 57, 255))
    d.text((bx + 210 * S, by + 37 * S), 'Donate with PayPal', font=font(FB, 32), fill=(20, 40, 90), anchor='mm')
    d.text((bx + 450 * S, by + 37 * S), 'Any amount helps. Thank you!', font=font(FS, 30), fill=WHITE, anchor='lm')
    finish(im, os.path.join(ART, 'donate.png'))


# ---------------------------------------------------------------- range chart
def ranges(plugin=False):
    rows = [('beacon', 'Beacon', 'Effect area: 20 to 50 blocks out (by pyramid levels), up to the sky', 'Numpad 1'),
            ('conduit', 'Conduit', 'Effect sphere: 32 to 96 blocks (by prismarine frame size)', 'Numpad 2'),
            ('spawner', 'Spawner', 'Activation sphere (16) + the box mobs spawn in', 'Numpad 3'),
            ('raid', 'Raid', 'Raid area: 96 blocks around the raid center', 'Numpad 4'),
            ('sculk', 'Sculk', 'Sensor 8 · calibrated 16 · shrieker 8 + Darkness 40', 'Numpad 5'),
            ('spawn', 'Spawn chunks', 'Chunks that stay loaded (up to 1.21.8)', 'Numpad 6')]
    if plugin:
        cmds = ['/range beacon', '/range conduit', '/range spawner', '/range raid', '/range sculk', '/range spawnchunks']
        rows = [(r[0], r[1], r[2], c) for r, c in zip(rows, cmds)]
    W, H = 1600 * S, (150 + 96 * len(rows) + 120) * S
    im = background(W, H, 1350 * S, H // 2, [(260, COLS['conduit']), (190, COLS['sculk']), (120, COLS['beacon'])])
    mask = Image.new('L', (W, H), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, W - 1, H - 1], radius=36 * S, fill=255)
    im.putalpha(mask)
    im = panel(im, [50 * S, 120 * S, 1550 * S, H - 80 * S], 225)
    title = 'Every range, its own color' + ('' if plugin else ' and key')
    im = shadow_text(im, (60 * S, 88 * S), title, font(FB, 54), WHITE, 'ls')
    d = ImageDraw.Draw(im)
    y = 150 * S
    for key, name, desc, k in rows:
        col = COLS[key]
        d.rounded_rectangle([90 * S, y + 18 * S, 150 * S, y + 78 * S], radius=12 * S, outline=col + (255,), width=5 * S)
        d.rounded_rectangle([106 * S, y + 34 * S, 134 * S, y + 62 * S], radius=6 * S, fill=col + (255,))
        d.text((180 * S, y + 40 * S), name, font=font(FB, 34), fill=col, anchor='ls')
        d.text((180 * S, y + 80 * S), desc, font=font(FR, 26), fill=(205, 214, 240), anchor='ls')
        kw = d.textlength(k, font=font(FS, 26))
        d.rounded_rectangle([1500 * S - kw - 40 * S, y + 26 * S, 1500 * S, y + 72 * S], radius=10 * S,
                            fill=(36, 42, 74, 255), outline=(90, 100, 150, 255), width=2 * S)
        d.text((1500 * S - 20 * S - kw / 2, y + 49 * S), k, font=font(FS, 26), fill=WHITE, anchor='mm')
        y += 96 * S
    foot = ('/range all  ·  /range off  ·  /range list. Outlines are shown only to you.' if plugin
            else 'Numpad 0 turns every range on or off. Colors and keys can be changed.')
    d.text((60 * S, H - 30 * S), foot, font=font(FS, 26), fill=MUTED, anchor='ls')
    finish(im, os.path.join(ART, 'features', 'ranges-plugin.png' if plugin else 'ranges.png'))


header()
donate()
ranges()
ranges(True)
