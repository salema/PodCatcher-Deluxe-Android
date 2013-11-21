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

package net.alliknow.podcatcher.model.test;

import android.content.Context;
import android.util.Log;

import net.alliknow.podcatcher.listeners.OnLoadSuggestionListener;
import net.alliknow.podcatcher.model.tasks.remote.LoadSuggestionsTask;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.model.types.Progress;
import net.alliknow.podcatcher.model.types.Suggestion;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 * Defines some util methods for tests.
 */
@SuppressWarnings("javadoc")
public class Utils {

    public static final String TEST_STATUS = "Teststatus";

    public static XmlPullParser getParser(Podcast podcast) {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);

            XmlPullParser parser = factory.newPullParser();
            parser.setInput(podcast.getUrl().openStream(), null);

            return parser;
        } catch (Exception e) {
            Log.e(TEST_STATUS, e.getMessage(), e);
        }

        return null;
    }

    public static List<Podcast> getExamplePodcasts(Context context) {
        return getExamplePodcasts(context, 0);
    }

    /**
     * @param context Context to load in.
     * @param limit Limit the result to the given number of podcasts randomly
     *            chosen.
     * @return The list of podcast examples;
     */
    public static List<Podcast> getExamplePodcasts(Context context, final int limit) {
        final CountDownLatch signal = new CountDownLatch(1);
        final List<Podcast> examples = new ArrayList<Podcast>();

        LoadSuggestionsTask task = new LoadSuggestionsTask(context, new OnLoadSuggestionListener() {

            @Override
            public void onSuggestionsLoaded(List<Suggestion> suggestions) {
                Log.d(TEST_STATUS, "Load example podcasts task complete");
                if (limit <= 0)
                    for (Podcast podcast : suggestions)
                        examples.add(podcast);
                else {
                    int count = 0;

                    while (count++ < limit && !suggestions.isEmpty())
                        examples.add(suggestions.remove(new Random().nextInt(suggestions.size())));
                }

                signal.countDown();
            }

            @Override
            public void onSuggestionsLoadProgress(Progress progress) {
                // Log.d(TEST_STATUS, "Load example podcasts task progress: " +
                // progress);
            }

            @Override
            public void onSuggestionsLoadFailed() {
                Log.d(TEST_STATUS, "Load example podcasts task failed");
                signal.countDown();
            }
        });

        task.execute((Void) null);
        Log.d(TEST_STATUS, "Load example podcasts task started");

        // Wait for the task to finish...
        try {
            signal.await();
        } catch (InterruptedException e) {
            Log.e(TEST_STATUS, e.getMessage(), e);
        }

        return examples;
    }
}
