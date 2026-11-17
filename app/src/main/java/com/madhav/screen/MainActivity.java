package com.madhav.screen;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.Manifest;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {

    private static final int REQUEST_CAPTURE = 1001;
    private static final int REQUEST_MIC = 1002;

    private int dp(float value) {
        return (int)(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(35), dp(24), dp(20));
        root.setBackgroundColor(Color.rgb(5, 12, 27));

        TextView title = new TextView(this);
        title.setText("HEY MADHAV");
        title.setTextColor(Color.WHITE);
        title.setTextSize(27);
        title.setTypeface(null, 1);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("YOUR PERSONAL AI ASSISTANT");
        subtitle.setTextColor(Color.rgb(0, 210, 255));
        subtitle.setTextSize(11);
        subtitle.setLetterSpacing(0.18f);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sp =
            new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = dp(8);
        root.addView(subtitle, sp);

        CoreView core = new CoreView();
        LinearLayout.LayoutParams cp =
            new LinearLayout.LayoutParams(dp(260), dp(260));
        cp.topMargin = dp(38);
        root.addView(core, cp);

        TextView status = new TextView(this);
        status.setText("●  WAKE WORD SYSTEM");
        status.setTextColor(Color.rgb(0, 230, 170));
        status.setTextSize(14);
        status.setTypeface(null, 1);
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams stp =
            new LinearLayout.LayoutParams(-1, -2);
        stp.topMargin = dp(25);
        root.addView(status, stp);

        TextView instruction = new TextView(this);
        instruction.setText("Say  \"Hey Madhav\"  to activate");
        instruction.setTextColor(Color.LTGRAY);
        instruction.setTextSize(15);
        instruction.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams ip =
            new LinearLayout.LayoutParams(-1, -2);
        ip.topMargin = dp(12);
        root.addView(instruction, ip);

        TextView info = new TextView(this);
        info.setText("OFFLINE WAKE WORD  •  LOW POWER MODE");
        info.setTextColor(Color.rgb(100, 130, 160));
        info.setTextSize(10);
        info.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams infop =
            new LinearLayout.LayoutParams(-1, -2);
        infop.topMargin = dp(12);
        root.addView(info, infop);

        View line = new View(this);
        line.setBackgroundColor(Color.rgb(30, 55, 80));
        LinearLayout.LayoutParams lp =
            new LinearLayout.LayoutParams(-1, dp(1));
        lp.topMargin = dp(30);
        root.addView(line, lp);

        Button capture = new Button(this);
        capture.setText("SCREEN CAPTURE");
        capture.setTextColor(Color.WHITE);

        GradientDrawable buttonBg = new GradientDrawable();
        buttonBg.setColor(Color.rgb(0, 105, 145));
        buttonBg.setCornerRadius(dp(14));
        capture.setBackground(buttonBg);

        LinearLayout.LayoutParams bp =
            new LinearLayout.LayoutParams(-1, dp(52));
        bp.topMargin = dp(22);
        root.addView(capture, bp);

        capture.setOnClickListener(v -> {
            MediaProjectionManager manager =
                (MediaProjectionManager)getSystemService(
                    MEDIA_PROJECTION_SERVICE
                );
            startActivityForResult(
                manager.createScreenCaptureIntent(),
                REQUEST_CAPTURE
            );
        });

        setContentView(root);

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.POST_NOTIFICATIONS
                },
                REQUEST_MIC
            );
        } else {
            startWakeService();
        }
    }

    private void startWakeService() {
        Intent intent = new Intent(this, WakeService.class);
        startForegroundService(intent);
    }

    @Override
    public void onRequestPermissionsResult(
        int requestCode, String[] permissions, int[] grantResults
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        );

        if (requestCode == REQUEST_MIC &&
            grantResults.length > 0 &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startWakeService();
        }
    }

    @Override
    protected void onActivityResult(
        int requestCode, int resultCode, Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CAPTURE &&
            resultCode == RESULT_OK && data != null) {
            Intent service = new Intent(this, CaptureService.class);
            service.putExtra("resultCode", resultCode);
            service.putExtra("data", data);
            startForegroundService(service);
        }
    }

    private class CoreView extends View {
        private final Paint p = new Paint(3);

        CoreView() {
            super(MainActivity.this);
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float r = Math.min(cx, cy) - dp(8);

            p.setShader(null);
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.rgb(8, 28, 52));
            canvas.drawCircle(cx, cy, r, p);

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(2));
            p.setColor(Color.rgb(0, 190, 255));
            canvas.drawCircle(cx, cy, r, p);

            p.setStrokeWidth(dp(1));
            p.setColor(Color.rgb(0, 105, 170));
            canvas.drawCircle(cx, cy, r * 0.78f, p);
            canvas.drawCircle(cx, cy, r * 0.60f, p);

            p.setStyle(Paint.Style.FILL);
            p.setShader(new RadialGradient(
                cx, cy, r * 0.58f,
                new int[]{
                    Color.rgb(0, 190, 255),
                    Color.rgb(0, 75, 135),
                    Color.rgb(5, 18, 38)
                },
                null, Shader.TileMode.CLAMP
            ));
            canvas.drawCircle(cx, cy, r * 0.48f, p);
            p.setShader(null);

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(dp(3));
            p.setColor(Color.rgb(80, 230, 255));
            canvas.drawArc(
                cx-r*0.88f, cy-r*0.88f,
                cx+r*0.88f, cy+r*0.88f,
                -65, 110, false, p
            );
            canvas.drawArc(
                cx-r*0.88f, cy-r*0.88f,
                cx+r*0.88f, cy+r*0.88f,
                115, 110, false, p
            );

            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.WHITE);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            p.setTextSize(dp(25));
            canvas.drawText("MADHAV", cx, cy + dp(7), p);

            p.setColor(Color.rgb(120, 235, 255));
            p.setTextSize(dp(9));
            canvas.drawText("AI CORE", cx, cy + dp(28), p);
        }
    }
}
