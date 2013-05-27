/** Copyright 2012, 2013 Kevin Hausmann
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

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.widget.BaseAdapter;

import net.alliknow.podcatcher.R;
import net.alliknow.podcatcher.SettingsActivity;
import net.alliknow.podcatcher.preferences.DownloadFolderPreference;

import java.io.File;

/**
 * Fragment for settings.
 */
public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }

    public void updateDownloadFolder(File newFolder) {
        DownloadFolderPreference folderPreference =
                (DownloadFolderPreference) findPreference(SettingsActivity.DOWNLOAD_FOLDER_KEY);

        if (folderPreference != null) {
            folderPreference.update(newFolder);
            ((BaseAdapter) getPreferenceScreen().getRootAdapter()).notifyDataSetChanged();
        }
    }
}
