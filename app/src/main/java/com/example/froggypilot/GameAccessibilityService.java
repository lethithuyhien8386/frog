package com.example.froggypilot;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.HardwareBuffer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** User-initiated, offline, screen-aware game controller. */
public final class GameAccessibilityService extends AccessibilityService {
    private static volatile GameAccessibilityService instance;
    public static GameAccessibilityService get(){return instance;}
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private WindowManager wm;
    private View bar;
    private TextView status;
    private WindowManager.LayoutParams layout;
    private boolean running=false, pending=false;
    private int generation=0;
    private String lockedPackage;
    private RestaurantVision engine;
    private long lastActionMs=0, lastUpgradeOpenMs=0;
    private long lastSignature=Long.MIN_VALUE;
    private int lastX=-1,lastY=-1, consecutive=0;
    private RestaurantVision.Kind lastKind=RestaurantVision.Kind.WAIT;

    @Override protected void onServiceConnected(){super.onServiceConnected();instance=this;
        wm=(WindowManager)getSystemService(WINDOW_SERVICE);}
    @Override public void onAccessibilityEvent(AccessibilityEvent event){}
    @Override public void onInterrupt(){stopBot();}
    @Override public boolean onUnbind(android.content.Intent intent){stopBot();closeBar();instance=null;
        return super.onUnbind(intent);}
    @Override public void onDestroy(){stopBot();closeBar();instance=null;worker.shutdownNow();super.onDestroy();}

    private int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView label(String text,int size,int color){
        TextView t=new TextView(this);t.setText(text);t.setTextColor(color);t.setTextSize(size);
        t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setGravity(Gravity.CENTER);return t;
    }
    private GradientDrawable background(){GradientDrawable d=new GradientDrawable();
        d.setColor(Color.rgb(15,32,52));d.setCornerRadius(dp(13));
        d.setStroke(dp(1),Color.rgb(83,115,140));return d;}

    /** Called by MainActivity. First START displays the floating controls. */
    public void openOverlay(){
        if(Looper.myLooper()!=Looper.getMainLooper()){main.post(this::openOverlay);return;}
        if(bar!=null || !Settings.canDrawOverlays(this))return;
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(3),dp(3),dp(3),dp(3));
        row.setBackground(background());
        TextView drag=label("⋮⋮",18,0xffe1edff);
        TextView start=label("▶",20,0xff52ed96);
        TextView stop=label("■",19,0xffff657b);
        status=label("READY",10,Color.WHITE);
        TextView exit=label("×",20,0xffc5d2e2);
        row.addView(drag,new LinearLayout.LayoutParams(dp(33),dp(38)));
        row.addView(start,new LinearLayout.LayoutParams(dp(38),dp(38)));
        row.addView(stop,new LinearLayout.LayoutParams(dp(38),dp(38)));
        row.addView(status,new LinearLayout.LayoutParams(dp(67),dp(38)));
        row.addView(exit,new LinearLayout.LayoutParams(dp(30),dp(38)));
        layout=new WindowManager.LayoutParams(-2,-2,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        layout.gravity=Gravity.TOP|Gravity.LEFT;
        layout.x=dp(3);
        // Avoid the top currency HUD and bottom upgrade toolbar by default.
        layout.y=Math.round(getResources().getDisplayMetrics().heightPixels*.41f);
        drag.setOnTouchListener(new View.OnTouchListener(){
            int startX,startY;float pointerX,pointerY;
            @Override public boolean onTouch(View v,MotionEvent e){
                switch(e.getActionMasked()){
                    case MotionEvent.ACTION_DOWN:
                        startX=layout.x;startY=layout.y;pointerX=e.getRawX();pointerY=e.getRawY();return true;
                    case MotionEvent.ACTION_MOVE:
                        layout.x=Math.max(0,startX+Math.round(e.getRawX()-pointerX));
                        layout.y=Math.max(0,startY+Math.round(e.getRawY()-pointerY));
                        if(bar!=null)try{wm.updateViewLayout(bar,layout);}catch(Exception ignored){}
                        return true;
                    default:return true;
                }
            }
        });
        start.setOnClickListener(v->startBot());stop.setOnClickListener(v->stopBot());
        exit.setOnClickListener(v->{stopBot();closeBar();});
        bar=row;
        try{wm.addView(row,layout);}catch(Exception e){bar=null;status=null;}
    }
    private void closeBar(){if(bar!=null&&wm!=null)try{wm.removeView(bar);}catch(Exception ignored){}
        bar=null;status=null;}
    private void message(String m){if(status!=null)status.setText(m.length()>11?m.substring(0,11):m);}
    private void startBot(){
        if(running)return;
        LocalSettings store=new LocalSettings(this);
        RestaurantVision.Options opt=new RestaurantVision.Options();
        opt.autoCoinUpgrades=store.getText("coins","true").equals("true");
        opt.autoContinue=store.getText("continue","true").equals("true");
        opt.autoOpenUpgrades=store.getText("open","true").equals("true");
        engine=new RestaurantVision(opt);
        running=true;generation++;lockedPackage=null;
        lastActionMs=0;lastUpgradeOpenMs=0;lastSignature=Long.MIN_VALUE;
        lastKind=RestaurantVision.Kind.WAIT;lastX=-1;lastY=-1;consecutive=0;
        message("RUNNING");schedule(generation,45);
    }
    private void stopBot(){
        // Invalidate all capture callbacks and queued taps, immediately.
        running=false;generation++;message("STOPPED");
    }
    private void schedule(int token,long ms){main.postDelayed(()->{
        if(running&&token==generation)capture(token);
    },ms);}
    private boolean gameForeground(){
        AccessibilityNodeInfo root=null;
        try{
            root=getRootInActiveWindow();
            if(root==null||root.getPackageName()==null){message("OPEN GAME");return false;}
            String pkg=root.getPackageName().toString();
            if(pkg.equals(getPackageName()) || pkg.equals("android") ||
                    pkg.startsWith("com.android.") || pkg.startsWith("com.google.android.") ||
                    pkg.startsWith("com.sec.android.") || pkg.contains("launcher") ||
                    pkg.contains("permissioncontroller")){
                message("OPEN GAME");return false;
            }
            if(lockedPackage==null)lockedPackage=pkg;
            if(!lockedPackage.equals(pkg)){message("PAUSED APP");return false;}
            return true;
        }catch(Exception e){message("NO WINDOW");return false;}
        finally{if(root!=null)root.recycle();}
    }
    private void capture(int token){
        if(!gameForeground()){schedule(token,600);return;}
        if(pending){schedule(token,160);return;}
        pending=true;
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.R){
            LegacyProjectionService projection=LegacyProjectionService.get();
            if(projection==null || !LegacyProjectionService.isReady()){
                pending=false;message("CAPTURE OFF");schedule(token,900);return;
            }
            worker.execute(()->{
                Bitmap bmp=projection.acquireBitmap();
                if(bmp!=null)process(bmp,token);
                else main.post(()->{pending=false;if(running&&token==generation)schedule(token,230);});
            });
            return;
        }
        try{
            takeScreenshot(Display.DEFAULT_DISPLAY,getMainExecutor(),new TakeScreenshotCallback(){
                @Override public void onSuccess(ScreenshotResult result){
                    HardwareBuffer hw=result.getHardwareBuffer();Bitmap bmp=null;
                    try{
                        Bitmap base=Bitmap.wrapHardwareBuffer(hw,result.getColorSpace());
                        if(base!=null){bmp=base.copy(Bitmap.Config.ARGB_8888,false);base.recycle();}
                    }catch(Exception ignored){}finally{if(hw!=null)hw.close();}
                    if(bmp==null){pending=false;if(running&&token==generation)schedule(token,260);return;}
                    final Bitmap image=bmp;worker.execute(()->process(image,token));
                }
                @Override public void onFailure(int errorCode){
                    pending=false;
                    if(running&&token==generation){message("SCREEN "+errorCode);
                        schedule(token,errorCode==ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT?420:700);}
                }
            });
        }catch(Exception e){pending=false;if(running&&token==generation)schedule(token,700);}
    }
    private void process(Bitmap bmp,int token){
        Bitmap scaled=bmp;
        try{
            int originalW=bmp.getWidth(),originalH=bmp.getHeight();
            if(originalW!=360){scaled=Bitmap.createScaledBitmap(bmp,360,
                    Math.max(1,Math.round(originalH*360f/originalW)),true);bmp.recycle();}
            int w=scaled.getWidth(),h=scaled.getHeight();
            int[] rgba=new int[w*h];scaled.getPixels(rgba,0,w,0,0,w,h);
            RestaurantVision.Decision d=engine.inspect(rgba,w,h);
            float sx=originalW/(float)w,sy=originalH/(float)h;
            scaled.recycle();
            main.post(()->{
                pending=false;
                if(running&&token==generation)respond(d,sx,sy,token);
            });
        }catch(Exception err){
            if(scaled!=null && !scaled.isRecycled())scaled.recycle();
            main.post(()->{pending=false;if(running&&token==generation){message("IMAGE ERR");schedule(token,700);}});
        }
    }
    private boolean overlayCovers(int x,int y){
        if(bar==null)return false;int[] loc=new int[2];bar.getLocationOnScreen(loc);
        return x>=loc[0] && x<=loc[0]+bar.getWidth() && y>=loc[1] && y<=loc[1]+bar.getHeight();
    }
    private void respond(RestaurantVision.Decision d,float sx,float sy,int token){
        long now=SystemClock.elapsedRealtime();
        if(d.kind==RestaurantVision.Kind.WAIT){message("NO TARGET");schedule(token,200);return;}
        if(d.kind==RestaurantVision.Kind.OPEN_UPGRADES && now-lastUpgradeOpenMs<13500){
            message("AUTO IDLE");schedule(token,480);return;
        }
        // Tutorial interaction must respond as soon as first screenshot returns.
        // Other actions are also faster, but remain rate-limited to avoid
        // accidentally sending multiple gestures before the game updates.
        long gap=d.kind==RestaurantVision.Kind.TUTORIAL_TAP?100:
                d.kind==RestaurantVision.Kind.BUY_WITH_COINS?320:
                d.kind==RestaurantVision.Kind.CONTINUE?320:
                d.kind==RestaurantVision.Kind.CLOSE_PANEL?420:750;
        if(now-lastActionMs<gap){schedule(token,Math.max(65,gap-(now-lastActionMs)));return;}
        int x=Math.round(d.x*sx),y=Math.round(d.y*sy);
        boolean same=d.kind==lastKind && Math.abs(x-lastX)<18 && Math.abs(y-lastY)<18 &&
                d.signature==lastSignature;
        if(same){
            consecutive++;
            if(consecutive>=2){message("NO CHANGE");schedule(token,1050);return;}
        }else consecutive=0;
        if(overlayCovers(x,y)){message("MOVE FLOAT");schedule(token,800);return;}
        if(d.kind==RestaurantVision.Kind.OPEN_UPGRADES)lastUpgradeOpenMs=now;
        Path path=new Path();path.moveTo(x,y);
        GestureDescription gesture=new GestureDescription.Builder().addStroke(
                new GestureDescription.StrokeDescription(path,0,42)).build();
        try{
            boolean sent=dispatchGesture(gesture,new GestureResultCallback(){
                @Override public void onCompleted(GestureDescription g){
                    if(running&&token==generation)schedule(token,120);
                }
                @Override public void onCancelled(GestureDescription g){
                    if(running&&token==generation)schedule(token,320);
                }
            },main);
            if(!sent){message("CLICK ERROR");schedule(token,750);return;}
            lastActionMs=now;lastSignature=d.signature;lastKind=d.kind;lastX=x;lastY=y;
            switch(d.kind){
                case TUTORIAL_TAP:message("TUTORIAL");break;
                case BUY_WITH_COINS:message("COIN UP");break;
                case CONTINUE:message("NEXT");break;
                case CLOSE_PANEL:message("CLOSE");break;
                case OPEN_UPGRADES:message("UPGRADE");break;
                default:message("SCAN");
            }
        }catch(Exception e){message("CLICK ERROR");schedule(token,700);}
    }
}
