package net.alliknow.podcatcher;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class ContextMenuEpisodeDialog extends Dialog {

    public static final int ITEM_CANCEL = -1;
    public static final int ITEM_PLAY = 0;
    public static final int ITEM_REVERSE_MARKER = 1;
    public static final int ITEM_ADD_TO_PLAYLIST = 2;
    public static final int ITEM_DOWNLOAD = 3;

    ListView list;

    private boolean isNew;
    private int result = ITEM_CANCEL;

    public ContextMenuEpisodeDialog(Context context, boolean isNew) {
        super(context);
        setContentView(R.layout.context_menu_dialog);
        list = (ListView) findViewById(R.id.list);

        this.isNew = isNew;

        String[] strings = (String[]) formList().toArray();
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getContext(),
                android.R.layout.simple_list_item_1,
                strings
        );
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                result = position;
                dismiss();
            }
        });
    }

    private List<String> formList() {
        List<String> list = new ArrayList<String>(4);
        list.add(getContext().getString(R.string.context_play));
        list.add(getContext().getString(isNew ? R.string.context_mark_old : R.string.context_mark_new));
        list.add(getContext().getString(R.string.context_add_playlist));
        list.add(getContext().getString(R.string.context_download));
        return list;
    }

    public int getResult() {
        return result;
    }
}
