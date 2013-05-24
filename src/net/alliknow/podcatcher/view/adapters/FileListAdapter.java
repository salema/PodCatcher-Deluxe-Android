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

package net.alliknow.podcatcher.view.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.alliknow.podcatcher.R;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;

/**
 * The file list adapter to provide the data for the list in the file/folder
 * selection dialog.
 */
public class FileListAdapter extends PodcatcherBaseListAdapter {

    /** The current path items */
    private final File[] files;
    /** The default file filter to apply */
    private static final FileFilter filter = new FileFilter() {

        @Override
        public boolean accept(File pathname) {
            return !pathname.isHidden();
        }
    };

    /**
     * Create new adapter. Sub-files of given path will be sorted. Hidden files
     * are excluded.
     * 
     * @param context Context we live in.
     * @param path Path to represent children of.
     */
    public FileListAdapter(Context context, File path) {
        super(context);

        this.files = path.listFiles(filter);
        Arrays.sort(files);
    }

    @Override
    public int getCount() {
        return files.length;
    }

    @Override
    public Object getItem(int position) {
        return files[position];
    }

    @Override
    public long getItemId(int position) {
        return files[position].hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the return view (possibly recycle a used one)
        final View fileView = findReturnView(convertView, parent, R.layout.file_list_item);
        // Get the file object to represent
        final File file = (File) getItem(position);

        // Set list item color background
        setBackgroundColorForPosition(fileView, position);
        // Set the icon
        ((ImageView) fileView.findViewById(R.id.icon)).setImageResource(
                file.isDirectory() ? R.drawable.ic_file_folder : R.drawable.ic_file);
        // Set the file name as text
        ((TextView) fileView.findViewById(R.id.file_name)).setText(file.getName());

        return fileView;
    }
}
