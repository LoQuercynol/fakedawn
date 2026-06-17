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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.balau.fakedawn.ColorPickerDialog.OnColorChangedListener;
import org.balau.fakedawn.TimeSlider.OnTimesChangedListener; // Import décommenté

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

public class Preferences extends Activity implements OnClickListener, OnSeekBarChangeListener, OnColorChangedListener, OnTimeSetListener, OnTimesChangedListener {

	// Variables pour ta nouvelle logique
	private int lightStartH = 7, lightStartM = 0;
	private int lightPreDelayMinutes = 15;
	private int lightPostDelayMinutes = 15;
	private int lightDuration = lightPreDelayMinutes + lightPostDelayMinutes;
	private int soundStartH = 7, soundStartM = 15;
	private CheckBox m_checkBoxSoundEnabled;
	private LinearLayout m_containerSoundOptions, m_containerAlarmHours, m_containerAlarmDays, m_containerSoundEnable;
	private ToggleButton m_toggleButtonAlarmEnabled, m_toggleButtonUseDismiss;
	private static final int REQUEST_PICK_SOUND = 0;
	private Uri m_soundUri;
	private HelpListener m_helpListener = new HelpListener();

	private TextView txtLightStart, txtLightPreDelay, txtLightPostDelay, txtSoundStart, textDismissMethod;

	private void updateLabels() {
		if (txtLightStart != null) txtLightStart.setText(String.format("%02d:%02d", lightStartH, lightStartM));
		if (txtLightPreDelay != null) txtLightPreDelay.setText(lightPreDelayMinutes + " min");
		if (txtLightPostDelay != null) txtLightPostDelay.setText(lightPostDelayMinutes + " min");
		if (txtSoundStart != null) txtSoundStart.setText(String.format("%02d:%02d", soundStartH, soundStartM));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.preferences);

		txtLightStart = findViewById(R.id.txtLightStart);
		txtLightPreDelay = findViewById(R.id.txtLightPreDelay);
		txtLightPostDelay = findViewById(R.id.txtLightPostDelay);
		txtSoundStart = findViewById(R.id.txtSoundStart);
		m_toggleButtonUseDismiss = findViewById(R.id.toggleButtonUseDismiss);
		textDismissMethod = findViewById(R.id.textDismissMethod);
		m_checkBoxSoundEnabled = findViewById(R.id.checkBoxSoundEnabled);
		m_containerSoundEnable = findViewById(R.id.containerSoundEnable);
		m_containerSoundOptions = findViewById(R.id.containerSoundOptions);
		m_containerAlarmHours = findViewById(R.id.containerAlarmHours);
		m_containerAlarmDays = findViewById(R.id.containerAlarmDays);
		m_toggleButtonAlarmEnabled = findViewById(R.id.toggleButtonAlarmEnabled);

		m_checkBoxSoundEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
			// Active/Désactive le container et le texte de durée
			updateSoundUI(isChecked);
		});
		if(m_toggleButtonAlarmEnabled != null) m_toggleButtonAlarmEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
			updateGlobalUI(isChecked);
		});
		// Liaison des clics
		/*if(txtLightStart != null) txtLightStart.setOnClickListener(this);
		if(txtLightPreDelay != null) txtLightPreDelay.setOnClickListener(this);
		if(txtLightPostDelay != null) txtLightPostDelay.setOnClickListener(this);
		if(txtSoundStart != null) txtSoundStart.setOnClickListener(this);
		if(txtSoundDuration != null) txtSoundDuration.setOnClickListener(this); */
		if(txtLightStart != null) txtLightStart.setOnClickListener(v -> {
			showTimePicker(lightStartH, lightStartM, (view, hour, minute) -> {
				lightStartH = hour;
				lightStartM = minute;
				// Formule : Sound = Light + PreDelay
				lightStartH = (soundStartH * 60 + soundStartM - lightPreDelayMinutes) % 1440 / 60;
				lightStartM = (soundStartH * 60 + soundStartM - lightPreDelayMinutes) % 1440 % 60;
				updateLabels();
			});
		});
		if(txtLightPreDelay != null) txtLightPreDelay.setOnClickListener(v -> {
				showDurationPicker(lightPreDelayMinutes, (newDelay) -> {
				lightPreDelayMinutes = newDelay;
				// Formule : Sound = Light + PreDelay
				int totalLightMinutes = lightStartH * 60 + lightStartM;
				int totalSoundMinutes = (totalLightMinutes + lightPreDelayMinutes) % 1440;

				soundStartH = totalSoundMinutes / 60;
				soundStartM = totalSoundMinutes % 60;
				lightDuration = lightPreDelayMinutes + lightPostDelayMinutes;
				updateLabels();
			});
		});
		if(txtLightPostDelay != null) txtLightPostDelay.setOnClickListener(v -> {
			showDurationPicker(lightPostDelayMinutes, (newDelay) -> {
				lightPostDelayMinutes = newDelay;
				lightDuration = lightPreDelayMinutes + lightPostDelayMinutes;
				updateLabels();
			});
		});
		if(txtSoundStart != null) txtSoundStart.setOnClickListener(v -> {
			showTimePicker(soundStartH, soundStartM, (view, hour, minute) -> {
				soundStartH = hour;
				soundStartM = minute;
				// Logic: Light = Sound - PreDelay
				int totalMinutes = (soundStartH * 60 + soundStartM) - lightPreDelayMinutes;
				if (totalMinutes < 0) totalMinutes += 1440;
				lightStartH = totalMinutes / 60;
				lightStartM = totalMinutes % 60;
				updateLabels();
			});
		});

		// Liaison des boutons standards
		findViewById(R.id.buttonSave).setOnClickListener(this);
		findViewById(R.id.buttonDiscard).setOnClickListener(this);
		findViewById(R.id.buttonSound).setOnClickListener(this);

		SeekBar seekBarVolume = (SeekBar) findViewById(R.id.seekBarVolume);
		seekBarVolume.setOnSeekBarChangeListener(this);

		loadPreferences();
	}

	private void updateSoundUI(boolean enabled) {
		// Grise ou active les composants
		m_containerSoundOptions.setEnabled(enabled);
		findViewById(R.id.buttonSound).setEnabled(enabled);
		findViewById(R.id.seekBarVolume).setEnabled(enabled);
		findViewById(R.id.toggleButtonVibrate).setEnabled(enabled);

		// Change l'opacité pour l'effet visuel "grisé"
		float alpha = enabled ? 1.0f : 0.4f;
		m_containerSoundOptions.setAlpha(alpha);
	}

	private void updateGlobalUI(boolean enabled) {
		// Liste des vues à griser/désactiver
		View[] viewsToToggle = {
				m_containerAlarmHours, m_containerAlarmDays,txtSoundStart,
				m_containerSoundOptions, m_containerSoundEnable, textDismissMethod, m_toggleButtonUseDismiss
		};

		float alpha = enabled ? 1.0f : 0.4f;
		for (View v : viewsToToggle) {
			if (v != null) {
				v.setAlpha(alpha);
				setRecursiveEnabled(v, enabled);
			}
		}

		// Si l'alarme est activée, on respecte aussi l'état de la checkbox Sound
		if (enabled) {
			updateSoundUI(m_checkBoxSoundEnabled.isChecked());
		}
	}

	private void setRecursiveEnabled(View view, boolean enabled) {
		view.setEnabled(enabled);
		if (view instanceof ViewGroup) {
			ViewGroup group = (ViewGroup) view;
			for (int i = 0; i < group.getChildCount(); i++) {
				setRecursiveEnabled(group.getChildAt(i), enabled);
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		// On vérifie que c'est bien le retour du sélecteur de son (REQUEST_PICK_SOUND est 0 chez vous)
		if (resultCode == RESULT_OK && requestCode == REQUEST_PICK_SOUND) {
			Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);

			// On met à jour la variable de classe
			m_soundUri = uri;

			// On met à jour le texte du bouton immédiatement
			updateSoundButtonText();
		}
	}

	private void updateSoundButtonText() {
		Button buttonSound = findViewById(R.id.buttonSound);
		if (buttonSound == null) return;

		if (m_soundUri != null) {
			// On récupère le titre de la sonnerie via le RingtoneManager
			android.media.Ringtone ringtone = RingtoneManager.getRingtone(this, m_soundUri);
			if (ringtone != null) {
				buttonSound.setText(ringtone.getTitle(this));
			} else {
				buttonSound.setText(getString(R.string.sound_selected));
			}
		} else {
			buttonSound.setText(getString(R.string.Silent));
		}
	}

	private void loadPreferences() {
		SharedPreferences pref = getSharedPreferences("main", MODE_PRIVATE);
		int startH = pref.getInt("dawn_start_hour", 7);
		int startM = pref.getInt("dawn_start_minute", 0);
		int globalStart = startH * 60 + startM;

		// Calcul des heures à partir des offsets enregistrés
		int lStart = globalStart; // + pref.getInt("light_start", 0);
		lightStartH = lStart / 60;
		lightStartM = lStart % 60;
		lightPreDelayMinutes = pref.getInt("light_pre_delay", 15);
		lightPostDelayMinutes = pref.getInt("light_post_delay", 15);
		lightDuration = lightPreDelayMinutes + lightPostDelayMinutes; // pref.getInt("light_max", 30) - pref.getInt("light_start", 0);

		int sStart = globalStart + lightPreDelayMinutes; // globalStart + pref.getInt("sound_start", 15);
		soundStartH = sStart / 60;
		soundStartM = sStart % 60;

		// --- 2. État Global et Volume ---
		ToggleButton alarmEnabled = findViewById(R.id.toggleButtonAlarmEnabled);
		if (alarmEnabled != null) {
			boolean isEnabled = pref.getBoolean("enabled", true);
			alarmEnabled.setChecked(pref.getBoolean("enabled", true));
			updateGlobalUI(isEnabled);
		}

		boolean soundMaster = pref.getBoolean("sound_master_enabled", true);
		m_checkBoxSoundEnabled.setChecked(soundMaster);
		updateSoundUI(soundMaster);

		SeekBar sb = (SeekBar) findViewById(R.id.seekBarVolume);
		if (sb != null) sb.setProgress(pref.getInt("volume", 50));

		// --- 3. Vibreur et Méthode d'arrêt ---
		ToggleButton vibrateButton = (ToggleButton) findViewById(R.id.toggleButtonVibrate);
		if (vibrateButton != null) vibrateButton.setChecked(pref.getBoolean("vibrate", false));

		ToggleButton dismissButton = (ToggleButton) findViewById(R.id.toggleButtonUseDismiss);
		if (dismissButton != null) dismissButton.setChecked(pref.getBoolean("dismiss", false));

		// --- 4. Jours de la semaine ---
		int[] checkBoxes = {R.id.checkBoxMondays, R.id.checkBoxTuesdays, R.id.checkBoxWednesdays,
				R.id.checkBoxThursdays, R.id.checkBoxFridays, R.id.checkBoxSaturdays, R.id.checkBoxSundays};
		String[] keys = {"mondays", "tuesdays", "wednesdays", "thursdays", "fridays", "saturdays", "sundays"};

		for (int i = 0; i < checkBoxes.length; i++) {
			CheckBox cb = (CheckBox) findViewById(checkBoxes[i]);
			if (cb != null) cb.setChecked(pref.getBoolean(keys[i], true));
		}

		// --- 5. Son ---
		String soundStr = pref.getString("sound", null);
		if (soundStr != null && !soundStr.isEmpty()) {
			m_soundUri = Uri.parse(soundStr);
		} else {
			m_soundUri = Settings.System.DEFAULT_ALARM_ALERT_URI;
		}
		// Validation du son (s'assure que le fichier existe toujours)
		m_soundUri = checkSound(this, m_soundUri);
		updateSoundButtonText();
		// Mise à jour des textes (Heures/Durées)
		updateLabels();
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();

		if (id == R.id.buttonSave) {
			savePreferences();
		} else if (id == R.id.buttonDiscard) {
			finish();
		} else if (id == R.id.buttonSound) {
			pickSound();
		} else if (id == R.id.txtLightStart) {
			showTimePicker(lightStartH, lightStartM, (view, h, m) -> {
				lightStartH = h; lightStartM = m;
				((TextView)v).setText(String.format("%02d:%02d", h, m));
				updateLabels();
			});
		} /* else if (id == R.id.txtLightPreDelay) {
			showDurationPicker(lightPreDelayMinutes, (val) -> {
				lightPreDelayMinutes = val;
				((TextView)v).setText(val + " min");
				updateLabels();
			});
		} else if (id == R.id.txtLightPostDelay) {
			showDurationPicker(lightPostDelayMinutes, (val) -> {
				lightPostDelayMinutes = val;
				((TextView)v).setText(val + " min");
				updateLabels();
			});
		} */
		  else if (id == R.id.txtSoundStart) {
			showTimePicker(soundStartH, soundStartM, (view, h, m) -> {
				soundStartH = h; soundStartM = m;
				updateLabels();
			});
		}
	}

	private void showTimePicker(int h, int m, OnTimeSetListener listener) {
		new TimePickerDialog(this, listener, h, m, true).show();
	}

	private void showDurationPicker(int currentVal, OnDurationSetListener listener) {
		final NumberPicker np = new NumberPicker(this);
		np.setMinValue(1);
		np.setMaxValue(120);
		np.setValue(currentVal);
		new AlertDialog.Builder(this)
				.setTitle("Durée (minutes)")
				.setView(np)
				.setPositiveButton("OK", (d, w) -> listener.onSet(np.getValue()))
				.show();
	}

	private void savePreferences() {
		SharedPreferences pref = getSharedPreferences("main", MODE_PRIVATE);
		SharedPreferences.Editor editor = pref.edit();
		// 1. Calcul de l'heure de début absolue (le plus tôt entre lumière et son)
		int lightStartTotal = lightStartH * 60 + lightStartM;
		int soundStartTotal = soundStartH * 60 + soundStartM;
		int globalStartTotal = Math.min(lightStartTotal, soundStartTotal);

		// 2. Sauvegarde de l'heure de réveil système
		editor.putInt("dawn_start_hour", globalStartTotal / 60);
		editor.putInt("dawn_start_minute", globalStartTotal % 60);

		// 3. Sauvegarde des décalages (en minutes par rapport au début global)
		editor.putInt("light_pre_delay", lightPreDelayMinutes);
		editor.putInt("light_post_delay", lightPostDelayMinutes);

		editor.putBoolean("sound_master_enabled", m_checkBoxSoundEnabled.isChecked());

		// 4. Jours de la semaine
		int[] checkBoxes = {R.id.checkBoxMondays, R.id.checkBoxTuesdays, R.id.checkBoxWednesdays,
				R.id.checkBoxThursdays, R.id.checkBoxFridays, R.id.checkBoxSaturdays, R.id.checkBoxSundays};
		String[] keys = {"mondays", "tuesdays", "wednesdays", "thursdays", "fridays", "saturdays", "sundays"};

		for (int i = 0; i < checkBoxes.length; i++) {
			CheckBox cb = (CheckBox) findViewById(checkBoxes[i]);
			if (cb != null) editor.putBoolean(keys[i], cb.isChecked());
		}

		// Volume
		SeekBar sb = (SeekBar) findViewById(R.id.seekBarVolume);
		if (sb != null) editor.putInt("volume", sb.getProgress());

		// Vibreur et Dismiss
		ToggleButton vibrateButton = (ToggleButton) findViewById(R.id.toggleButtonVibrate);
		if (vibrateButton != null) editor.putBoolean("vibrate", vibrateButton.isChecked());

		ToggleButton dismissButton = (ToggleButton) findViewById(R.id.toggleButtonUseDismiss);
		if (dismissButton != null) editor.putBoolean("dismiss", dismissButton.isChecked());

		// 4. Autres réglages (Volume, État alarme, etc.)
		ToggleButton alarmEnabled = findViewById(R.id.toggleButtonAlarmEnabled);
		if (alarmEnabled != null) {
			editor.putBoolean("enabled", alarmEnabled.isChecked());
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
				android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(Context.ALARM_SERVICE);
				if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
					// Si on n'a pas le droit, on envoie l'utilisateur dans les réglages
					Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
							Uri.parse("package:" + getPackageName()));
					startActivity(intent);
					// return; // On ne sauvegarde pas tant qu'on n'a pas la permission
				}
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
				if (!Settings.canDrawOverlays(this)) {
					Toast.makeText(this, getString(R.string.overlay_permission), Toast.LENGTH_LONG).show();
					Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
							Uri.parse("package:" + getPackageName()));
					startActivity(intent);
				}
			}
		}

		Log.d("FakeDawn", "Alarme activée : " + alarmEnabled.isChecked());
		if (m_soundUri != null) {
			editor.putString("sound", m_soundUri.toString());
		} else {
			editor.putString("sound", "");
		}
		editor.commit();
		// editor.apply();

		// 5. Relancer le service pour mettre à jour l'alarme système
		Intent updateAlarm = new Intent(this, Alarm.class);
		// updateAlarm.putExtra("force_update", true); // On peut ajouter un flag pour forcer
		updateAlarm.putExtra(Alarm.EXTRA_SHOW_TOAST, true);

		// 2. Sur Android 12+, il est parfois plus fiable de stopper
		// le service avant de le relancer pour forcer un rafraîchissement complet
		stopService(updateAlarm);
		try {
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
				startForegroundService(updateAlarm);
			} else {
				startService(updateAlarm);
			}
		} catch (Exception e) {
			Log.e("FakeDawn", "Erreur lors du lancement du service Alarm" + e.getMessage());
		}
		Log.d("FakeDawn", "Preferences sauvegardées" + globalStartTotal / 60 + "h" + globalStartTotal % 60);
		setResult(RESULT_OK);
		finish();
	}

	private void pickSound() {
		Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
		// 1. Permet d'afficher l'option "Silencieux" ou "Aucun" en haut de la liste
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);

		// 2. Titre de la fenêtre
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Choisir une sonnerie");

		// 3. Pré-sélectionner la sonnerie actuelle (si elle existe)
		if (m_soundUri != null) {
			intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, m_soundUri);
		}
		startActivityForResult(intent, REQUEST_PICK_SOUND);
	}

	private void changeSound(Uri sound) {
		m_soundUri = sound;
	}

	public static Uri checkSound(Context context, Uri sound) {
		//TODO: move in other class?
		Uri[] sounds = {
				sound,
				Settings.System.DEFAULT_ALARM_ALERT_URI,
				Settings.System.DEFAULT_RINGTONE_URI,
				Settings.System.DEFAULT_NOTIFICATION_URI,
		};
		for (Uri s: sounds)
		{
			try {
				InputStream tmp = context.getContentResolver().openInputStream(s);
				tmp.close();
				//TODO: toast if not first.
				return s;
			} catch (FileNotFoundException e) {
				continue;
			} catch (IOException e) {
				continue;
			}
		}
		//TODO: error toast
		return null;
	}
	// --- MÉTHODES D'INTERFACE (Indispensables pour la compilation) ---

	@Override public void onProgressChanged(SeekBar sb, int p, boolean b) {}
	@Override public void onStartTrackingTouch(SeekBar sb) {}
	@Override public void onStopTrackingTouch(SeekBar sb) {}
	@Override public void colorChanged(int c) {}
	@Override public void onTimeSet(TimePicker v, int h, int m) {}
	@Override public void onTimesChanged(TimeSlider s) {} //}, int l, int r) {}

	// --- CLASSES INTERNES ---

	private class HelpListener implements DialogInterface.OnClickListener {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			// Logique du bouton Aide
		}
	}

	interface OnDurationSetListener {
		void onSet(int value);
	}

	private class VolumePreview {
		// Logique de preview sonore
	}
}