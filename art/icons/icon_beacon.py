import math, sys
from PIL import Image, ImageDraw, ImageFilter
OUT=sys.argv[1]; S=4; W=400*S; C=W/2
COLS=[(85,255,255),(60,140,255),(255,80,80),(255,153,51),(30,217,200),(255,225,77)]
CY=C+44*S
def iso(x,y,z): return (C+(x-z)*0.8660, CY+(x+z)*0.5 - y)
def poly(d,pts,fill,outline=None,w=0): d.polygon(pts,fill=fill,outline=outline,width=w)
def faces(x0,y0,z0,s):
    x1,y1,z1=x0+s,y0+s,z0+s
    top=[iso(x0,y1,z0),iso(x1,y1,z0),iso(x1,y1,z1),iso(x0,y1,z1)]
    left=[iso(x0,y0,z1),iso(x1,y0,z1),iso(x1,y1,z1),iso(x0,y1,z1)]   # +z face (front-left)
    right=[iso(x1,y0,z0),iso(x1,y0,z1),iso(x1,y1,z1),iso(x1,y1,z0)]  # +x face (front-right)
    return top,left,right
def cube(d,x,y,z,s,ct,cl,cr,edge,w=2):
    t,l,r=faces(x,y,z,s); poly(d,t,ct,edge,w*S); poly(d,l,cl,edge,w*S); poly(d,r,cr,edge,w*S)
def inset(pts,f):
    cx=sum(p[0] for p in pts)/4; cy=sum(p[1] for p in pts)/4
    return [(cx+(p[0]-cx)*f, cy+(p[1]-cy)*f) for p in pts]

im=Image.new("RGBA",(W,W),(0,0,0,0)); d=ImageDraw.Draw(im)
d.rounded_rectangle([0,0,W-1,W-1],radius=72*S,fill=(17,20,38,255))
# ground grid
for i in range(-8,9):
    d.line([iso(i*24*S,0,-200*S),iso(i*24*S,0,200*S)],fill=(28,33,58,255),width=2*S)
    d.line([iso(-200*S,0,i*24*S),iso(200*S,0,i*24*S)],fill=(28,33,58,255),width=2*S)
# range rings (dashed squares on the ground), with glow
lay=Image.new("RGBA",(W,W),(0,0,0,0)); ld=ImageDraw.Draw(lay)
for r,col in [(196,COLS[2]),(160,COLS[5]),(126,COLS[0])]:
    pts=[iso(-r*S,0,-r*S),iso(r*S,0,-r*S),iso(r*S,0,r*S),iso(-r*S,0,r*S)]
    for a,b in zip(pts,pts[1:]+pts[:1]):
        n=14
        for k in range(0,n,2):
            p=(a[0]+(b[0]-a[0])*k/n,a[1]+(b[1]-a[1])*k/n); q=(a[0]+(b[0]-a[0])*(k+1)/n,a[1]+(b[1]-a[1])*(k+1)/n)
            ld.line([p,q],fill=col+(255,),width=7*S)
im=Image.alpha_composite(im,lay.filter(ImageFilter.GaussianBlur(12*S))); im=Image.alpha_composite(im,lay)
d=ImageDraw.Draw(im)
# iron block pyramid layer (3x3), drawn back to front
B=64*S
blocks=[(i,k) for i in (-1,0,1) for k in (-1,0,1)]
blocks.sort(key=lambda b:b[0]+b[1])
for i,k in blocks:
    x=i*B-B/2; z=k*B-B/2
    cube(d,x,0,z,B,(218,222,228,255),(168,174,184,255),(132,138,150,255),(90,96,110,255),2)
    t,l,r=faces(x,0,z,B)
    for f in (l,r):  # iron block texture: inner panel
        poly(d,inset(f,0.62),None,(110,116,128,255),1*S)
# beacon on top
BB=B*1.45

bx,by,bz=-BB/2,B,-BB/2
t,l,r=faces(bx,by,bz,BB)
# inside: obsidian base + glowing core
ob_h=BB*0.30
ot,ol,orr=faces(bx+3*S,by,bz+3*S,BB-6*S)
ot2,_,_=faces(bx+3*S,by,bz+3*S,BB-6*S)
obsidian=faces(bx+3*S,by,bz+3*S,BB-6*S)
# draw obsidian as a short box
def boxf(x0,y0,z0,sx,sy,sz):
    x1,y1,z1=x0+sx,y0+sy,z0+sz
    return ([iso(x0,y1,z0),iso(x1,y1,z0),iso(x1,y1,z1),iso(x0,y1,z1)],
            [iso(x0,y0,z1),iso(x1,y0,z1),iso(x1,y1,z1),iso(x0,y1,z1)],
            [iso(x1,y0,z0),iso(x1,y0,z1),iso(x1,y1,z1),iso(x1,y1,z0)])
ot,ol,orr=boxf(bx+3*S,by,bz+3*S,BB-6*S,ob_h,BB-6*S)
poly(d,ol,(40,24,62,255)); poly(d,orr,(28,16,46,255)); poly(d,ot,(70,44,100,255))
cs=BB*0.52; c0=(BB-cs)/2
ct,cl,cr=boxf(bx+c0,by+ob_h,bz+c0,cs,cs,cs)
core=Image.new("RGBA",(W,W),(0,0,0,0)); cd=ImageDraw.Draw(core)
poly(cd,cl,(120,235,250,255)); poly(cd,cr,(80,205,230,255)); poly(cd,ct,(220,255,255,255))
im=Image.alpha_composite(im,core.filter(ImageFilter.GaussianBlur(6*S))); im=Image.alpha_composite(im,core)
# glass shell (translucent faces, white frame)
glass=Image.new("RGBA",(W,W),(0,0,0,0)); gd=ImageDraw.Draw(glass)
poly(gd,l,(150,230,255,45)); poly(gd,r,(120,210,245,55)); poly(gd,t,(200,245,255,70))
im=Image.alpha_composite(im,glass); d=ImageDraw.Draw(im)
for f in (t,l,r):
    d.line(f+[f[0]],fill=(235,252,255,255),width=6*S,joint="curve")
    d.line(inset(f,0.80)+[inset(f,0.80)[0]],fill=(200,240,255,150),width=2*S)
# beam
top_c=iso(0,by+BB,0)
beam=Image.new("RGBA",(W,W),(0,0,0,0)); bd=ImageDraw.Draw(beam)
bw=12*S
bd.rectangle([top_c[0]-bw,0,top_c[0]+bw,top_c[1]],fill=(190,245,255,215))
bd.rectangle([top_c[0]-bw/2.5,0,top_c[0]+bw/2.5,top_c[1]],fill=(255,255,255,255))
im=Image.alpha_composite(im,beam.filter(ImageFilter.GaussianBlur(10*S))); im=Image.alpha_composite(im,beam)
m=Image.new("L",(W,W),0); ImageDraw.Draw(m).rounded_rectangle([0,0,W-1,W-1],radius=72*S,fill=255)
out=Image.new("RGBA",(W,W),(0,0,0,0)); out.paste(im,(0,0),m)
out.resize((400,400),Image.LANCZOS).save(f"{OUT}/A2-beacon.png")
