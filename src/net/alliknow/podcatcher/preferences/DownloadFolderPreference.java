/** Copyright 2012 Kevin Hausmann
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

package net.alliknow.podcatcher.preferences;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.preference.Preference;
import android.util.AttributeSet;

import net.alliknow.podcatcher.SelectFileActivity;
import net.alliknow.podcatcher.SelectFileActivity.SelectionMode;

import java.io.File;

/**
 * @author Kevin Hausmann
 */
public class DownloadFolderPreference extends Preference {

    public static final int REQUEST_CODE = 99;

    private File downloadFolder;

    public DownloadFolderPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        onSetInitialValue(false, null);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        downloadFolder = new File(getPersistedString(Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS)
                .getAbsolutePath()));
    }

    @Override
    protected void onClick() {
        Intent selectFolderIntent = new Intent(getContext(), SelectFileActivity.class);
        selectFolderIntent
                .putExtra(SelectFileActivity.SELECTION_MODE_KEY, SelectionMode.FOLDER);

        ((Activity) getContext()).startActivityForResult(selectFolderIntent, REQUEST_CODE);
    }

    @Override
    public CharSequence getSummary() {
        return downloadFolder.getAbsolutePath();
    }

    public void update(File newFolder) {
        this.downloadFolder = newFolder;

        persistString(newFolder.getAbsolutePath());
    }
}
