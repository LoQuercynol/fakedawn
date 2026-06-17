/**
 *   Copyright 2012 Francesco Balducci
 *   Copyright 2026 Olivier Vialatte
 *
 *   This file is part of FakeDawn.
 *
 *   FakeDawn is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   FakeDawn is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with FakeDawn.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.balau.fakedawn;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
// import android.widget.TextView;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
// import android.util.Log;
import java.io.IOException;
import android.os.Vibrator;

public class Dawn extends Activity {
	private boolean useDismissButton;
	private ValueAnimator colorAnimation;
	private MediaPlayer mediaPlayer;
	private final Handler soundHandler = new Handler(Looper.getMainLooper());
	private Runnable stopSoundRunnable;
	private Vibrator vibrator;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// 1. Luminosité au minimum immédiatement
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.screenBrightness = 0.01f;
		getWindow().setAttributes(lp);

		// 1. Gestion du réveil de l'écran (Indispensable pour Android 14)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
			setShowWhenLocked(true);
			setTurnScreenOn(true);
		}
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
				WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
				WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
				WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

		// 2. Passage en plein écran (cache les barres système)
		getWindow().getDecorView().setSystemUiVisibility(
				View.SYSTEM_UI_FLAG_LAYOUT_STABLE
						| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
						| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
						| View.SYSTEM_UI_FLAG_FULLSCREEN
						| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

		super.onCreate(savedInstanceState);
		setContentView(R.layout.predawn);
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		if (nm != null) nm.cancel(2); // AlarmReceiver.NOTIFICATION_ALARM_RUNNING_ID);
		SharedPreferences pref = getSharedPreferences("main", MODE_PRIVATE);
		useDismissButton = pref.getBoolean("dismiss", false);
		View root = findViewById(R.id.predawn_root);
		root.setOnClickListener(v -> {
			if (!useDismissButton) {
				// Méthode "Touch Screen" : on arrête tout
				stopAlarm();
			} else {
				// Méthode "Dismiss Button" : on bascule vers la vue normale
				showDawnWithButton();
			}
		});
		soundHandler.postDelayed(() -> {
			boolean soundEnabled = pref.getBoolean("sound_master_enabled", true);

			if (soundEnabled) {
				startAlarmSound(); // Lance la musique
				boolean vibrateEnabled = pref.getBoolean("vibrate", false); // Clé de votre ToggleButton
				if (vibrateEnabled) {
					vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
					if (vibrator != null && vibrator.hasVibrator()) {
						// Vibration : 0ms pause, 500ms vibre, 500ms pause... (en boucle)
						long[] pattern = {0, 1000, 10000};
						vibrator.vibrate(pattern, 0); // 0 = répéter indéfiniment
					}
				}
				// 2. Programmation de l'ARRÊT automatique du son
				stopSoundRunnable = () -> {
					if (mediaPlayer != null) {
						if (mediaPlayer.isPlaying()) mediaPlayer.stop();
						mediaPlayer.release();
						mediaPlayer = null;
						// Log.d("FakeDawn", "Sound stopped automatically after duration limit.");
						if (vibrator != null) {
							vibrator.cancel();
						}
					}
				};
				soundHandler.postDelayed(stopSoundRunnable, pref.getInt("sound_duration", 15) * 60 * 1000L);
			}
		}, pref.getInt("light_pre_delay", 15) * 60 * 1000L);
		// if (nm != null) nm.cancel(2); // AlarmReceiver.NOTIFICATION_ALARM_RUNNING_ID);
		startColorAnimation(root);
	}

	private void startAlarmSound() {
		SharedPreferences pref = getSharedPreferences("main", MODE_PRIVATE);
		String soundStr = pref.getString("sound", "");

		if (soundStr.isEmpty()) {
			// Log.d("FakeDawn", "No sound selected (Silent)");
			return;
		}

		try {
			mediaPlayer = new MediaPlayer();
			mediaPlayer.setDataSource(this, Uri.parse(soundStr));

			// Configuration pour que le son sorte comme une alarme (même si le téléphone est en vibreur)
			mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
					.setUsage(AudioAttributes.USAGE_ALARM)
					.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
					.build());

			mediaPlayer.setLooping(true); // La musique boucle jusqu'à l'arrêt
			mediaPlayer.prepare();
			mediaPlayer.start();

			// Log.d("FakeDawn", "Alarm sound started");
		} catch (IOException e) {
			// Log.e("FakeDawn", "Error playing sound: " + e.getMessage());
		}
	}

	private void showDawnWithButton() {
		if (colorAnimation != null) colorAnimation.cancel();
		// Affiche le layout avec le bouton
		setContentView(R.layout.dawn);

		// Rétablir la luminosité normale (système)
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.screenBrightness = -1f; // -1 = Valeur par défaut de l'utilisateur
		getWindow().setAttributes(lp);
		final View background = findViewById(R.id.dawn_background);

		// Configurer le bouton Dismiss de la nouvelle vue
		Button dismissBtn = findViewById(R.id.dismiss_button);
		if (dismissBtn != null) {
			dismissBtn.setOnClickListener(v -> stopAlarm());
		}

		// Lancer l'animation de couleur si vous en avez une
		startColorAnimation(background);
	}

	private void startColorAnimation(View background) {
		// 5. Calcul de la durée de transition
		SharedPreferences pref = getSharedPreferences("main", MODE_PRIVATE);
		// On récupère la durée : light_max (fin) - light_start (début)
		int durationMinutes = pref.getInt("light_pre_delay", 15) + pref.getInt("light_post_delay", 15);
		if (durationMinutes <= 0) durationMinutes = 1; // Sécurité
		long durationMillis = durationMinutes * 60 * 1000;

		// 6. Animation du fond (Noir vers Blanc)
		colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), Color.BLACK, Color.WHITE);
		colorAnimation.setDuration(durationMillis);

		colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animator) {
				int color = (int) animator.getAnimatedValue();
				background.setBackgroundColor(color);

				// Optionnel : On peut aussi ajuster la luminosité physique de l'écran
				WindowManager.LayoutParams lp = getWindow().getAttributes();
				if (lp.screenBrightness < 1.0f && lp.screenBrightness != -1f) {
					lp.screenBrightness = animator.getAnimatedFraction();
					getWindow().setAttributes(lp);
				}
			}
		});

		colorAnimation.start();
	}
	private void stopAlarm() {
		soundHandler.removeCallbacksAndMessages(null);
		if (colorAnimation != null) colorAnimation.cancel();
		if (vibrator != null) {
			vibrator.cancel();
		}
		if (mediaPlayer != null) {
			try {
				if (mediaPlayer.isPlaying()) mediaPlayer.stop();
			} catch (Exception e) {
				mediaPlayer.release();
				mediaPlayer = null;
			}
		}
		// 2. EFFACER LA NOTIFICATION "En cours"
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		if (nm != null) nm.cancel(2); // ID 2 correspond à celui d'AlarmReceiver

		// 3. Fermer l'activité
		finish();
	}
}


	/*
		// 3. Récupération des vues
		final View background = findViewById(R.id.dawn_background);
		Button dismissButton = findViewById(R.id.dismiss_button);
		// Note: DigitalClock est déprécié mais fonctionne encore si vous l'utilisez dans le XML

		// 4. Action du bouton Dismiss
		dismissButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Arrêter l'activité (et donc l'alarme visuelle)
				finish();
			}
		});

		// 5. Calcul de la durée de transition
		SharedPreferences pref = getSharedPreferences("main", MODE_PRIVATE);
		// On récupère la durée : light_max (fin) - light_start (début)
		int durationMinutes = pref.getInt("light_max", 30) - pref.getInt("light_start", 0);
		if (durationMinutes <= 0) durationMinutes = 1; // Sécurité
		long durationMillis = durationMinutes * 60 * 1000;

		// 6. Animation du fond (Noir vers Blanc)
		ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), Color.BLACK, Color.WHITE);
		colorAnimation.setDuration(durationMillis);

		colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animator) {
				int color = (int) animator.getAnimatedValue();
				background.setBackgroundColor(color);

				// Optionnel : On peut aussi ajuster la luminosité physique de l'écran
				WindowManager.LayoutParams lp = getWindow().getAttributes();
				lp.screenBrightness = animator.getAnimatedFraction(); // de 0.0 à 1.0
				getWindow().setAttributes(lp);
			}
		});

		colorAnimation.start();
	}
}*/