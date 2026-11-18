"""Range Sight gallery graphics (1920x1080): title cards + explainers for the mod and the plugin.
Drawn graphics only (no gameplay); real screenshots come from CI. Run: python3 gallery.py <art dir>"""
import os, sys, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import banners as B
from PIL import Image, ImageDraw, ImageFilter

S = B.S
ART = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
OUT = os.path.join(ART, 'gallery')
W, H = 1920 * S, 1080 * S
C = B.COLS
WHITE, MUTED = B.WHITE, B.MUTED
f = B.font


def canvas(cx, cy, rings, fade=None):
    im = B.background(W, H, cx * S, cy * S, rings)
    if fade:
        x0, x1 = fade
        lay = Image.new('RGBA', (W, H), (0, 0, 0, 0))
        d = ImageDraw.Draw(lay)
        for x in range(0, W, S):
            t = (x - x0 * S) / ((x1 - x0) * S)
            a = 236 if t <= 0 else int(236 * max(0.0, 1 - t))
            d.line([(x, 0), (x, H)], fill=B.BG + (a,), width=S)
        im = Image.alpha_composite(im, lay)
    return im


def title(im, text, sub=None, y=120):
    im = B.shadow_text(im, (110 * S, y * S), text, f(B.FB, 76), WHITE, 'ls')
    if sub:
        ImageDraw.Draw(im).text((114 * S, (y + 56) * S), sub, font=f(B.FS, 34), fill=(195, 210, 240), anchor='ls')
    return im


def tag(im):
    ImageDraw.Draw(im).text((W - 60 * S, H - 40 * S), 'Range Sight  ·  graphic', font=f(B.FS, 24), fill=(120, 130, 170), anchor='rs')
    return im


def chips(d, x, y, items, size=30, maxx=1800):
    fo = f(B.FS, size)
    x0 = x
    for label, key in items:
        tw = d.textlength(label, font=fo)
        bw = tw + 66 * S
        if x + bw > maxx * S:
            x, y = x0, y + 72 * S
        col = C[key]
        d.rounded_rectangle([x, y, x + bw, y + 56 * S], radius=28 * S, fill=(24, 28, 52, 235), outline=col + (255,), width=3 * S)
        d.ellipse([x + 20 * S, y + 19 * S, x + 38 * S, y + 37 * S], fill=col + (255,))
        d.text((x + 48 * S, y + 28 * S), label, font=fo, fill=WHITE, anchor='lm')
        x += bw + 16 * S
    return y


def finish(im, name):
    os.makedirs(OUT, exist_ok=True)
    mask = Image.new('L', im.size, 0)
    ImageDraw.Draw(mask).rectangle([0, 0, W, H], fill=255)
    B.finish(im, os.path.join(OUT, name))


# ------------------------------------------------------------------ title cards
def title_card(plugin=False):
    im = canvas(1700, 700, [(380, C['spawn']), (300, C['raid']), (220, C['spawner']), (140, C['beacon'])], fade=(1480, 1760))
    icon = Image.open(os.path.join(ART, 'icon.png')).convert('RGBA').resize((420 * S, 420 * S), Image.LANCZOS)
    sh = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(sh).rounded_rectangle([130 * S, 330 * S, 550 * S, 750 * S], radius=84 * S, fill=(0, 0, 0, 180))
    im = Image.alpha_composite(im, sh.filter(ImageFilter.GaussianBlur(22 * S)))
    im.alpha_composite(icon, (120 * S, 310 * S))
    name = 'Range Sight Plugin' if plugin else 'Range Sight'
    im = B.shadow_text(im, (630 * S, 420 * S), name, f(B.FB, 128 if not plugin else 112), WHITE, 'ls')
    line = 'Type /range. See the invisible ranges.' if plugin else 'See the invisible ranges in your world.'
    im = B.shadow_text(im, (636 * S, 500 * S), line, f(B.FS, 46), (200, 230, 255), 'ls')
    d = ImageDraw.Draw(im)
    chips(d, 636 * S, 560 * S, [('Beacons', 'beacon'), ('Conduits', 'conduit'), ('Spawners', 'spawner'),
                                ('Raids', 'raid'), ('Sculk', 'sculk'), ('Spawn chunks', 'spawn')], 32, 1860)
    sub = ('Paper 1.12.2 to 26.3  ·  one jar  ·  requires LogicLib Plugin' if plugin
           else 'Forge + NeoForge  ·  Minecraft 1.12.2 to 26.3  ·  client-side')
    d.text((636 * S, 780 * S), sub, font=f(B.FS, 34), fill=MUTED, anchor='ls')
    finish(im, 'p01-title.png' if plugin else 'm01-title.png')


# ------------------------------------------------------------------ range chart (keys or commands)
def chart(plugin=False):
    rows = [('beacon', 'Beacon', 'Effect area: 20 to 50 blocks out (by pyramid levels), up to the sky', 'Numpad 1', '/range beacon'),
            ('conduit', 'Conduit', 'Effect sphere: 32 to 96 blocks (by prismarine frame size)', 'Numpad 2', '/range conduit'),
            ('spawner', 'Spawner', 'Activation sphere (16) + the box its mobs spawn in', 'Numpad 3', '/range spawner'),
            ('raid', 'Raid', 'Raid area: 96 blocks around the raid center', 'Numpad 4', '/range raid'),
            ('sculk', 'Sculk', 'Sensor 8 · calibrated 16 · shrieker 8 + Darkness 40', 'Numpad 5', '/range sculk'),
            ('spawn', 'Spawn chunks', 'Chunks that stay loaded (up to 1.21.8)', 'Numpad 6', '/range spawnchunks')]
    im = canvas(1650, 560, [(300, C['conduit']), (220, C['sculk']), (140, C['beacon'])])
    im = title(im, 'Every range, its own color' + ('' if plugin else ' and key'),
               'Turn on only the ranges you need.' if not plugin else 'Each player picks their own ranges.')
    im = B.panel(im, [90 * S, 220 * S, 1830 * S, 940 * S], 228)
    d = ImageDraw.Draw(im)
    y = 250 * S
    for key, name, desc, k, cmd in rows:
        col = C[key]
        d.rounded_rectangle([140 * S, y + 22 * S, 210 * S, y + 92 * S], radius=14 * S, outline=col + (255,), width=6 * S)
        d.rounded_rectangle([159 * S, y + 41 * S, 191 * S, y + 73 * S], radius=7 * S, fill=col + (255,))
        d.text((250 * S, y + 50 * S), name, font=f(B.FB, 40), fill=col, anchor='ls')
        d.text((250 * S, y + 96 * S), desc, font=f(B.FR, 30), fill=(205, 214, 240), anchor='ls')
        lab = cmd if plugin else k
        fo = f(B.FS, 30)
        kw = d.textlength(lab, font=fo)
        d.rounded_rectangle([1780 * S - kw - 48 * S, y + 32 * S, 1780 * S, y + 86 * S], radius=12 * S,
                            fill=(36, 42, 74, 255), outline=(90, 100, 150, 255), width=2 * S)
        d.text((1780 * S - 24 * S - kw / 2, y + 59 * S), lab, font=fo, fill=WHITE, anchor='mm')
        y += 113 * S
    foot = ('/range all  ·  /range off  ·  /range list. Outlines are shown only to you.' if plugin
            else 'Numpad 0 turns every range on or off. Change keys in Options → Controls; colors in config/rangesight.json.')
    d.text((110 * S, 1000 * S), foot, font=f(B.FS, 30), fill=MUTED, anchor='ls')
    tag(im)
    finish(im, 'p02-commands.png' if plugin else 'm02-ranges.png')


# ------------------------------------------------------------------ explainers (mod)
def iso_block(d, cx, cy, s, top, left, right, edge=(70, 76, 92, 255)):
    h = s * 0.5
    t = [(cx, cy - h), (cx + s * 0.866, cy), (cx, cy + h), (cx - s * 0.866, cy)]
    l = [(cx - s * 0.866, cy), (cx, cy + h), (cx, cy + h + s), (cx - s * 0.866, cy + s)]
    r = [(cx + s * 0.866, cy), (cx, cy + h), (cx, cy + h + s), (cx + s * 0.866, cy + s)]
    d.polygon(l, fill=left, outline=edge)
    d.polygon(r, fill=right, outline=edge)
    d.polygon(t, fill=top, outline=edge)


def beacon_levels():
    im = canvas(960, 760, [])
    im = title(im, 'Beacon range grows with the pyramid', 'Range Sight counts the levels for you and draws the real area.')
    d = ImageDraw.Draw(im)
    xs = [270, 690, 1110, 1560]
    for i, levels in enumerate([1, 2, 3, 4]):
        cx = xs[i] * S
        base = 690 * S
        s = 26 * S
        # pyramid, top level first drawn last
        for lv in range(levels, 0, -1):
            n = lv
            y0 = base - (levels - lv) * s
            cells = [(a, b) for a in range(-n, n + 1) for b in range(-n, n + 1)]
            cells.sort(key=lambda c: c[0] + c[1])
            for a, b in cells:
                px = cx + (a - b) * s * 0.866
                py = y0 + (a + b) * s * 0.5
                iso_block(d, px, py, s, (222, 226, 232, 255), (170, 176, 186, 255), (134, 140, 152, 255))
        top_y = base - levels * s
        iso_block(d, cx, top_y, s, (120, 255, 255, 255), (70, 170, 190, 255), (50, 130, 150, 255))
        d.line([(cx, top_y - 20 * S), (cx, 240 * S)], fill=(150, 255, 255, 200), width=5 * S)
        rng = levels * 10 + 10
        d.text((cx, 840 * S), f'{levels} level' + ('s' if levels > 1 else ''), font=f(B.FS, 36), fill=MUTED, anchor='ms')
        d.text((cx, 920 * S), f'{rng} blocks', font=f(B.FB, 64), fill=C['beacon'], anchor='ms')
    d.text((110 * S, 1010 * S), 'Range = 10 × levels + 10 blocks out from the beacon, from the ground up to the sky.', font=f(B.FS, 30), fill=MUTED, anchor='ls')
    tag(im)
    finish(im, 'm03-beacon-levels.png')


def conduit_frames():
    im = canvas(1500, 600, [])
    im = title(im, 'Conduit range comes from its frame', 'More prismarine around the conduit = a bigger sphere.')
    d = ImageDraw.Draw(im)
    rows = [('16 – 20 frame blocks', 32), ('21 – 27', 48), ('28 – 34', 64), ('35 – 41', 80), ('42 (full frame)', 96)]
    y = 290 * S
    for label, r in rows:
        d.text((140 * S, y + 46 * S), label, font=f(B.FS, 36), fill=WHITE, anchor='ls')
        bw = r / 96 * 980 * S
        d.rounded_rectangle([620 * S, y, 620 * S + bw, y + 64 * S], radius=14 * S, fill=C['conduit'] + (230,))
        d.text((640 * S + bw, y + 46 * S), f'{r} blocks', font=f(B.FB, 40), fill=C['conduit'], anchor='ls')
        y += 120 * S
    d.text((110 * S, 1010 * S), 'Prismarine, prismarine bricks, dark prismarine and sea lanterns all count. Under 16 = no power.', font=f(B.FS, 30), fill=MUTED, anchor='ls')
    tag(im)
    finish(im, 'm04-conduit-frames.png')


def sculk_rings():
    im = canvas(960, 600, [])
    im = title(im, 'Sculk: who can hear you', 'Sensors, calibrated sensors and shriekers each get their own ring.')
    d = ImageDraw.Draw(im)
    dark = tuple(int(v * 0.6) for v in C['sculk'])
    groups = [(300, 'Sculk sensor', [(130, C['sculk'], 'hears 8 blocks')]),
              (820, 'Calibrated sensor', [(230, C['sculk'], 'hears 16 blocks')]),
              (1480, 'Sculk shrieker', [(130, C['sculk'], 'hears 8'), (330, dark, 'Darkness reaches 40')])]
    for cx, name, rings in groups:
        cx *= S
        cy = 620 * S
        for rr, col, label in rings:
            rr *= S
            for k in range(0, 360, 6):
                d.arc([cx - rr, cy - rr * 0.5, cx + rr, cy + rr * 0.5], k, k + 3.5, fill=col + (255,), width=5 * S)
            d.text((cx, cy - rr * 0.5 - 14 * S), label, font=f(B.FS, 30), fill=col, anchor='ms')
        d.rounded_rectangle([cx - 26 * S, cy - 26 * S, cx + 26 * S, cy + 26 * S], radius=7 * S, fill=(20, 60, 70, 255), outline=C['sculk'] + (255,), width=4 * S)
        d.text((cx, 930 * S), name, font=f(B.FB, 42), fill=WHITE, anchor='ms')
    d.text((110 * S, 1010 * S), 'Drawn as spheres in the game. The darker ring is how far a shrieker\'s Darkness reaches.', font=f(B.FS, 30), fill=MUTED, anchor='ls')
    tag(im)
    finish(im, 'm05-sculk-rings.png')


def versions(plugin=False):
    im = canvas(1500, 640, [(320, C['spawn']), (220, C['raid'])])
    if plugin:
        im = title(im, 'One jar for every Paper version', 'Drop it in plugins/ with LogicLib Plugin. Players install nothing.')
    else:
        im = title(im, 'Every version, Forge and NeoForge', 'Pick the file for your version on the Files tab.')
    d = ImageDraw.Draw(im)
    vers = ['1.12.2', '1.16.5', '1.18.2', '1.19.2', '1.20.1', '1.21.1', '1.21.11', '26.1', '26.2', '26.3']
    cols = [('Paper', vers)] if plugin else [('Forge', vers), ('NeoForge', ['1.20.1', '1.21.1', '1.21.11', '26.1', '26.2', '26.3'])]
    x0 = 140 * S
    for ci, (loader, vs) in enumerate(cols):
        y = (280 + ci * 330) * S
        d.text((x0, y), loader, font=f(B.FB, 48), fill=WHITE, anchor='ls')
        x = x0
        y += 40 * S
        for i, v in enumerate(vs):
            bw = 152 * S
            if i == 5 and not plugin:
                pass
            col = [C['beacon'], C['conduit'], C['sculk'], C['spawn'], C['raid'], C['spawner']][i % 6]
            d.rounded_rectangle([x, y, x + bw, y + 96 * S], radius=18 * S, fill=(24, 28, 52, 240), outline=col + (255,), width=3 * S)
            d.text((x + bw / 2, y + 48 * S), v, font=f(B.FB, 36), fill=WHITE, anchor='mm')
            x += bw + 14 * S
    if plugin:
        d.rounded_rectangle([140 * S, 560 * S, 1780 * S, 760 * S], radius=24 * S, fill=(24, 28, 52, 235), outline=(255, 196, 57, 255), width=3 * S)
        d.text((180 * S, 640 * S), 'Requires LogicLib Plugin', font=f(B.FB, 44), fill=(255, 210, 90), anchor='ls')
        d.text((180 * S, 706 * S), 'Shared code for CosmicLogic plugins. The CurseForge app adds it for you.', font=f(B.FS, 32), fill=(205, 214, 240), anchor='ls')
    d.text((110 * S, 1010 * S), 'Every file is started on a real server and tested automatically before release.', font=f(B.FS, 30), fill=MUTED, anchor='ls')
    tag(im)
    finish(im, 'p04-versions.png' if plugin else 'm06-versions.png')


# ------------------------------------------------------------------ explainer (plugin)
def person(d, x, y, col):
    d.ellipse([x - 34 * S, y - 150 * S, x + 34 * S, y - 82 * S], fill=col)
    d.rounded_rectangle([x - 50 * S, y - 76 * S, x + 50 * S, y + 60 * S], radius=20 * S, fill=col)


def only_you():
    im = canvas(960, 700, [])
    im = title(im, 'Only you see your outlines', 'Each player turns on their own ranges. Nobody else\'s screen gets busy.')
    d = ImageDraw.Draw(im)
    # left: player who typed /range sees particles; right: another player sees nothing
    for cx, sees, label in [(560, True, 'You typed /range spawner'), (1360, False, 'Another player')]:
        cx *= S
        cy = 720 * S
        d.rounded_rectangle([cx - 340 * S, 300 * S, cx + 340 * S, 880 * S], radius=30 * S, fill=(22, 26, 48, 235), outline=(70, 82, 130, 255), width=2 * S)
        d.rounded_rectangle([cx - 30 * S, cy - 40 * S, cx + 30 * S, cy + 20 * S], radius=6 * S, fill=(40, 46, 70, 255), outline=(120, 130, 170, 255), width=3 * S)
        if sees:
            r = 230 * S
            for k in range(0, 360, 8):
                a = math.radians(k)
                for rr, yy in [(r, 0.42), (r * 0.7, 0.3)]:
                    px, py = cx + rr * math.cos(a), cy - 10 * S + rr * yy * math.sin(a)
                    d.ellipse([px - 6 * S, py - 6 * S, px + 6 * S, py + 6 * S], fill=C['spawner'] + (255,))
        person(d, cx - 230 * S, 820 * S, (90, 170, 255) if sees else (150, 150, 170))
        d.text((cx, 360 * S), label, font=f(B.FB, 38), fill=WHITE, anchor='ms')
        d.text((cx, 850 * S), 'sees the range' if sees else 'sees nothing extra', font=f(B.FS, 32),
               fill=C['spawner'] if sees else MUTED, anchor='ms')
    d.text((110 * S, 1010 * S), 'Particle outlines are sent only to the player who asked. Permission: rangesight.use (everyone by default).', font=f(B.FS, 30), fill=MUTED, anchor='ls')
    tag(im)
    finish(im, 'p03-only-you.png')


if __name__ == '__main__':
    title_card()
    chart()
    beacon_levels()
    conduit_frames()
    sculk_rings()
    versions()
    title_card(True)
    chart(True)
    only_you()
    versions(True)
