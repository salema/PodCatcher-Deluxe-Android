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

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;

import net.alliknow.podcatcher.SelectFileActivity.SelectionMode;
import net.alliknow.podcatcher.listeners.OnLoadPodcastListListener;
import net.alliknow.podcatcher.model.tasks.LoadPodcastListTask;
import net.alliknow.podcatcher.model.types.Podcast;

import java.io.File;
import java.util.List;

/**
 * Activity that imports podcasts from an OPML file.
 */
public class ImportOpmlActivity extends BaseActivity implements OnLoadPodcastListListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Only do this initial creation to avoid multiple file selection
        // dialogs
        if (savedInstanceState == null) {
            Intent selectFolderIntent = new Intent(this, SelectFileActivity.class);
            // Set file deialog mode
            selectFolderIntent
                    .putExtra(SelectFileActivity.SELECTION_MODE_KEY, SelectionMode.FILE);
            // Set initial folder selection
            final File downloadDir = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            downloadDir.mkdirs();

            selectFolderIntent
                    .putExtra(SelectFileActivity.INITIAL_PATH_KEY, downloadDir.getAbsolutePath());

            startActivityForResult(selectFolderIntent, 1);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (RESULT_OK == resultCode && data != null) {
            // Get file handle
            File opmlFile = new File(data.getStringExtra(SelectFileActivity.RESULT_PATH_KEY));

            // Run the import task
            LoadPodcastListTask importTask = new LoadPodcastListTask(this, this);
            importTask.setCustomLocation(opmlFile);

            importTask.execute();
        }
        // Nothing more to do
        else
            finish();
    }

    @Override
    public void onPodcastListLoaded(List<Podcast> podcastList) {
        // Iff the list is empty, the import went wrong
        if (podcastList.isEmpty())
            showToast(getString(R.string.opml_import_failed));
        else
            // Add all podcasts to the list
            for (Podcast podcast : podcastList)
                podcastManager.addPodcast(podcast);

        // End the activity
        finish();
    }
}
