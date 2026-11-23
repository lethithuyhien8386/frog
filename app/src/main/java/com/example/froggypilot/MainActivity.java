package com.example.froggypilot;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** All controls are local, user-visible and explicitly permission gated. */
public final class MainActivity extends Activity {
    private static final int CAPTURE_CODE=1104;
    private LinearLayout body;
    private TextView permission;
    private CheckBox coins,continueButton,openUpgrades;
    private LocalSettings settings;
    private int white=0xfff0f7ff,muted=0xffb7c6dd;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);settings=new LocalSettings(this);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(12,22,40));
        body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(20),dp(28),dp(20),dp(30));
        scroll.addView(body);setContentView(scroll);
        body.addView(text("FROGGY PILOT AUTO",24,white,true));
        body.addView(text("v1.1 FASTFIX • Nhận diện tutorial • Chạm nhanh • Offline",13,muted,false));
        gap(22);title("1. Quyền truy cập");
        permission=text("Đang kiểm tra…",13,white,false);body.addView(permission);gap(10);
        Button access=button("CLICK — Bật quyền Trợ năng (Accessibility)");body.addView(access);
        access.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        gap(8);
        Button overlay=button("FLOAT — Cho phép hiển thị trên ứng dụng khác");body.addView(overlay);
        overlay.setOnClickListener(v->{
            try{startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:"+getPackageName())));}
            catch(Exception err){startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));}
        });
        if(Build.VERSION.SDK_INT<Build.VERSION_CODES.R){
            gap(8);Button screenshot=button("CAPTURE — Cho phép ghi hình màn hình (Android 8–10)");
            body.addView(screenshot);screenshot.setOnClickListener(v->requestCapture());
        }else{
            gap(7);body.addView(text("CAPTURE: Android 11+ chụp màn hình bằng quyền Accessibility đã bật; không cần ứng dụng bên ngoài.",12,muted,false));
        }
        gap(23);title("2. Cách chơi tự động");
        body.addView(text("Đã thêm nhận diện vùng sáng của màn hướng dẫn để chạm ngay; tiếp theo tự xử lý nút giá xu, tiếp tục và nâng cấp khi nhận diện được.",13,muted,false));
        gap(12);
        coins=check("Tự mua nâng cấp / mở món bằng xu", "coins");
        continueButton=check("Tự nhấn tiếp tục khi nhận diện được", "continue");
        openUpgrades=check("Tự mở trang nâng cấp định kỳ", "open");
        body.addView(text("Bảo vệ: KHÔNG tự mua bằng kim cương, tiền thật hoặc tự mở quảng cáo. Không có quyền root. Khi chuyển sang ứng dụng khác bot sẽ tạm ngừng.",12,0xffffd3a1,false));
        gap(21);title("3. Khởi động");
        Button start=button("START — MỞ THANH NỔI");body.addView(start);
        start.setOnClickListener(v->{
            save();
            if(!Settings.canDrawOverlays(this)){toast("Chưa cấp quyền FLOAT");return;}
            if(GameAccessibilityService.get()==null){toast("Chưa cấp quyền CLICK — hãy bật Froggy Pilot trong Trợ năng");return;}
            if(Build.VERSION.SDK_INT<Build.VERSION_CODES.R && !LegacyProjectionService.isReady()){
                toast("Cần cấp quyền CAPTURE");requestCapture();return;
            }
            GameAccessibilityService.get().openOverlay();
            toast("Chuyển sang game và nhấn ▶ trên thanh nổi. Nhấn ■ để dừng.");
        });
        gap(17);
        body.addView(text("Thanh nổi: kéo biểu tượng ⋮⋮ để đổi vị trí; ▶ bắt đầu, ■ dừng toàn bộ chạm tự động, × đóng. Đây là bản nhận diện quy tắc, cần kiểm thử trên đúng thiết bị/game; không bảo đảm vượt mọi màn.",12,muted,false));
        refresh();
    }
    private int dp(float v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private TextView text(String s,int size,int color,boolean bold){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setLineSpacing(dp(2),1.06f);return t;
    }
    private void gap(int h){View space=new View(this);body.addView(space,
            new LinearLayout.LayoutParams(1,dp(h)));}
    private void title(String s){body.addView(text(s,17,white,true));gap(10);}
    private Button button(String s){Button b=new Button(this);b.setText(s);b.setAllCaps(false);
        b.setTextColor(Color.WHITE);b.setTextSize(13);
        GradientDrawable bg=new GradientDrawable();bg.setColor(0xff235a9b);bg.setCornerRadius(dp(12));
        b.setBackground(bg);b.setMinHeight(dp(52));b.setPadding(dp(11),dp(9),dp(11),dp(9));
        return b;}
    private CheckBox check(String title,String key){
        CheckBox cb=new CheckBox(this);cb.setText(title);cb.setTextSize(14);cb.setTextColor(white);
        cb.setChecked(settings.getText(key,"true").equals("true"));
        cb.setButtonTintList(android.content.res.ColorStateList.valueOf(0xff52ed96));
        cb.setOnCheckedChangeListener((b,v)->settings.putText(key,String.valueOf(v)));
        body.addView(cb);return cb;
    }
    private void save(){settings.putText("coins",String.valueOf(coins.isChecked()));
        settings.putText("continue",String.valueOf(continueButton.isChecked()));
        settings.putText("open",String.valueOf(openUpgrades.isChecked()));}
    private void refresh(){
        if(permission==null)return;
        boolean access=GameAccessibilityService.get()!=null;
        boolean overlay=Settings.canDrawOverlays(this);
        String capture=Build.VERSION.SDK_INT>=Build.VERSION_CODES.R?"Bằng CLICK":
                (LegacyProjectionService.isReady()?"Đã cấp":"Chưa cấp");
        permission.setText("CLICK: "+(access?"✓ Đã bật":"✗ Chưa bật")+
                "    FLOAT: "+(overlay?"✓ Đã bật":"✗ Chưa bật")+"\nCAPTURE: "+capture);
    }
    @Override public void onResume(){super.onResume();refresh();}
    private void toast(String m){Toast.makeText(this,m,Toast.LENGTH_LONG).show();}
    private void requestCapture(){
        MediaProjectionManager mgr=(MediaProjectionManager)getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if(mgr!=null)startActivityForResult(mgr.createScreenCaptureIntent(),CAPTURE_CODE);
        else toast("Thiết bị không hỗ trợ chụp màn hình");
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request==CAPTURE_CODE){
            if(result==RESULT_OK&&data!=null){
                Intent intent=new Intent(this,LegacyProjectionService.class);
                intent.putExtra("result_code",result);intent.putExtra("result_data",data);
                if(Build.VERSION.SDK_INT>=26)startForegroundService(intent);else startService(intent);
                toast("Đã cấp quyền CAPTURE");
            }else toast("Chưa cấp quyền CAPTURE");
        }
    }
}
