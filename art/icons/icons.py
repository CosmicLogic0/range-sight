import math, random, sys
from PIL import Image, ImageDraw, ImageFilter, ImageFont
OUT=sys.argv[1]; S=4; W=400*S; C=W/2
COLS=[(85,255,255),(60,140,255),(255,80,80),(255,153,51),(30,217,200),(255,225,77)]
def base(bg=(20,24,36)):
    im=Image.new("RGBA",(W,W),(0,0,0,0)); d=ImageDraw.Draw(im)
    d.rounded_rectangle([0,0,W-1,W-1],radius=72*S,fill=bg+(255,)); return im
def mask(im):
    m=Image.new("L",(W,W),0); ImageDraw.Draw(m).rounded_rectangle([0,0,W-1,W-1],radius=72*S,fill=255)
    out=Image.new("RGBA",(W,W),(0,0,0,0)); out.paste(im,(0,0),m); return out
def glow(im,layer,blur=14):
    g=layer.filter(ImageFilter.GaussianBlur(blur*S)); im=Image.alpha_composite(im,g); return Image.alpha_composite(im,layer)
def save(im,name): mask(im).resize((400,400),Image.LANCZOS).save(f"{OUT}/{name}.png")

# A: isometric beacon with range diamonds on the ground
def iso(x,y,z,cx=C,cy=C+40*S,s=1.0):
    return (cx+(x-z)*math.cos(math.radians(30))*s, cy+(x+z)*math.sin(math.radians(30))*s - y*s)
im=base((18,22,40)); lay=Image.new("RGBA",(W,W),(0,0,0,0)); d=ImageDraw.Draw(lay)
gd=ImageDraw.Draw(im)
for i in range(-6,7):
    gd.line([iso(i*28*S,0,-170*S),iso(i*28*S,0,170*S)],fill=(30,36,60,255),width=S*2)
    gd.line([iso(-170*S,0,i*28*S),iso(170*S,0,i*28*S)],fill=(30,36,60,255),width=S*2)
for r,col in [(148,COLS[2]),(108,COLS[5]),(70,COLS[0])]:
    pts=[iso(-r*S,0,-r*S),iso(r*S,0,-r*S),iso(r*S,0,r*S),iso(-r*S,0,r*S)]
    for a,b in zip(pts,pts[1:]+pts[:1]):
        n=14
        for k in range(n):
            if k%2: continue
            p=(a[0]+(b[0]-a[0])*k/n,a[1]+(b[1]-a[1])*k/n); q=(a[0]+(b[0]-a[0])*(k+1)/n,a[1]+(b[1]-a[1])*(k+1)/n)
            d.line([p,q],fill=col+(255,),width=8*S)
# beam
d.polygon([iso(-8*S,40*S,0),iso(8*S,40*S,0),iso(8*S,330*S,0),iso(-8*S,330*S,0)],fill=(200,250,255,170))
im=glow(im,lay); d=ImageDraw.Draw(im)
h=40*S
top=[iso(-h,2*h,-h),iso(h,2*h,-h),iso(h,2*h,h),iso(-h,2*h,h)]
left=[iso(-h,0,h),iso(h,0,h),iso(h,2*h,h),iso(-h,2*h,h)]
right=[iso(h,0,-h),iso(h,0,h),iso(h,2*h,h),iso(h,2*h,-h)]
d.polygon(top,fill=(150,240,250,255),outline=(230,255,255,255),width=3*S)
d.polygon(left,fill=(60,180,205,255),outline=(230,255,255,255),width=3*S)
d.polygon(right,fill=(35,130,165,255),outline=(230,255,255,255),width=3*S)
core=[iso(-h/2,h*1.5,h),iso(h/2,h*1.5,h),iso(h/2,h*0.5,h),iso(-h/2,h*0.5,h)]
d.polygon(core,fill=(240,255,255,255))
save(im,"A-isometric-beacon")

# B: eye whose iris is made of range rings
im=base((16,18,30)); lay=Image.new("RGBA",(W,W),(0,0,0,0)); d=ImageDraw.Draw(lay)
ew,eh=150*S,92*S
eye=[]
for i in range(0,181):
    t=math.radians(i); eye.append((C-ew*math.cos(t), C-eh*math.sin(t)))
for i in range(0,181):
    t=math.radians(i); eye.append((C+ew*math.cos(t), C+eh*math.sin(t)))
d.polygon(eye,fill=(232,240,255,255))
for i,(r,col) in enumerate([(78,COLS[2]),(66,COLS[3]),(54,COLS[5]),(42,COLS[4]),(30,COLS[1])]):
    d.ellipse([C-r*S,C-r*S,C+r*S,C+r*S],fill=col+(255,))
d.rectangle([C-14*S,C-14*S,C+14*S,C+14*S],fill=(20,24,40,255))
d.rectangle([C-6*S,C-6*S,C+6*S,C+6*S],fill=(150,250,255,255))
d.line(eye+[eye[0]],fill=(120,230,255,255),width=9*S)
im=glow(im,lay,10)
d=ImageDraw.Draw(im)
for k in range(4):  # dashed square frame
    r=158*S; pts=[(C-r,C-r),(C+r,C-r),(C+r,C+r),(C-r,C+r)]
a,b=None,None
pts=[(C-158*S,C-158*S),(C+158*S,C-158*S),(C+158*S,C+158*S),(C-158*S,C+158*S)]
for a,b in zip(pts,pts[1:]+pts[:1]):
    n=16
    for k in range(0,n,2):
        p=(a[0]+(b[0]-a[0])*k/n,a[1]+(b[1]-a[1])*k/n); q=(a[0]+(b[0]-a[0])*(k+1)/n,a[1]+(b[1]-a[1])*(k+1)/n)
        d.line([p,q],fill=(85,255,255,150),width=6*S)
save(im,"B-range-eye")

# C: pixel art, Minecraft style
G=40; P=W//G
im=base((0,0,0)); d=ImageDraw.Draw(im); random.seed(7)
for gx in range(G):
    for gy in range(G):
        v=random.randint(-7,7); col=(46+v,50+v,62+v)
        d.rectangle([gx*P,gy*P,gx*P+P-1,gy*P+P-1],fill=col+(255,))
def px(gx,gy,col): d.rectangle([gx*P,gy*P,gx*P+P-1,gy*P+P-1],fill=col+(255,))
for r,col in [(17,COLS[2]),(13,COLS[5]),(9,COLS[0])]:
    for i in range(-r,r):
        if (i+r)%2==0:
            for (x,y) in [(20+i,20-r),(20+i,20+r-1),(20-r,20+i),(20+r-1,20+i)]: px(x,y,col)
for x in range(17,23):
    for y in range(17,23):
        edge = x in (17,22) or y in (17,22)
        px(x,y,(215,255,255) if edge else (70,200,220))
for x in range(19,21):
    for y in range(19,21): px(x,y,(255,255,255))
for y in range(3,17): px(19,y,(190,245,255)); px(20,y,(230,255,255))
save(im,"C-pixel-art")

# D: radar
im=base((10,22,26)); d=ImageDraw.Draw(im)
R=168*S
d.ellipse([C-R,C-R,C+R,C+R],fill=(14,40,44,255),outline=(60,200,190,255),width=5*S)
for f in (0.33,0.66):
    r=R*f; d.ellipse([C-r,C-r,C+r,C+r],outline=(40,120,115,255),width=3*S)
d.line([(C-R,C),(C+R,C)],fill=(40,120,115,255),width=3*S); d.line([(C,C-R),(C,C+R)],fill=(40,120,115,255),width=3*S)
sweep=Image.new("RGBA",(W,W),(0,0,0,0)); sd=ImageDraw.Draw(sweep)
for k in range(60):
    a0=-90-k; sd.pieslice([C-R,C-R,C+R,C+R],a0-1,a0,fill=(60,255,220,int(140*(1-k/60))))
im=Image.alpha_composite(im,sweep)
lay=Image.new("RGBA",(W,W),(0,0,0,0)); ld=ImageDraw.Draw(lay)
blips=[(-0.45,-0.35,COLS[0],18),(0.5,0.2,COLS[2],15),(-0.2,0.55,COLS[3],14),(0.3,-0.55,COLS[5],13),(0.62,-0.18,COLS[4],12),(-0.6,0.15,COLS[1],13)]
for bx,by,col,s in blips:
    x,y=C+bx*R,C+by*R; s*=S
    ld.rectangle([x-s,y-s,x+s,y+s],fill=col+(255,))
    ld.ellipse([x-s*2.6,y-s*2.6,x+s*2.6,y+s*2.6],outline=col+(200,),width=3*S)
im=glow(im,lay,8)
save(im,"D-radar")
