package com.aware.plugin.tracescollector;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.aware.Aware;

public class Settings extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String STATUS_PLUGIN_TRACESCOLLECTOR = "status_plugin_tracescollector";
    public static final String BT_UUID_KEY = "bt_uuid_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        syncSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        syncSettings();
    }

    private void syncSettings() {
        //Make sure to load the latest values
        CheckBoxPreference status = (CheckBoxPreference) findPreference(STATUS_PLUGIN_TRACESCOLLECTOR);
        status.setChecked(Aware.getSetting(getApplicationContext(), STATUS_PLUGIN_TRACESCOLLECTOR).equals("true"));

        Preference uuid = (Preference) findPreference(BT_UUID_KEY);
        if( Aware.getSetting(getApplicationContext(), BT_UUID_KEY).length() == 0 ) {
            Aware.setSetting(getApplicationContext(), BT_UUID_KEY, "cfa37877-e7a1-41a8-9673-2b0844b5868f");
        }
        uuid.setSummary(Aware.getSetting(getApplicationContext(), BT_UUID_KEY));

        //...
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference setting = findPreference(key);

        if( setting.getKey().toString().equals(STATUS_PLUGIN_TRACESCOLLECTOR) ) {
            boolean is_active = sharedPreferences.getBoolean(key, false);
            Aware.setSetting(this, key, is_active);
            if( is_active ) {
                Aware.startPlugin(getApplicationContext(), getPackageName());
            } else {
                Aware.stopPlugin(getApplicationContext(), getPackageName());
            }
        }

        if( setting.getKey().equals(BT_UUID_KEY)) {
            setting.setSummary(sharedPreferences.getString(key, "cfa37877-e7a1-41a8-9673-2b0844b5868f"));
            Aware.setSetting(getApplicationContext(), key, sharedPreferences.getString(key, "cfa37877-e7a1-41a8-9673-2b0844b5868f"));
        }

        Intent apply = new Intent(Aware.ACTION_AWARE_REFRESH);
        sendBroadcast(apply);
    }
}
