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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import android.content.pm.ServiceInfo;

public class Alarm extends Service {

	public static final String EXTRA_SHOW_TOAST = "org.balau.fakedawn.Alarm.EXTRA_SHOW_TOAST";
	private static final int NOTIFICATION_ID = 1;
	private static final long TOLERANCE_MILLIS = 1000*10;

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// 1. Créer le canal de notification pour Android 8+
		/* String CHANNEL_ID = "fakedawn_service";
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
					"Alarme FakeDawn", NotificationManager.IMPORTANCE_LOW);
			getSystemService(NotificationManager.class).createNotificationChannel(channel);
		}

		// 2. Lancer en premier plan (Foreground) pour éviter le crash immédiat
		Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
				.setContentTitle("FakeDawn")
				.setContentText("Mise à jour de l'alarme...")
				.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
				.build();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
			startForeground(
					NOTIFICATION_ID,
					notification,
					ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE // Matches the manifest
			);
		} else {
			startForeground(NOTIFICATION_ID, notification);
		} */

		boolean showToast;
		if(intent != null)
		{
			showToast = intent.getBooleanExtra(EXTRA_SHOW_TOAST, false);
		}
		else // intent is null when Service is restarted
		{
			showToast = false;
		}

		cancel();
		String message;
		if(getPreferences().getBoolean("enabled", false))
		{
			Calendar nextAlarmTime = getNextAlarmTime();
			if (nextAlarmTime == null)
			{
				message = "No week day selected! Fake Dawn Alarm Disabled.";	
			}
			else
			{
				set(nextAlarmTime);
				message = nextAlarmMessage(nextAlarmTime);
				java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("E dd/MM HH:mm", java.util.Locale.getDefault());
				String timeLabel = sdf.format(nextAlarmTime.getTime());
				showNotification(timeLabel);
				return START_STICKY;
			}
		}
		else
		{
			message = "Fake Dawn Alarm Disabled.";
		}
		Log.d("FakeDawn", message);
		if(showToast)
		{
			Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
		}
		// If we get killed, after returning from here, restart
		stopForeground(true);
		return START_STICKY;
	}

	private void showNotification(String nextAlarmStr) {
		// 1. Créer le canal de notification pour Android 8+
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		String CHANNEL_ID = "fakedawn_service";

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
					getString(R.string.alarme_fakeDawn), NotificationManager.IMPORTANCE_LOW);
			nm.createNotificationChannel(channel);
		}

		Intent intent = new Intent(this, Preferences.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

		int flags = PendingIntent.FLAG_UPDATE_CURRENT;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			flags |= PendingIntent.FLAG_IMMUTABLE;
		}
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, flags);

		// 2. Lancer en premier plan (Foreground) pour éviter le crash immédiat
		NotificationCompat.Builder notification = new NotificationCompat.Builder(this, CHANNEL_ID)
				.setContentTitle(getString(R.string.fakedawn_activated))
				.setContentText(getString(R.string.next_alarm) + nextAlarmStr)
				.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
				.setOngoing(true)
				.setContentIntent(pendingIntent)
				.setPriority(NotificationCompat.PRIORITY_LOW);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
			startForeground(NOTIFICATION_ID, notification.build(),
					android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
		} else {
			startForeground(NOTIFICATION_ID, notification.build());
		}
	}

	private PendingIntent getOpenDawnPendingIntent()
	{
		Intent openDawn = new Intent(this, AlarmReceiver.class);
		openDawn.setAction(AlarmReceiver.ACTION_START_ALARM);

		int flags = PendingIntent.FLAG_UPDATE_CURRENT;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			flags |= PendingIntent.FLAG_IMMUTABLE; // Obligatoire sur Android 12+
		}

		return PendingIntent.getBroadcast(
				getApplicationContext(),
				0,
				openDawn,
				flags);
	}
	
	private AlarmManager getAlarmManager()
	{
		return (AlarmManager) getSystemService(ALARM_SERVICE);
	}
	
	private SharedPreferences getPreferences()
	{
		return getApplicationContext().getSharedPreferences("main", MODE_PRIVATE);
	}
	
	private void cancel()
	{
		getAlarmManager().cancel(
				getOpenDawnPendingIntent());
	}
	
	public static boolean shouldFire(SharedPreferences pref, int dayOfWeek)
	{
		String day;
		//TODO: dictionary?
		switch (dayOfWeek) {
		case Calendar.MONDAY:
			day = "mondays";
			break;
		case Calendar.TUESDAY:
			day = "tuesdays";
			break;
		case Calendar.WEDNESDAY:
			day = "wednesdays";
			break;
		case Calendar.THURSDAY:
			day = "thursdays";
			break;
		case Calendar.FRIDAY:
			day = "fridays";
			break;
		case Calendar.SATURDAY:
			day = "saturdays";
			break;
		case Calendar.SUNDAY:
			day = "sundays";
			break;
		default:
			day = "NON_EXISTING_WEEKDAY";
			break;
		}
		return pref.getBoolean(day, false);
	}
	
	private boolean shouldFire(int dayOfWeek)
	{
		return shouldFire(getPreferences(), dayOfWeek);
	}
	
	private Calendar getNextAlarmTime()
	{
		SharedPreferences pref = getPreferences();
		Calendar nextAlarmTime = Calendar.getInstance();
		nextAlarmTime.set(Calendar.HOUR_OF_DAY, pref.getInt("dawn_start_hour", 8));
		nextAlarmTime.set(Calendar.MINUTE, pref.getInt("dawn_start_minute", 0));
		nextAlarmTime.set(Calendar.SECOND, 0);
		if(nextAlarmTime.getTimeInMillis() < System.currentTimeMillis() + TOLERANCE_MILLIS)
		{
			nextAlarmTime.add(Calendar.DAY_OF_YEAR, 1);
			//TODO: check if enough?
		}
		int ndays = 0;
		while(!shouldFire(nextAlarmTime.get(Calendar.DAY_OF_WEEK)))
		{
			nextAlarmTime.add(Calendar.DAY_OF_YEAR, 1);
			ndays++;
			if(ndays >= 7) // No weekday is ticked.
			{
				return null;
			}
		}
		
		return nextAlarmTime;
	}
	
	private void set(Calendar nextAlarmTime) {
		AlarmManager alarmManager = getAlarmManager();
		PendingIntent openDawnIntent = getOpenDawnPendingIntent();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
					nextAlarmTime.getTimeInMillis(), openDawnIntent);
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			alarmManager.setExact(AlarmManager.RTC_WAKEUP,
					nextAlarmTime.getTimeInMillis(), openDawnIntent);
		} else {
			alarmManager.set(AlarmManager.RTC_WAKEUP,
					nextAlarmTime.getTimeInMillis(), openDawnIntent);
		}
	}

	private void set(AlarmManager alarmManager, int type, long triggerAtMillis, PendingIntent operation)
	{
		// API 19 changed set() behaviour and added setExact
		// https://developer.android.com/reference/android/app/AlarmManager.html#set(int, long, android.app.PendingIntent)
		// Using setExact if it exists, otherwise fall back to set.
		try {
			Method setExact = AlarmManager.class.getDeclaredMethod(
					"setExact", int.class, long.class, PendingIntent.class);
			setExact.invoke(alarmManager, type,
					triggerAtMillis, operation);
		} catch (NoSuchMethodException e) {
			alarmManager.set(type,
					triggerAtMillis, operation);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (IllegalArgumentException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
	
	private String getPlural(long n, String name)
	{
		String plural;
		if(n != 1)
		{
			plural = "s";
		}
		else
		{
			plural = "";
		}
		return String.format("%d %s%s", n, name, plural);
	}

	private String buildMessage(String majorTime, String minorTime)
	{
		return String.format(
				"Fake Dawn starting in %s and %s.",
				majorTime,
				minorTime);
	}
	
	private String nextAlarmMessage(Calendar nextAlarmTime)
	{
		long elapsed = nextAlarmTime.getTimeInMillis() - System.currentTimeMillis();
		long dayMillis = 1000*60*60*24;
		long elapsedDays = elapsed / dayMillis;
		elapsed -= elapsedDays * dayMillis;
		long hourMillis = 1000*60*60;
		long elapsedHours = elapsed / hourMillis;
		String message;
		if (elapsedDays > 0)
		{
			message = buildMessage(
					getPlural(elapsedDays, "day"),
					getPlural(elapsedHours, "hour"));
		}
		else
		{
			elapsed -= elapsedHours * hourMillis;
			long minuteMillis = 1000*60;
			long elapsedMinutes = elapsed / minuteMillis;
			if (elapsedHours > 0)
			{
				message = buildMessage(
						getPlural(elapsedHours, "hour"),
						getPlural(elapsedMinutes, "minute"));
			}
			else
			{
				elapsed -= elapsedMinutes * minuteMillis;
				long secondMillis = 1000;
				long elapsedSeconds = elapsed / secondMillis;
				message = buildMessage(
						getPlural(elapsedMinutes, "minute"),
						getPlural(elapsedSeconds, "second"));
			}
		}
		return message;
	}
}
