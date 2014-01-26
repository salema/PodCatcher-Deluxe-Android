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

import static net.alliknow.podcatcher.SelectFileActivity.SELECTION_MODE_KEY;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import net.alliknow.podcatcher.SelectFileActivity.SelectionMode;
import net.alliknow.podcatcher.listeners.OnStorePodcastListListener;
import net.alliknow.podcatcher.model.PodcastManager;
import net.alliknow.podcatcher.model.tasks.StorePodcastListTask;
import net.alliknow.podcatcher.model.types.Podcast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity that exports a list of selected podcasts to an OPML file.
 */
public class ExportOpmlActivity extends BaseActivity implements OnStorePodcastListListener {

    @Override
    @TargetApi(Build.VERSION_CODES.KITKAT)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If list is there, export podcasts at given positions
        if (getIntent().getIntegerArrayListExtra(PODCAST_POSITION_LIST_KEY) != null) {
            // Only do this on initial creation in order to avoid multiple
            // folder selection dialogs
            if (savedInstanceState == null) {
                // For Android version 4.4 and newer, we use the storage access
                // framework for file creation
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    final Intent createIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    createIntent.addCategory(Intent.CATEGORY_OPENABLE);
                    createIntent.setType("text/opml");
                    createIntent.putExtra(Intent.EXTRA_TITLE, PodcastManager.OPML_FILENAME);

                    startActivityForResult(createIntent, 42);
                }
                // We are on a device with Android < 4.4, use the app's own file
                // selection dialog (local files only)
                else {
                    final Intent selectFolderIntent = new Intent(this, SelectFileActivity.class);
                    selectFolderIntent.putExtra(SELECTION_MODE_KEY, SelectionMode.FOLDER);

                    startActivityForResult(selectFolderIntent, 42);
                }
            }
        } else
            // Nothing to do
            finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (RESULT_OK == resultCode) {
            // We need the result to contain an URI
            if (result != null && result.getData() != null) {
                // Get the list of positions to export
                final List<Integer> positions = getIntent()
                        .getIntegerArrayListExtra(PODCAST_POSITION_LIST_KEY);

                // If list is there, export podcasts at given positions
                if (positions != null) {
                    final List<Podcast> podcasts = podcastManager.getPodcastList();
                    final List<Podcast> exports = new ArrayList<Podcast>();

                    for (Integer position : positions)
                        exports.add(podcasts.get(position));

                    // Create and configure the export task
                    final StorePodcastListTask exportTask = new StorePodcastListTask(this, this);
                    exportTask.setCustomLocation(result.getData());

                    exportTask.execute(exports);
                }
            } else
                onPodcastListStoreFailed(null, null, null);
        }

        // Make sure we finish here
        finish();
    }

    @Override
    public void onPodcastListStored(List<Podcast> podcastList, Uri outputFile) {
        final File exportFile = new File(outputFile.getPath());

        // This runs if the export was to a local file
        if (exportFile.exists())
            showToast(getString(R.string.opml_export_success_at, exportFile.getAbsolutePath()),
                    Toast.LENGTH_LONG);
        else
            showToast(getString(R.string.opml_export_success),
                    Toast.LENGTH_LONG);
    }

    @Override
    public void onPodcastListStoreFailed(List<Podcast> podcastList, Uri outputFile,
            Exception exception) {
        showToast(getString(R.string.opml_export_failed));
    }
}
