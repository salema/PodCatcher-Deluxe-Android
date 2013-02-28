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
import android.widget.TextView;

/**
 * Abstract base for spinner adapters. The default implementation will use
 * simple text views and string resources named ("keyed")
 * <code>item.toString().toLowerCase()</code>. (This will fail if these
 * resources do not exist!)
 */
public abstract class PodcatcherBaseSpinnerAdapter extends PodcatcherBaseAdapter {

    /**
     * Create the adapter.
     * 
     * @param context Context we live in.
     */
    public PodcatcherBaseSpinnerAdapter(Context context) {
        super(context);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView result = (TextView) findReturnView(convertView, parent,
                android.R.layout.simple_spinner_item);

        // Get the resource string as the item's toString().toLowerCase() result
        // plus "R.string." and set as label
        int stringId = getStringIdentifier(getItem(position));
        result.setText(resources.getString(stringId));

        return result;
    }

    @Override
    public long getItemId(int position) {
        // Since there are only enums behind this, it is actually okay to simply
        // return the position...
        return position;
    }
}
