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

package net.alliknow.podcatcher.model.tasks.remote;

import android.content.Context;
import android.util.Log;

import net.alliknow.podcatcher.listeners.OnLoadSuggestionListener;
import net.alliknow.podcatcher.model.tags.JSON;
import net.alliknow.podcatcher.model.types.Genre;
import net.alliknow.podcatcher.model.types.Language;
import net.alliknow.podcatcher.model.types.MediaType;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.model.types.Progress;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * A task that loads and reads suggested podcasts.
 */
public class LoadSuggestionsTask extends LoadRemoteFileTask<Void, List<Podcast>> {

    /** Call back */
    private OnLoadSuggestionListener listener;
    /** The task's context */
    private final Context context;

    /** The file encoding */
    private static final String SUGGESTIONS_ENCODING = "utf8";
    /** The online resource to find suggestions */
    private static final String SOURCE = "https://raw.github.com/salema/PodCatcher-Deluxe/master/suggestions.json";

    /** Flag to indicate the max age that would trigger re-load. */
    private int maxAge = 60 * 24 * 7;

    /**
     * Create new task.
     * 
     * @param context The context the task is carried out in.
     * @param listener Callback to be alerted on progress and completion.
     */
    public LoadSuggestionsTask(Context context, OnLoadSuggestionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    @Override
    protected List<Podcast> doInBackground(Void... params) {
        List<Podcast> result = new ArrayList<Podcast>();
        byte[] suggestions = null;

        // 1. Load the file from the cache or the Internet
        try {
            publishProgress(Progress.CONNECT);
            // 1.1 This the simple case where we have the local version and
            // it is fresh enough. Use that one.
            if (isCachedLocally() && getCachedLogoAge() <= maxAge)
                suggestions = restoreSuggestionsFromFileCache();
            // 1.2 If that is not the case, we need to go over the air.
            else {
                // We store a cached version ourselves
                // useCaches = false;
                suggestions = loadFile(new URL(SOURCE));

                storeSuggestionsToFileCache(suggestions);
            }
        } catch (Exception e) {
            // Use cached version even if it is stale
            if (isCachedLocally())
                try {
                    suggestions = restoreSuggestionsFromFileCache();
                } catch (IOException e1) {
                    cancel(true);
                    return null; // Nothing more we could do here
                }
            else {
                Log.w(getClass().getSimpleName(), "Load failed for podcast suggestions file", e);

                cancel(true);
                return null;
            }
        }

        // 2. Parse the result
        try {
            // 2.1 Get result as a document
            publishProgress(Progress.PARSE);
            JSONObject completeJson = new JSONObject(new String(suggestions, SUGGESTIONS_ENCODING));
            if (isCancelled())
                return null;

            // 2.2 Add all featured podcasts
            addSuggestionsFromJsonArray(completeJson.getJSONArray(JSON.FEATURED), result);
            if (isCancelled())
                return null;

            // 2.3 Add all suggestions
            addSuggestionsFromJsonArray(completeJson.getJSONArray(JSON.SUGGESTION), result);
            if (isCancelled())
                return null;

            // 2.4 Sort the result
            Collections.sort(result);
            publishProgress(Progress.DONE);
        } catch (Exception e) {
            Log.w(getClass().getSimpleName(), "Parse failed for podcast suggestions ", e);

            cancel(true);
            return null;
        }

        return result;
    }

    @Override
    protected void onProgressUpdate(Progress... progress) {
        if (listener != null)
            listener.onSuggestionsLoadProgress(progress[0]);
        else if (listener == null)
            Log.w(getClass().getSimpleName(),
                    "Suggestions progress update, but no listener attached");
    }

    @Override
    protected void onPostExecute(List<Podcast> suggestions) {
        // Suggestions loaded successfully
        if (listener != null)
            listener.onSuggestionsLoaded(suggestions);
        else
            Log.w(getClass().getSimpleName(), "Suggestions loaded, but no listener attached");
    }

    @Override
    protected void onCancelled(List<Podcast> suggestions) {
        // Suggestions failed to load
        if (listener != null)
            listener.onSuggestionsLoadFailed();
        else
            Log.w(getClass().getSimpleName(),
                    "Suggestions failed to load, but no listener attached");
    }

    /**
     * Add all podcast suggestions in given array to the list.
     * 
     * @param array JSON array to scan.
     * @param list List to add suggestions to.
     */
    private void addSuggestionsFromJsonArray(JSONArray array, List<Podcast> list) {
        for (int index = 0; index < array.length(); index++) {
            JSONObject object;

            try {
                object = array.getJSONObject(index);
            } catch (JSONException e) {
                continue; // If an index fails, try the next one...
            }

            Podcast suggestion = createSuggestion(object);
            if (suggestion != null)
                list.add(suggestion);
        }
    }

    /**
     * Create a podcast suggestion for the given JSON object and set its
     * properties.
     * 
     * @param json The JSON object to work on.
     * @return The podcast suggestion or <code>null</code> if any problem
     *         occurs.
     */
    private Podcast createSuggestion(JSONObject json) {
        Podcast suggestion = null;

        try {
            suggestion = new Podcast(json.getString(JSON.TITLE), new URL(json.getString(JSON.URL)));
            suggestion.setDescription(json.getString(JSON.DESCRIPTION).trim());
            suggestion.setLanguage(Language.valueOf(json.getString(JSON.LANGUAGE)
                    .toUpperCase(Locale.US).trim()));
            suggestion.setMediaType(MediaType.valueOf(json.getString(JSON.TYPE)
                    .toUpperCase(Locale.US).trim()));
            suggestion.setGenre(Genre.valueOf(json.getString(JSON.CATEGORY).toUpperCase(Locale.US)
                    .trim()));
        } catch (JSONException e) {
            Log.w(getClass().getSimpleName(), "JSON parsing failed for: " + suggestion, e);
            return null;
        } catch (IllegalArgumentException e) {
            Log.w(getClass().getSimpleName(), "Enum value missing for: " + suggestion, e);
            return null;
        } catch (MalformedURLException e) {
            Log.w(getClass().getSimpleName(), "Bad URL for: " + suggestion, e);
            return null;
        }

        return suggestion;
    }

    private File getSuggestionsCacheFile() {
        // Create the complete path leading to where we expect the cached file
        return new File(context.getCacheDir(), "suggestions.json");
    }

    private boolean isCachedLocally() {
        return getSuggestionsCacheFile().exists();
    }

    private int getCachedLogoAge() {
        if (isCachedLocally())
            return (int) ((new Date().getTime() - getSuggestionsCacheFile().lastModified())
            / (60 * 1000)); // Calculate to minutes
        else
            return -1;
    }

    private byte[] restoreSuggestionsFromFileCache() throws IOException {
        final File cachedFile = getSuggestionsCacheFile();
        final byte[] result = new byte[(int) cachedFile.length()];

        FileInputStream input = null;
        try {
            input = new FileInputStream(cachedFile);
            input.read(result);
        } catch (IOException e) {
            throw e;
        } finally {
            try {
                input.close();
            } catch (Exception e) {
                // Nothing more we could do here
            }
        }

        return result;
    }

    private void storeSuggestionsToFileCache(byte[] suggestions) {
        context.getCacheDir().mkdirs();

        FileOutputStream out = null;
        // If this fails, we have no cached version, but that's okay
        try {
            out = new FileOutputStream(getSuggestionsCacheFile());
            out.write(suggestions);
            out.flush();
        } catch (IOException e) {
            // pass
        } finally {
            try {
                out.close();
            } catch (Exception e) {
                // Nothing more we could do here
            }
        }
    }
}
