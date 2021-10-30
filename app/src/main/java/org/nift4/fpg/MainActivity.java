package org.nift4.fpg;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.collection.ArrayMap;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.io.SuFile;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {
    public static class SettingsFragment extends PreferenceFragmentCompat {
        MainActivity a;
        File keycharfile;
        File targetfile = new File("/data/system/devices/keychars/uinput-fpc.kcm");
        String baseHeader = "#\n" +
                "# This file has been generated by LineageFpg.\n" +
                "# Do not change manually!\n#\n" +
                "type SPECIAL_FUNCTION\n";
        ArrayMap<String, Integer> codes = new ArrayMap<>();

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            keycharfile = new File(requireContext().getFilesDir(), "uinput-fpc.kcm");
            a = (MainActivity) getActivity();
            setPreferencesFromResource(R.xml.main, rootKey);
            codes.put("", R.string.action_default);
            codes.put("POWER", R.string.action_power);
            codes.put("SYSRQ", R.string.action_screenshot);
            codes.put("BACK", R.string.action_back);
            codes.put("HOME", R.string.action_home);
            codes.put("MENU", R.string.action_menu);
            codes.put("VOICE_ASSIST", R.string.action_assistant);
            codes.put("APP_SWITCH", R.string.action_recents);
            codes.put("NOTIFICATION", R.string.action_notification_bar);
            codes.put("SYSTEM_NAVIGATION_DOWN", R.string.action_notification_bar_alt);
            codes.put("EXPLORER", R.string.action_browser);
            codes.put("AVR_INPUT", R.string.action_nothing);
            codes.put("CALENDAR", R.string.action_calendar);
            codes.put("CALL", R.string.action_call);
            codes.put("CONTACTS", R.string.action_contacts);
            codes.put("ENVELOPE", R.string.action_envelope);
            codes.put("MUSIC", R.string.action_music);
            codes.put("VOLUME_UP", R.string.action_volume_up);
            codes.put("VOLUME_MUTE", R.string.action_volume_mute);
            codes.put("VOLUME_DOWN", R.string.action_volume_down);
            codes.put("MUTE", R.string.action_mute);
            codes.put("BRIGHTNESS_UP", R.string.action_brightness_up);
            codes.put("BRIGHTNESS_DOWN", R.string.action_brightness_down);
            codes.put("MEDIA_AUDIO_TRACK", R.string.action_media_audio_track);
            codes.put("MEDIA_FAST_FORWARD", R.string.action_media_fast_forward);
            codes.put("MEDIA_NEXT", R.string.action_media_next);
            codes.put("MEDIA_PAUSE", R.string.action_media_pause);
            codes.put("MEDIA_PLAY", R.string.action_media_play);
            codes.put("MEDIA_PLAY_PAUSE", R.string.action_media_play_pause);
            codes.put("MEDIA_PREVIOUS", R.string.action_media_previous);
            codes.put("MEDIA_REWIND", R.string.action_media_rewind);
            codes.put("MEDIA_SKIP_BACKWARD", R.string.action_media_skip_backward);
            codes.put("MEDIA_SKIP_FORWARD", R.string.action_media_skip_forward);
            codes.put("MEDIA_STEP_BACKWARD", R.string.action_media_step_backward);
            codes.put("MEDIA_STEP_FORWARD", R.string.action_media_step_forward);
            codes.put("MEDIA_STOP", R.string.action_media_stop);
            Log.d("FPG", "mkdir ./files: " + Objects.requireNonNull(keycharfile.getParentFile()).mkdir());
            String result;
            try {
                if (!keycharfile.exists()) {
                    FileWriter writer = new FileWriter(keycharfile);
                    writer.write(baseHeader);
                    writer.close();
                }
                StringBuilder builder = new StringBuilder();
                Scanner scanner = new Scanner(keycharfile);
                while (scanner.hasNextLine())
                    builder.append("\n").append(scanner.nextLine());
                result = builder.toString();
                Log.d("FPG", "File parser, raw data: " + result);
            } catch (IOException e) {
                e.printStackTrace();
                new AlertDialog.Builder(a)
                        .setCancelable(false)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.uerror)
                        .setNegativeButton(R.string.quit, (dialogInterface, i) -> a.finish())
                        .show();
                return;
            }
            if (!SuFile.open(targetfile.toURI()).exists())
                new AlertDialog.Builder(a)
                        .setCancelable(false)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.setup)
                        .setNegativeButton(R.string.quit, (dialogInterface, i) -> a.finish())
                        .setPositiveButton(R.string.ok, ((dialogInterface, i) -> {
                            assert targetfile != null;
                            assert targetfile.getParentFile() != null;
                            Shell.su("mkdir -p " + targetfile.getParentFile().getAbsolutePath()).exec();
                            Shell.su("touch " + targetfile.getAbsolutePath()).exec();
                            Shell.su("chmod 644 " + targetfile.getAbsolutePath()).exec();
                            Shell.su("chcon u:object_r:system_data_file:s0 " + targetfile.getAbsolutePath()).exec();
                            a.finish();
                        }))
                        .show();
            else {
                HashMap<Integer, String> gestures = new HashMap<>();
                for (String line : result.split("\n"))
                    if (!(line.startsWith("#") || line.startsWith("type") || line.equals("")) && line.startsWith("map")) {
                        String[] splt = line.replace("map key ", "").split(" ");
                        gestures.put(Integer.valueOf(splt[0]), splt[1]);
                    }
                gestures.forEach((key, code) -> Log.d("FPG", "key: " + key + " code: '" + code + "'"));
                String[] codesL = new String[codes.size()];
                int j = 0;
                for (int i : codes.values().toArray(new Integer[0])) {
                    codesL[j++] = getString(i);
                }
                for (int id : new int[]{616, 617, 620, 621}) {
                    String gcode = keycodeToGcode(id);
                    assert gcode != null;
                    Preference pref = Objects.requireNonNull(getPreferenceFromGcode(gcode));
                    pref.setOnPreferenceClickListener((v) -> {
                        final String oldVal = gestures.get(id);
                        new AlertDialog.Builder(a)
                            .setTitle(getStringFromGcode(gcode))
                            .setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
                                if (oldVal != null)
                                    gestures.put(id, oldVal);
                                else
                                    gestures.remove(id);
                            })
                            .setPositiveButton(R.string.ok, ((dialogInterface, i) -> {
                                FileWriter w = null;
                                pref.setSummary(R.string.action_default);
                                gestures.forEach((key, code) -> {
                                    if (codes.containsKey(code) && codes.get(code) != null)
                                        pref.setSummary(Objects.requireNonNull(codes.get(code)));
                                });
                                try {
                                    w = new FileWriter(keycharfile);
                                    w.write(baseHeader);
                                    FileWriter finalW = w;
                                    gestures.forEach((key, value) -> {
                                        try {
                                            finalW.write("map key " + key + " " + value + "\n");
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    });
                                    finalW.flush();
                                    Shell.su("cat " + keycharfile.getAbsolutePath() + " > " + targetfile.getAbsolutePath()).exec();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    Toast.makeText(a, R.string.uerror, Toast.LENGTH_LONG).show();
                                } finally {
                                    if (w != null) {
                                        try {
                                            w.close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                            Toast.makeText(a, R.string.uerror, Toast.LENGTH_LONG).show();
                                        }
                                    }
                                }
                            }))
                            .setSingleChoiceItems(codesL, codes.indexOfKey(gestures.get(id)) > -1 ? codes.indexOfKey(gestures.get(id)) : codes.indexOfKey(""), (dialog, itemId) -> {
                                if (!codes.keyAt(itemId).equals(""))
                                    gestures.put(id, codes.keyAt(itemId));
                                else
                                    gestures.remove(id);
                            })
                            .show();
                        return true;});
                    pref.setSummary(R.string.action_default);
                }
                gestures.forEach((key, code) -> {
                    if (codes.containsKey(code) && codes.get(code) != null)
                        Objects.requireNonNull(getPreferenceFromGcode(Objects.requireNonNull(keycodeToGcode(key)))).setSummary(Objects.requireNonNull(codes.get(code)));
                });
                ((Preference)Objects.requireNonNull(findPreference("apply_changes"))).setOnPreferenceClickListener((v) -> {Shell.su("setprop ctl.restart fps_hal").submit();return true;});
                ((Preference)Objects.requireNonNull(findPreference("perma_enable"))).setOnPreferenceClickListener((v) -> {
                    startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://github.com/n4archive/G5-Resources/releases/tag/fpg-plus-navbar")));
                    return true;
                });
            }
        }

        private Preference getPreferenceFromGcode(String gcode) {
            switch (gcode) {
                case "gesture_tap":
                    return findPreference("tap");
                case "gesture_hold":
                    return findPreference("hold");
                case "gesture_swipe_left":
                    return findPreference("swipe_left");
                case "gesture_swipe_right":
                    return findPreference("swipe_right");
                default:
                    return null;
            }
        }

        public int getStringFromGcode(String gcode) {
            switch (gcode) {
                case "gesture_tap":
                    return R.string.gesture_tap;
                case "gesture_hold":
                    return R.string.gesture_hold;
                case "gesture_swipe_left":
                    return R.string.gesture_swipe_left;
                case "gesture_swipe_right":
                    return R.string.gesture_swipe_right;
                default:
                    return -1;
            }
        }

        private String keycodeToGcode(int keycode) {
            switch (keycode) {
                case 616:
                    return "gesture_tap";
                case 617:
                    return "gesture_hold";
                case 620:
                    return Build.DEVICE.equals("montana") ? "gesture_swipe_right" : "gesture_swipe_left";
                case 621:
                    return Build.DEVICE.equals("montana") ? "gesture_swipe_left" : "gesture_swipe_right";
                default:
                    return null;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frag, new SettingsFragment())
                .commit();
    }

}