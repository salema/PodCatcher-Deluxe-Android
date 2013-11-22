package net.alliknow.podcatcher;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import net.alliknow.podcatcher.model.EpisodeManager;
import net.alliknow.podcatcher.model.types.Episode;

import java.util.ArrayList;
import java.util.List;

public class ContextMenuEpisodeDialog extends Dialog implements AdapterView.OnItemClickListener {

    public static final int ITEM_CANCEL = -1;
    public static final int ITEM_PLAY = 0;
    public static final int ITEM_REVERSE_MARKER = 1;
    public static final int ITEM_ADD_TO_PLAYLIST = 2;
    public static final int ITEM_DOWNLOAD = 3;

    ListView list;

    private boolean isNew;
    private Episode episode;
    private int result = ITEM_CANCEL;

    /** The episode manager handle */
    private EpisodeManager episodeManager;

    public ContextMenuEpisodeDialog(Context context, Episode episode) {
        super(context);
        setContentView(R.layout.context_menu_dialog);
        list = (ListView) findViewById(R.id.list);

        this.episodeManager = EpisodeManager.getInstance();
        this.isNew = !episodeManager.isOld(episode);
        this.episode = episode;

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                getContext(),
                android.R.layout.simple_list_item_1,
                formList()
        );
        list.setAdapter(adapter);
        list.setOnItemClickListener(this);
    }

    private List<String> formList() {
        List<String> list = new ArrayList<String>(4);
        list.add(getContext().getString(R.string.context_play));
        list.add(getContext().getString(isNew ? R.string.context_mark_old : R.string.context_mark_new));
        list.add(getContext().getString(R.string.context_playlist_add));
        list.add(getContext().getString(R.string.context_download));
        return list;
    }

    public int getResult() {
        return result;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        result = position;

        switch (result) {

            case ITEM_PLAY:
                if (getContext() instanceof PodcastActivity) {
                    PodcastActivity activity = (PodcastActivity) getContext();
                    activity.onToggleLoad();
                }

                break;

            case ITEM_REVERSE_MARKER:
                episodeManager.setState(episode, !isNew);
                view.invalidate();
                break;

            case ITEM_DOWNLOAD:
                episodeManager.download(episode);
                break;

//            case R.id.episode_remove_contextmenuitem:
//                final DeleteDownloadsConfirmationFragment confirmationDialog =
//                        new DeleteDownloadsConfirmationFragment();
//                // Create bundle to make dialog aware of selection count
//                final Bundle args = new Bundle();
//                args.putInt(EPISODE_COUNT_KEY, deletesTriggered);
//                confirmationDialog.setArguments(args);
//                // Set the callback
//                confirmationDialog.setListener(new OnDeleteDownloadsConfirmationListener() {
//
//                    @Override
//                    public void onConfirmDeletion() {
//                        // Go delete the downloads
//                        for (int position = 0; position < fragment.getListAdapter().getCount(); position++)
//                            if (checkedItems.get(position))
//                                episodeManager.deleteDownload(
//                                        (Episode) fragment.getListAdapter().getItem(position));
//
//                        // Action picked, so close the CAB
//                        mode.finish();
//                    }
//
//                    @Override
//                    public void onCancelDeletion() {
//                        // Nothing to do here
//                    }
//                });
//                // Finally show the dialog
//                confirmationDialog.show(fragment.getFragmentManager(), TAG);
//
//                return true;

            case ITEM_ADD_TO_PLAYLIST:
                if (episodeManager.isInPlaylist(episode))
                    episodeManager.removeFromPlaylist(episode);
                else
                    episodeManager.appendToPlaylist(episode);
                break;

//            case R.id.episode_select_all_contextmenuitem:
//                // Disable expensive UI updates
//                updateUi = false;
//                for (int index = 0; index < fragment.getListAdapter().getCount(); index++)
//                    fragment.getListView().setItemChecked(index, true);
//
//                // Re-enable UI updates
//                updateUi = true;
//                update(mode);
//                return true;
//            default:
//                return false;
        }

        dismiss();
    }
}
