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

import net.alliknow.podcatcher.listeners.OnLoadSuggestionListener;
import net.alliknow.podcatcher.model.tasks.remote.LoadSuggestionsTask;
import net.alliknow.podcatcher.model.types.Podcast;
import net.alliknow.podcatcher.model.types.Progress;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Defines some util methods for tests.
 */
@SuppressWarnings("javadoc")
public class Utils {

    public static XmlPullParser getParser(Podcast podcast) {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);

            XmlPullParser parser = factory.newPullParser();
            parser.setInput(podcast.getUrl().openStream(), null);

            return parser;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static List<Podcast> getExamplePodcasts() throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);
        final List<Podcast> examples = new ArrayList<Podcast>();

        LoadSuggestionsTask task = new LoadSuggestionsTask(new OnLoadSuggestionListener() {

            @Override
            public void onSuggestionsLoaded(List<Podcast> suggestions) {
                for (Podcast podcast : suggestions)
                    examples.add(podcast);

                signal.countDown();
            }

            @Override
            public void onSuggestionsLoadProgress(Progress progress) {
            }

            @Override
            public void onSuggestionsLoadFailed() {
                signal.countDown();
            }
        });

        task.execute((Void) null);
        signal.await();

        return examples;
    }
}
