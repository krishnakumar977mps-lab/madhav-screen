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

    @Override
    public void onCreate() {
        super.onCreate();

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
                    speechService = new SpeechService(recognizer, 16000.0f);
                    speechService.startListening(this);

                    Log.d("HeyMadhav", "Offline listening started");

                } catch (Exception e) {
                    Log.e("HeyMadhav", "Recognition start failed", e);
                    stopSelf();
                }
            },
            error -> {
                Log.e("HeyMadhav", "Model loading failed", error);
                stopSelf();
            }
        );
    }

    private void checkWakeWord(String result) {
        if (activated || result == null) return;

        String text = result.toLowerCase();

        if (text.contains("madhav") && text.contains("hey")) {
            activated = true;

            if (speechService != null) {
                speechService.stop();
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
        Log.e("HeyMadhav", "Voice recognition error", e);
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
                "/data/data/com.termux/files/usr/bin/python"
            );

            intent.putExtra(
                "com.termux.RUN_COMMAND_ARGUMENTS",
                new String[]{
                    "/data/data/com.termux/files/home/hey-madhav/madhav.py"
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
        }
    }

    @Override
    public void onDestroy() {
        if (speechService != null) {
            speechService.stop();
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
