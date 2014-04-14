/** Copyright 2012-2014 Kevin Hausmann
 *
 * This file is part of PodCatcher Deluxe.
 *
 * PodCatcher Deluxe is free software: you can redistribute it 
 * and/or modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * PodCatcher Deluxe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PodCatcher Deluxe. If not, see <http://www.gnu.org/licenses/>.
 */

package net.alliknow.podcatcher.view.fragments;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.BaseAdapter;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.SettingsActivity;

/**
 * Fragment for settings.
 */
public class SettingsFragment extends PreferenceFragment implements
        OnSharedPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
        // Register this fragment to listen to preference changes
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (SettingsActivity.KEY_THEME_COLOR.equals(key)) {
            // Update the color preview widget since this is not done
            // automatically by the color picker preference
            final View previewColorView = getView().findViewById(R.id.color_preview);

            if (previewColorView != null)
                previewColorView.setBackgroundColor(sharedPreferences.getInt(key,
                        getActivity().getResources().getColor(R.color.theme_dark)));
        } else if (SettingsActivity.KEY_SYNC_ACTIVE.equals(key)
                || SettingsActivity.KEY_SYNC_RECEIVE.equals(key)) {

            // Update the summaries to reflect new settings
            ((BaseAdapter) getPreferenceScreen().getRootAdapter()).notifyDataSetChanged();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unregister this fragment
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .unregisterOnSharedPreferenceChangeListener(this);
    }
}
