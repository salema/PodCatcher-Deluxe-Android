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

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Abstract base for spinner adapters.
 * The default implementation will use simple text views and string
 * resources named <code>item.toString().toLowerCase()</code>.
 *
 * @author Kevin Hausmann
 */
public abstract class PodcatcherBaseSpinnerAdapter extends PodcatcherBaseAdapter {

	/** We need to know our package name to retrieve identifiers */
	protected String packageName;
	
	/**
	 * Create the adapter.
	 * @param context Context we live in.
	 */
	public PodcatcherBaseSpinnerAdapter(Context context) {
		super(context);
		
		this.packageName = context.getPackageName();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView result = (TextView) findReturnView(convertView, parent, android.R.layout.simple_spinner_item);
		
		result.setText(getStringIdentifier(position));
		return result;
	}

	@Override
	public long getItemId(int position) {
		return getItem(position).hashCode();
	}
	
	protected int getStringIdentifier(int position) {
		return resources.getIdentifier(getItem(position).toString().toLowerCase(), "string", packageName);
	}
}
