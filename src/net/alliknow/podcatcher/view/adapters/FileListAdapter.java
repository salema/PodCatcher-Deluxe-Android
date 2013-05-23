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

/**
 * 
 */
public class FileListAdapter extends PodcatcherBaseListAdapter {

    private File currentPath;

    public FileListAdapter(Context context, File path) {
        super(context);

        this.currentPath = path;
    }

    @Override
    public int getCount() {
        return currentPath.listFiles().length;
    }

    @Override
    public Object getItem(int position) {
        return currentPath.listFiles()[position];
    }

    @Override
    public long getItemId(int position) {
        return currentPath.listFiles()[position].hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the return view (possibly recycle a used one)
        View fileView = findReturnView(convertView, parent, R.layout.file_list_item);
        // Get the file object to represent
        final File file = (File) getItem(position);

        ((ImageView) fileView.findViewById(R.id.icon)).setImageResource(
                file.isDirectory() ? R.drawable.ic_file_folder : R.drawable.ic_file);

        ((TextView) fileView.findViewById(R.id.file_name)).setText(file.getName());

        return fileView;
    }
}
