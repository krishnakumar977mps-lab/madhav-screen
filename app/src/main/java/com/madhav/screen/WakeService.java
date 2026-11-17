package com.madhav.screen;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.os.*;
import android.util.Log;

import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.StorageService;
import org.vosk.android.SpeechService;
import org.vosk.android.RecognitionListener;

public class WakeService extends Service implements RecognitionListener {

    private Model model;
    private Recognizer recognizer;
    private SpeechService speechService;
    private boolean activated = false;
    private boolean restarting = false;

    private final Handler handler = new Handler(Looper.getMainLooper());

    private final BroadcastReceiver assistantReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.madhav.screen.ASSISTANT_FINISHED".equals(intent.getAction())) {
                Log.d("HeyMadhav", "Assistant finished. Restarting wake listener.");
                activated = false;
                restartListening();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("HeyMadhav", "WAKE SERVICE ONCREATE STARTED");
        android.widget.Toast.makeText(
            this, "MADHAV WAKE SERVICE STARTED",
            android.widget.Toast.LENGTH_LONG
        ).show();

        NotificationChannel channel = new NotificationChannel(
            "madhav_wake",
            "Hey Madhav Wake Word",
            NotificationManager.IMPORTANCE_LOW
        );

        getSystemService(NotificationManager.class)
            .createNotificationChannel(channel);

        Notification notification = new Notification.Builder(this, "madhav_wake")
            .setContentTitle("Hey Madhav")
            .setContentText("Offline Wake Word सुन रहा हूँ")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build();

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                2, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            );
        } else {
            startForeground(2, notification);
        }

        IntentFilter filter = new IntentFilter(
            "com.madhav.screen.ASSISTANT_FINISHED"
        );

        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(assistantReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(assistantReceiver, filter);
        }

        loadModel();
    }

    private void loadModel() {
        StorageService.unpack(
            this,
            "models/vosk-model-small-en-us-0.15",
            "vosk-model",
            loadedModel -> {
                model = loadedModel;

                try {
                    recognizer = new Recognizer(model, 16000.0f);
                    startListening();
                } catch (Exception e) {
                    Log.e("HeyMadhav", "Recognizer failed", e);
                    stopSelf();
                }
            },
            error -> {
                Log.e("HeyMadhav", "Model loading failed", error);
                stopSelf();
            }
        );
    }

    private void startListening() {
        if (restarting || activated || recognizer == null) return;

        try {
            speechService = new SpeechService(recognizer, 16000.0f);
            speechService.startListening(this);
            Log.d("HeyMadhav", "Wake listener active");
        } catch (Exception e) {
            Log.e("HeyMadhav", "Listening failed", e);
        }
    }

    private void restartListening() {
        if (restarting) return;

        restarting = true;

        if (speechService != null) {
            speechService.shutdown();
            speechService = null;
        }

        handler.postDelayed(() -> {
            restarting = false;
            startListening();
        }, 1000);
    }

    private void checkWakeWord(String result) {
        if (activated || result == null) return;

        String text = result.toLowerCase();

        if (text.contains("madhav") && text.contains("hey")) {
            activated = true;
            Log.d("HeyMadhav", "Wake word detected");

            if (speechService != null) {
                speechService.shutdown();
                speechService = null;
            }

            launchAssistant();
        }
    }

    @Override
    public void onPartialResult(String hypothesis) {
        checkWakeWord(hypothesis);
    }

    @Override
    public void onResult(String hypothesis) {
        checkWakeWord(hypothesis);
    }

    @Override
    public void onFinalResult(String hypothesis) {
        checkWakeWord(hypothesis);
    }

    @Override
    public void onError(Exception e) {
        Log.e("HeyMadhav", "Recognition error", e);
    }

    @Override
    public void onTimeout() {
        Log.d("HeyMadhav", "Recognition timeout");
    }

    private void launchAssistant() {
        try {
            Intent intent = new Intent();
            intent.setClassName(
                "com.termux",
                "com.termux.app.RunCommandService"
            );
            intent.setAction("com.termux.RUN_COMMAND");

            intent.putExtra(
                "com.termux.RUN_COMMAND_PATH",
                "/data/data/com.termux/files/usr/bin/bash"
            );

            intent.putExtra(
                "com.termux.RUN_COMMAND_ARGUMENTS",
                new String[]{
                    "/data/data/com.termux/files/home/hey-madhav/start_madhav.sh"
                }
            );

            intent.putExtra(
                "com.termux.RUN_COMMAND_WORKDIR",
                "/data/data/com.termux/files/home/hey-madhav"
            );

            intent.putExtra("com.termux.RUN_COMMAND_BACKGROUND", true);

            startService(intent);

            Log.d("HeyMadhav", "Assistant launched");

        } catch (Exception e) {
            Log.e("HeyMadhav", "Termux launch failed", e);
            activated = false;
            restartListening();
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);

        try {
            unregisterReceiver(assistantReceiver);
        } catch (Exception ignored) {}

        if (speechService != null) {
            speechService.shutdown();
            speechService = null;
        }

        if (recognizer != null) {
            recognizer.close();
            recognizer = null;
        }

        if (model != null) {
            model.close();
            model = null;
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
