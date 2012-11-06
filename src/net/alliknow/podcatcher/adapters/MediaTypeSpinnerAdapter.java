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
package net.alliknow.podcatcher.adapters;

import net.alliknow.podcatcher.types.MediaType;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Adapter for the media type spinner in the suggestion dialog.
 * 
 * @author Kevin Hausmann
 */
public class MediaTypeSpinnerAdapter extends PodcatcherBaseAdapter {
	
	/**
	 * Create new adapter
	 * @param context The current context
	 */
	public MediaTypeSpinnerAdapter(Context context) {
		super(context);
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView result = (TextView) findReturnView(convertView, parent, android.R.layout.simple_spinner_item);
		
		result.setText(getItem(position).toString());
		return result;
	}
	
	@Override
	public long getItemId(int position) {
		return getItem(position).hashCode();
	}
	
	@Override
	public Object getItem(int position) {
		if (position == 0) return "All";
		else return MediaType.values()[position - 1];
	}
	
	@Override
	public int getCount() {
		return MediaType.values().length + 1;
	}
}
