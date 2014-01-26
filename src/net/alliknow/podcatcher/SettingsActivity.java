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

package net.alliknow.podcatcher;

import android.content.Intent;
import android.os.Bundle;

import net.alliknow.podcatcher.preferences.DownloadFolderPreference;
import net.alliknow.podcatcher.view.fragments.SettingsFragment;

import java.io.File;

/**
 * Update settings activity.
 */
public class SettingsActivity extends BaseActivity {

    /** The flag for the first run dialog */
    public static final String KEY_FIRST_RUN = "first_run";

    /** The select all podcast on start-up preference key */
    public static final String KEY_SELECT_ALL_ON_START = "select_all_on_startup";
    /** The theme color preference key */
    public static final String KEY_THEME_COLOR = "theme_color";
    /** The episode list width preference key */
    public static final String KEY_WIDE_EPISODE_LIST = "wide_episode_list";
    /** The preference key for the auto download flag */
    public static final String AUTO_DOWNLOAD_KEY = "auto_download";
    /** The preference key for the auto delete flag */
    public static final String AUTO_DELETE_KEY = "auto_delete";
    /** The key for the download folder preference */
    public static final String DOWNLOAD_FOLDER_KEY = "download_folder";

    /** The settings fragment we display */
    private SettingsFragment settingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.preferences);

        // Create the fragment to show
        this.settingsFragment = new SettingsFragment();
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, settingsFragment)
                .commit();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        // This is used to fetch the result from the select folder dialog. The
        // result is forwarded to the preference object via the fragment.
        if (resultCode == RESULT_OK && requestCode == DownloadFolderPreference.REQUEST_CODE)
            if (settingsFragment != null && result != null) {
                final File folder = new File(result.getData().getPath());

                // Only accept folder we can write to.
                if (folder.canWrite())
                    settingsFragment.updateDownloadFolder(folder);
                else
                    showToast(getString(R.string.file_select_access_denied));
            }
    }
}
