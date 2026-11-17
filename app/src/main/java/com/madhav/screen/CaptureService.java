package com.madhav.screen;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.hardware.display.*;
import android.media.*;
import android.media.projection.*;
import android.os.*;
import android.provider.MediaStore;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CaptureService extends Service {
    private MediaProjection projection;
    private VirtualDisplay display;
    private ImageReader reader;

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                "madhav_capture", "Madhav Screen Capture",
                NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class)
                .createNotificationChannel(channel);
        }

        Notification notification = new Notification.Builder(this, "madhav_capture")
            .setContentTitle("Madhav Screen")
            .setContentText("Screen capture is active")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build();

        startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int resultCode = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED);
        Intent data = intent.getParcelableExtra("data");

        MediaProjectionManager manager =
            (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        projection = manager.getMediaProjection(resultCode, data);

        if (projection == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        projection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                stopSelf();
            }
        }, new Handler(Looper.getMainLooper()));

        android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        int density = metrics.densityDpi;

        reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);

        reader.setOnImageAvailableListener(r -> {
            Image image = null;
            Bitmap bitmap = null;

            try {
                image = r.acquireLatestImage();
                if (image == null) return;

                int w = image.getWidth();
                int h = image.getHeight();

                Image.Plane plane = image.getPlanes()[0];
                ByteBuffer buffer = plane.getBuffer();
                int rowStride = plane.getRowStride();
                int pixelStride = plane.getPixelStride();

                int[] pixels = new int[w * h];

                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        int index = y * rowStride + x * pixelStride;

                        int red = buffer.get(index) & 0xff;
                        int green = buffer.get(index + 1) & 0xff;
                        int blue = buffer.get(index + 2) & 0xff;
                        int alpha = buffer.get(index + 3) & 0xff;

                        pixels[y * w + x] =
                            (alpha << 24) | (red << 16) | (green << 8) | blue;
                    }
                }

                bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                bitmap.setPixels(pixels, 0, w, 0, 0, w, h);

                ContentValues values = new ContentValues();
                String filename = "madhav_screen_" +
                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                    .format(new Date()) + ".png";

                values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH,
                    "Pictures/HeyMadhav");

                android.net.Uri uri = getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                if (uri != null) {
                    try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                        if (out != null) {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                        }
                    }
                }

            } catch (Exception e) {
                android.util.Log.e("MadhavScreen", "Capture failed", e);
            } finally {
                if (bitmap != null) bitmap.recycle();
                if (image != null) image.close();
                stopSelf();
            }
        }, new Handler(Looper.getMainLooper()));

        display = projection.createVirtualDisplay(
            "MadhavScreen", width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.getSurface(), null, null);

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        if (display != null) display.release();
        if (reader != null) reader.close();
        if (projection != null) projection.stop();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
