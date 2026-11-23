package com.example.froggypilot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Device-local pixel recognizer for the FROGGY eatery game in the eleven
 * supplied recordings. No neural service, network access, or OCR dependency.
 * Coordinates returned are in the same space as the input screenshot.
 *
 * This engine deliberately limits autonomous purchases to buttons recognized
 * as bright-green *coin* prices. Green gem/advertising/real-money offers are
 * not purchased. The recipes, positions and level graphics may change.
 */
public final class RestaurantVision {
    public enum Kind { BUY_WITH_COINS, CONTINUE, CLOSE_PANEL, OPEN_UPGRADES, WAIT }
    public static final class Decision {
        public final Kind kind;
        public final int x, y;
        public final double confidence;
        public final long signature;
        public final String reason;
        public Decision(Kind kind, int x, int y, double confidence, long signature, String reason) {
            this.kind=kind; this.x=x; this.y=y; this.confidence=confidence;
            this.signature=signature; this.reason=reason;
        }
        @Override public String toString() {
            return kind+" @ ("+x+","+y+") "+String.format(java.util.Locale.US,"%.2f",confidence)+" "+reason;
        }
    }
    public static final class Options {
        public boolean autoCoinUpgrades=true;
        public boolean autoContinue=true;
        public boolean autoOpenUpgrades=true;
    }
    private static final class Box {
        int minX,maxX,minY,maxY,green,area,yellow;
        int width(){return maxX-minX+1;}
        int height(){return maxY-minY+1;}
        int cx(){return (minX+maxX)/2;}
        int cy(){return (minY+maxY)/2;}
        double density(){return green/(double)(width()*height());}
    }
    private final Options settings;
    public RestaurantVision(Options settings){this.settings=settings==null?new Options():settings;}

    /** Analyse already scaled/cropped screenshot; never touches external state. */
    public Decision inspect(int[] argb,int width,int height) {
        if(argb==null || width<160 || height<300 || argb.length<width*height)
            return new Decision(Kind.WAIT,0,0,0,0,"Invalid screenshot");
        long signature=hash(argb,width,height);
        List<Box> buttons=findGreenButtons(argb,width,height);
        boolean modal=looksLikeModal(argb,width,height);
        List<Box> coins=new ArrayList<>();
        List<Box> next=new ArrayList<>();
        for(Box box:buttons){
            // Never target HUD currency +, game ads, bottom toolbar or edges.
            if(box.cy()<height*.23 || box.cy()>height*.85)continue;
            if(box.cx()<width*.30 || box.cx()>width*.88)continue;
            if(box.yellow >= Math.max(12,box.area/95))coins.add(box);
            else if(box.cy()>height*.57 && box.cy()<height*.82 && box.width()>width*.18)
                next.add(box);
        }
        if(settings.autoCoinUpgrades&&!coins.isEmpty()){
            // Management panel: cheapest visible upgrade is usually uppermost.
            // A large level unlock button receives priority over generic offers.
            coins.sort((a,b)->{
                boolean levelA=a.width()>width*.22 && a.cy()>height*.67;
                boolean levelB=b.width()>width*.22 && b.cy()>height*.67;
                if(levelA!=levelB)return levelA?-1:1;
                return Integer.compare(a.cy(),b.cy());
            });
            Box b=coins.get(0);
            return new Decision(Kind.BUY_WITH_COINS,b.cx(),b.cy(),Math.min(.99,.66+b.density()*.29),signature,
                    "Green button with a yellow coin-price icon");
        }
        if(settings.autoContinue && modal && !next.isEmpty()){
            next.sort((a,b)-> Double.compare(
                    Math.abs(a.cx()-width*.52)+Math.abs(a.cy()-height*.65),
                    Math.abs(b.cx()-width*.52)+Math.abs(b.cy()-height*.65)));
            Box b=next.get(0);
            return new Decision(Kind.CONTINUE,b.cx(),b.cy(),.73,signature,"Central green panel button");
        }
        if(modal){
            int[] close=findClose(argb,width,height);
            if(close!=null)return new Decision(Kind.CLOSE_PANEL,close[0],close[1],.76,signature,
                    "Orange round dismiss icon; no safe coin purchase");
            return new Decision(Kind.WAIT,0,0,0,signature,"Modal without trusted action");
        }
        // The green arrow is the regular Upgrade tab at bottom-right in these
        // recordings. This is only activated periodically by the controller.
        if(settings.autoOpenUpgrades && hasUpgradeArrow(argb,width,height))
            return new Decision(Kind.OPEN_UPGRADES,(int)(width*.805),(int)(height*.969),.69,signature,
                    "Bottom-right upgrade tab");
        return new Decision(Kind.WAIT,0,0,0,signature,"Watching restaurant / waiting for new UI");
    }

    private boolean hasUpgradeArrow(int[] p,int w,int h){
        // The bottom navigation bar contains a persistent green upward arrow.
        int n=0,green=0;
        for(int y=(int)(h*.945);y<Math.min(h,(int)(h*.988));y+=2)
            for(int x=(int)(w*.770);x<(int)(w*.847);x+=2){
                n++;if(isGreen(p[y*w+x]))green++;
            }
        return n>0 && green>n*.057;
    }

    private List<Box> findGreenButtons(int[] p,int w,int h){
        // Allocate two compact arrays for the canonical <=480 pixel-wide image.
        int size=w*h;
        boolean[] marks=new boolean[size],seen=new boolean[size];
        for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            int i=y*w+x;
            if(x<(int)(w*.09) || x>(int)(w*.91) || y<(int)(h*.13) || y>(int)(h*.87))continue;
            marks[i]=isGreen(p[i]);
        }
        int[] queue=new int[size];
        List<Box> list=new ArrayList<>();
        for(int y=(int)(h*.13);y<(int)(h*.87);y++)for(int x=(int)(w*.09);x<(int)(w*.91);x++){
            int start=y*w+x;
            if(!marks[start] || seen[start])continue;
            int head=0,tail=0;
            seen[start]=true;queue[tail++]=start;
            Box b=new Box();b.minX=b.maxX=x;b.minY=b.maxY=y;
            while(head<tail){
                int i=queue[head++],xx=i%w,yy=i/w;
                b.green++;
                if(xx<b.minX)b.minX=xx;if(xx>b.maxX)b.maxX=xx;
                if(yy<b.minY)b.minY=yy;if(yy>b.maxY)b.maxY=yy;
                // 8-neighbour connectivity spans anti-aliasing and button text holes.
                for(int dy=-1;dy<=1;dy++)for(int dx=-1;dx<=1;dx++){
                    if(dx==0&&dy==0)continue;
                    int nx=xx+dx,ny=yy+dy;
                    if(nx<0||nx>=w||ny<0||ny>=h)continue;
                    int ni=ny*w+nx;
                    if(marks[ni]&&!seen[ni]){seen[ni]=true;queue[tail++]=ni;}
                }
            }
            b.area=b.width()*b.height();
            if(b.width()<w*.10 || b.width()>w*.42 || b.height()<h*.023 || b.height()>h*.095)
                continue;
            double aspect=b.width()/(double)b.height();
            if(aspect<1.60 || aspect>5.6 || b.density()<.53)continue;
            int count=0;
            // Count yellow coin glyph embedded in green button, not outside it.
            for(int yy=b.minY;yy<=b.maxY;yy++)for(int xx=b.minX;xx<=b.maxX;xx++)
                if(isCoinYellow(p[yy*w+xx]))count++;
            b.yellow=count;
            list.add(b);
        }
        return list;
    }
    private static boolean isGreen(int c){
        int r=(c>>>16)&255,g=(c>>>8)&255,b=c&255;
        return g>=112 && g>r+25 && g>b+14 && g*100>r*126;
    }
    private static boolean isCoinYellow(int c){
        int r=(c>>>16)&255,g=(c>>>8)&255,b=c&255;
        return r>155 && g>95 && b<r*.72 && r>g*1.04;
    }
    private static boolean isOrange(int c){
        int r=(c>>>16)&255,g=(c>>>8)&255,b=c&255;
        return r>168 && g>70 && g<210 && b<165 && r>g*1.17;
    }
    private static int lightness(int c){
        return (((c>>>16)&255)*3 + ((c>>>8)&255)*5 + (c&255)*2)/10;
    }
    private boolean looksLikeModal(int[] p,int w,int h){
        // Large beige, fairly uniform parchment panels are a distinguishing
        // feature of Froggy menus. A single bright horizontal band is NOT
        // sufficient: open restaurant floors are also light-coloured.
        // Sampling 4 separate rows prevents false-positive dismiss taps on
        // the active kitchen (confirmed with video05/06/08/10 frames).
        double[] rows={.40,.50,.60,.70};
        double[] cols={.19,.31,.43,.55,.67,.79};
        int matchingRows=0;
        int half=Math.max(3,w/60);
        for(double fy:rows){
            int cy=(int)(h*fy),creamTiles=0;
            for(double fx:cols){
                int cx=(int)(w*fx),good=0,total=0;
                for(int yy=cy-half;yy<=cy+half;yy++)for(int xx=cx-half;xx<=cx+half;xx++){
                    if(xx<0||xx>=w||yy<0||yy>=h)continue;
                    int rgb=p[yy*w+xx],r=(rgb>>>16)&255,g=(rgb>>>8)&255,b=rgb&255;
                    if(r>182&&g>166&&b>130 && Math.abs(r-g)<55&&Math.abs(g-b)<65 && r>=g-8&&g>=b-8)good++;
                    total++;
                }
                if(total>0 && good>total*.60)creamTiles++;
            }
            if(creamTiles>=4)matchingRows++;
        }
        return matchingRows>=2;
    }
    private int meanLight(int[] p,int w,int h,double x1,double x2,double y1,double y2){
        int sum=0,n=0;
        for(int y=Math.max(0,(int)(h*y1));y<Math.min(h,(int)(h*y2));y+=3)
            for(int x=Math.max(0,(int)(w*x1));x<Math.min(w,(int)(w*x2));x+=3){
                sum+=lightness(p[y*w+x]);n++;
            }
        return n==0?0:sum/n;
    }
    private int[] findClose(int[] p,int w,int h){
        // Small coral/orange X in the top-right of the parchment popup.
        // Its position changes as the panel height changes.
        int bestScore=0,bestX=-1,bestY=-1;
        for(int y=(int)(h*.17);y<(int)(h*.75);y+=3)
            for(int x=(int)(w*.76);x<(int)(w*.90);x+=3){
                int orange=0,white=0;
                int r=Math.max(4,(int)(w*.014));
                for(int dy=-r;dy<=r;dy+=2)for(int dx=-r;dx<=r;dx+=2){
                    int nx=x+dx,ny=y+dy;
                    if(nx<0||ny<0||nx>=w||ny>=h)continue;
                    int c=p[ny*w+nx];
                    if(isOrange(c))orange++;
                    if(lightness(c)>207)white++;
                }
                int score=orange+Math.min(white,12);
                if(score>bestScore){bestScore=score;bestX=x;bestY=y;}
            }
        return bestScore>=12?new int[]{bestX,bestY}:null;
    }
    private long hash(int[] p,int w,int h){
        // FNV-1a perceptual-ish hash. Use quantized color in central screen;
        // ignore rapidly changing top timers and overlay-located edges.
        long a=0xcbf29ce484222325L;
        for(int y=(int)(h*.19);y<(int)(h*.91);y+=Math.max(1,h/34))
            for(int x=(int)(w*.14);x<(int)(w*.88);x+=Math.max(1,w/20)){
                int c=p[y*w+x];
                int quant=((c>>>20)&15)<<8 | ((c>>>12)&15)<<4 | ((c>>>4)&15);
                a^=quant;a*=0x100000001b3L;
            }
        return a;
    }
}
