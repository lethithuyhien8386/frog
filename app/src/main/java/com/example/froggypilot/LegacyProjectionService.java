package com.example.froggypilot;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.IBinder;
import android.view.WindowManager;

import java.nio.ByteBuffer;

/**
 * Screen capture fallback for Android 8-10 (API 26-29).
 * Requires the system MediaProjection consent dialog, in addition to CLICK/FLOAT.
 * On Android 11+ the AccessibilityService takes screenshots directly instead.
 */
public final class LegacyProjectionService extends Service {
    private static final String CHANNEL="froggypilot_screen_capture";
    private static volatile LegacyProjectionService current;
    public static LegacyProjectionService get() { return current; }
    public static boolean isReady() { return current!=null && current.ready; }

    private MediaProjection projection;
    private VirtualDisplay display;
    private ImageReader reader;
    private volatile boolean ready;

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(new NotificationChannel(CHANNEL,
                "Froggy Pilot screen capture",NotificationManager.IMPORTANCE_LOW));
        Notification note=new Notification.Builder(this,CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setContentTitle("Froggy Pilot")
                .setContentText("Đang chia sẻ màn hình để nhận diện game")
                .setOngoing(true).build();
        startForeground(8030,note);
    }

    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        if(ready)return START_NOT_STICKY;
        if(intent==null || !intent.hasExtra("result_code")) {stopSelf();return START_NOT_STICKY;}
        Intent data=intent.getParcelableExtra("result_data");
        if(data==null) {stopSelf();return START_NOT_STICKY;}
        try {
            MediaProjectionManager manager=(MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            projection=manager.getMediaProjection(intent.getIntExtra("result_code",0),data);
            if(projection==null)throw new IllegalStateException("MediaProjection unavailable");
            projection.registerCallback(new MediaProjection.Callback(){
                @Override public void onStop() {ready=false;stopSelf();}
            },null);
            WindowManager wm=(WindowManager)getSystemService(WINDOW_SERVICE);
            Point size=new Point(); wm.getDefaultDisplay().getRealSize(size);
            int w=size.x,h=size.y;
            if(w<180 || h<350)throw new IllegalStateException("Screen dimensions unavailable");
            reader=ImageReader.newInstance(w,h,PixelFormat.RGBA_8888,3);
            display=projection.createVirtualDisplay("FroggyPilot",w,h,
                    getResources().getDisplayMetrics().densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    reader.getSurface(),null,null);
            if(display==null)throw new IllegalStateException("Virtual display unavailable");
            ready=true;current=this;
        } catch(Exception exception) {
            ready=false;current=null;stopSelf();
        }
        return START_NOT_STICKY;
    }

    /** Returns a detached bitmap. Called from a worker thread, never the UI thread. */
    public synchronized Bitmap acquireBitmap() {
        if(!ready || reader==null)return null;
        Image frame=null;
        try {
            frame=reader.acquireLatestImage();
            if(frame==null)return null;
            int w=frame.getWidth(),h=frame.getHeight();
            Image.Plane plane=frame.getPlanes()[0];
            int pixelStride=plane.getPixelStride(),rowStride=plane.getRowStride();
            if(pixelStride!=4 || rowStride%pixelStride!=0)return null;
            int paddedWidth=rowStride/pixelStride;
            ByteBuffer buffer=plane.getBuffer();buffer.rewind();
            Bitmap padded=Bitmap.createBitmap(paddedWidth,h,Bitmap.Config.ARGB_8888);
            padded.copyPixelsFromBuffer(buffer);
            if(paddedWidth==w)return padded;
            Bitmap result=Bitmap.createBitmap(padded,0,0,w,h);
            padded.recycle();return result;
        } catch(Exception e) {return null;}
        finally {if(frame!=null)frame.close();}
    }

    @Override public void onDestroy() {
        ready=false;current=null;
        if(display!=null){display.release();display=null;}
        if(reader!=null){reader.close();reader=null;}
        if(projection!=null){try{projection.stop();}catch(Exception ignored){}projection=null;}
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) {return null;}
}
