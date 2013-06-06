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

package net.alliknow.podcatcher;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.os.Environment;

import net.alliknow.podcatcher.listeners.OnSelectFileListener;
import net.alliknow.podcatcher.view.fragments.SelectFileFragment;

import java.io.File;

/**
 * Non-UI activity to select files and folders. Use the intent and constants
 * defined here to configure its behavior. Start the activity with
 * {@link Activity#startActivityForResult(Intent, int)} to be alerted on
 * selection.
 */
public class SelectFileActivity extends BaseActivity implements OnSelectFileListener,
        OnCancelListener {

    /** The tag we identify our file selection fragment with */
    private static final String SELECT_FILE_FRAGMENT_TAG = "select_file";

    /** The key to store initial path under in intent */
    public static final String INITIAL_PATH_KEY = "initial_path";
    /** The key to store result path under in intent */
    public static final String RESULT_PATH_KEY = "result_path";

    /** The key to store wanted selection mode under in intent */
    public static final String SELECTION_MODE_KEY = "file_selection_mode";
    /** The current selection mode */
    private SelectionMode selectionMode = SelectionMode.FILE;

    /** The selection mode options */
    public static enum SelectionMode {
        /** File selection */
        FILE,

        /** Folder selection */
        FOLDER
    }

    /** The fragment containing the select file UI */
    private SelectFileFragment selectFileFragment;

    @Override
    protected void onStart() {
        super.onStart();

        // Try to find existing fragment
        selectFileFragment = (SelectFileFragment) getFragmentManager().findFragmentByTag(
                SELECT_FILE_FRAGMENT_TAG);

        // No fragment found, create it
        if (selectFileFragment == null) {
            selectFileFragment = new SelectFileFragment();
            selectFileFragment.setStyle(DialogFragment.STYLE_NORMAL,
                    android.R.style.Theme_Holo_Light_Dialog);
        }

        // Use getIntent() to configure selection mode
        final SelectionMode modeFromIntent =
                (SelectionMode) getIntent().getSerializableExtra(SELECTION_MODE_KEY);
        if (modeFromIntent != null)
            selectionMode = modeFromIntent;
        // Set the selection mode
        selectFileFragment.setSelectionMode(selectionMode);

        // Use getIntent() to configure initial path
        final String initialPathString = getIntent().getStringExtra(INITIAL_PATH_KEY);
        // Set the initial path
        if (initialPathString != null && new File(initialPathString).exists())
            selectFileFragment.setPath(new File(initialPathString));
        else {
            // No path set, use default
            final File podcastDir = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS);
            podcastDir.mkdirs();

            selectFileFragment.setPath(podcastDir);
        }

        // Show the fragment
        selectFileFragment.show(getFragmentManager(), SELECT_FILE_FRAGMENT_TAG);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void onFileSelected(File selectedFile) {
        selectFileFragment.dismiss();

        Intent result = new Intent();
        result.putExtra(RESULT_PATH_KEY, selectedFile.getAbsolutePath());

        setResult(RESULT_OK, result);
        finish();
    }

    @Override
    public void onDirectoryChanged(File path) {
        getIntent().putExtra(INITIAL_PATH_KEY, path.getAbsolutePath());
    }

    @Override
    public void onAccessDenied(File path) {
        showToast(getString(R.string.file_select_access_denied));
    }
}
