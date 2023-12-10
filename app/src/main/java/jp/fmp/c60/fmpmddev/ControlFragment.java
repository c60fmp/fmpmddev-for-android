package jp.fmp.c60.fmpmddev;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;


class ArrayAdapterText<T> extends ArrayAdapter<T> {

    private int position = -1;

    private static final int UNSELECTEXTCOLOR = Color.BLACK;


    public ArrayAdapterText(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }


    public void setPosition(int position) {
        this.position = position;
        notifyDataSetChanged();
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = super.getView(position, convertView, parent);

        TextView tv = v.findViewById( android.R.id.text1 );
        if(position == this.position) {
            if(getContext() != null) {
                tv.setTextColor(ContextCompat.getColor(getContext(), R.color.colorPrimary));
            }
        } else {
            tv.setTextColor(ContextCompat.getColor(getContext(), R.color.colorBackground));
        }
        return v;
    }
}


class ListViewPos {
    int pos;
    int top;

    public ListViewPos(int pos, int top) {
        this.pos = pos;
        this.top = top;
    }

    public void setPos(int pos)
    {
        this.pos = pos;
    }
    public int getPos()
    {
        return this.pos;
    }

    public void setTop(int top)
    {
        this.top = top;
    }
    public int getTop()
    {
        return this.top;
    }
}


public class ControlFragment extends Fragment implements View.OnClickListener, ListView.OnItemClickListener, SeekBar.OnSeekBarChangeListener, OnSubscribedListener {

    // Control Fragment Tag
    public static final String CONTROL_FRAGMENT_TAG         = "ControlFragment";

    private static final String KEY_LOCAL_BROWSEDIRECTORY   = "localBrowseDirectory";

    private static final String KEY_LOCAL_SUBSCRIBECHILDREN = "localSubscribeChildren";

    private static final String KEY_LOCAL_PLAYMEDIAID       = "localPlayMediaID";


    // Control Fragment から呼び出す Callback
    public interface ControlFragmentListener {
        void subscribeCache(Bundle bundle);

        void onMusicPlay(Bundle bundle);
        void onMusicStop();
        void onMusicPause();
        void onMusicSeek(Bundle bundle);
        void onMusicSkipToNext();
        void onMusicSkipToPrevious();
    }

    // ListView の範囲設定の微調整
    public static final int LISTVIEW_DISP_DIGIT = 10;


    // MainActivity への Callback
    ControlFragmentListener listener;

    // アダプター
    private ArrayAdapterText<String> adapter = null;

    // ダイアログで保持する必要のある Bundle
    Bundle bundle = new Bundle();

    // ディレクトリ毎の表示位置
    private final HashMap<String, ListViewPos> listViewPosmap = new HashMap<>();


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            // Instantiate the SettingDialogListener so we can send events to the host
            listener = (ControlFragmentListener)context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(getActivity().toString()
                    + " must implement ControlFragmentListener");
        }
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        bundle.putString(KEY_LOCAL_BROWSEDIRECTORY, "");

        View view = inflater.inflate(R.layout.fragment_control, container, false);

        ListView listView = view.findViewById(R.id.listview_control);
        listView.setOnItemClickListener(this);

        // view.findViewById(R.id.pause_button).setOnClickListener(this);
        // view.findViewById(R.id.stop_button).setOnClickListener(this);
        view.findViewById(R.id.backward_button).setOnClickListener(this);
        view.findViewById(R.id.forward_button).setOnClickListener(this);
        ((SeekBar) view.findViewById(R.id.seekBar)).setOnSeekBarChangeListener(this);

        adapter = new ArrayAdapterText<>(view.getContext(), android.R.layout.simple_list_item_1);

        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
        setTopItem(bundle.getString(KEY_LOCAL_BROWSEDIRECTORY), bundle.getString(KEY_LOCAL_PLAYMEDIAID), suppressAssign(bundle.getSerializable(KEY_LOCAL_SUBSCRIBECHILDREN)));
    }


    // View を更新
    public void onSubscribed() {
        saveListViewPos();

        Bundle bundle = getArguments();
        String directory = bundle.getString(Common.KEY_ACTIVITY_TO_CONTROL_BROWSEDIRECTORY);
        List<MediaBrowserCompat.MediaItem> children = suppressAssign(bundle.getSerializable(Common.KEY_ACTIVITY_TO_CONTROL_SUBSCRIBECHILDREN));

        if(!directory.equals(this.bundle.getString(KEY_LOCAL_BROWSEDIRECTORY))) {
            adapter.clear();
            if (!directory.equals(bundle.getString(Common.KEY_ACTIVITY_TO_CONTROL_ROOTDIRECTORY))) {
                adapter.add(getString(R.string.privious_directory));
            }

            if (directory.equals("")) {
                directory = bundle.getString(Common.KEY_ACTIVITY_TO_CONTROL_ROOTDIRECTORY);
            }

            if (children != null) {
                for (MediaBrowserCompat.MediaItem child : children) {
                    if (child.isBrowsable()) {
                        adapter.add((String)child.getDescription().getTitle() + File.separator);
                    } else {
                        adapter.add(DrivePath.getFilename(child.getDescription().getMediaId()));
                    }
                }
            }

            ListView listView = getView().findViewById(R.id.listview_control);
            listView.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        }

        // ListView の表示範囲を設定
        setTopItem(directory, getArguments().getString(Common.KEY_ACTIVITY_TO_CONTROL_PLAYMEDIAID), children);

        TextView textView = getView().findViewById(R.id.directoryname);
        textView.setText(DrivePath.getDisplayPath(directory));

        this.bundle.putString(KEY_LOCAL_BROWSEDIRECTORY, directory);
        this.bundle.putSerializable(KEY_LOCAL_SUBSCRIBECHILDREN, (Serializable)children);
    }


    // 曲を更新
    public void updateMusic() {
        TextView textView = getView().findViewById(R.id.title_textview);
        if(textView != null) {
            textView.setText(getArguments().getString(Common.KEY_ACTIVITY_TO_CONTROL_PLAYTITLE));
        }

        SeekBar seekBar = getView().findViewById(R.id.seekBar);
        seekBar.setMax(getArguments().getInt(Common.KEY_ACTIVITY_TO_CONTROL_MUSICLENGTH));
        setTime(R.id.totaltime_textview, getArguments().getInt(Common.KEY_ACTIVITY_TO_CONTROL_MUSICLENGTH));

        bundle.putString(KEY_LOCAL_PLAYMEDIAID, getArguments().getString(Common.KEY_ACTIVITY_TO_CONTROL_PLAYMEDIAID));
    }


    // 再生位置を更新
    public void updateProgress() {
        SeekBar seekBar = getView().findViewById(R.id.seekBar);
        seekBar.setProgress(getArguments().getInt(Common.KEY_ACTIVITY_TO_CONTROL_MUSICPROGRESS));
        setTime(R.id.elapsedtime_textview, getArguments().getInt(Common.KEY_ACTIVITY_TO_CONTROL_MUSICPROGRESS));
    }


    // TextView に時間を表示
    private void setTime(int id, int value) {
        TextView textView = getView().findViewById(id);
        if(textView != null) {
            textView.setText(formatTime(value));
        }
    }


    // msec → (h:)mm:ssに変換
    private String formatTime(long msec) {
        String result;
        long sec = msec / 1000;

        if(msec <= 60*60*1000) {
            result =  String.format("%02d:%02d", sec / 60, sec % 60);
        } else {
            result =  String.format("%d:%02d:%02d", sec / (60 * 60), (sec / 60) % 60, sec % 60);
        }
        return result;
    }


    // 再生状態を更新
    public void updateStatus() {
        if (getArguments().getInt(Common.KEY_ACTIVITY_TO_CONTROL_PLAYSTATUS) == PlaybackStateCompat.STATE_PLAYING) {
            ImageButton button_pause = getView().findViewById(R.id.pause_button);
            button_pause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onMusicPause();
                }
            });
            button_pause.setImageResource(R.drawable.ic_baseline_pause_33);
        } else {
            ImageButton button_pause = getView().findViewById(R.id.pause_button);
            button_pause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onMusicPause();
                }
            });
            button_pause.setImageResource(R.drawable.ic_baseline_play_arrow_33);
        }
    }


    // 現在のListViewの位置を保存
    public void saveListViewPos() {
        String directory = bundle.getString(KEY_LOCAL_BROWSEDIRECTORY);
        if(directory.isEmpty()) return;

        ListView listView = getView().findViewById(R.id.listview_control);

        int p = listView.getFirstVisiblePosition();
        int y = 0;
        View v = listView.getChildAt(0);
        if(v != null) {
            y = v.getTop();
        }

        listViewPosmap.put(directory, new ListViewPos(p, y));
    }


    // ListView の表示位置を設定
    private void setTopItem(String directory, String playMediaId, List<MediaBrowserCompat.MediaItem> children) {
        if(directory == null || playMediaId == null || children == null) {
            return;
        }

        int pListViewPos;
        ListView listView = getView().findViewById(R.id.listview_control);

        if(!directory.equals(bundle.getString(KEY_LOCAL_BROWSEDIRECTORY)) && listViewPosmap.containsKey(directory)) {
            // フォルダ変更、かつ listViewPosmap 登録済の場合、その値を基準とする
            ListViewPos p = listViewPosmap.get(directory);

            pListViewPos = p.getPos();
            listView.setSelectionFromTop(pListViewPos, p.getTop());

        } else {
            // フォルダ変更なし、または listViewPosmap 未登録の場合、listView の先頭行を基準とする
            pListViewPos = listView.getFirstVisiblePosition();
        }

        // 演奏中の曲番号を解除(＝フォントの色を戻す)
        adapter.setPosition(-1);

        int pos = getMediaItemPos(playMediaId, children);
        if(pos < 0 || !children.get(pos).isPlayable()) {
            return;
        }

        // 演奏中の曲番号を設定(→フォントの色を変える)
        if(directory.equals(getArguments().getString(Common.KEY_ACTIVITY_TO_CONTROL_ROOTDIRECTORY))) {
            adapter.setPosition(pos);
        } else {
            adapter.setPosition(pos + 1);
        }

        // 表示範囲を設定
        if(listView.getFirstVisiblePosition() > pos) {
            // 演奏中の曲が表示範囲より上なら、演奏中の曲を一番上に設定
            listView.setSelectionFromTop(pos + 1, LISTVIEW_DISP_DIGIT);
            //@ listViewPosmap.put(bundle.getString(DIRECTORY), new ListViewPos(pos + 1, 10));

        } else {
            ListAdapter listAdapter = listView.getAdapter();
            View listItem = listAdapter.getView(0, null, listView);
            listItem.measure(
                    View.MeasureSpec.makeMeasureSpec(listView.getMeasuredWidth(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            int height = listItem.getMeasuredHeight();
            int totalHeight = listView.getHeight();

            if(pos + 1 > pListViewPos + totalHeight / height - 1) {
                // 演奏中の曲が表示範囲より下なら、演奏中の曲を一番下に設定
                int p = pos + 1 - (totalHeight / height - 1);
                listView.setSelectionFromTop(p, -LISTVIEW_DISP_DIGIT);
                //@ listViewPosmap.put(bundle.getString(DIRECTORY), new ListViewPos(p, -10));
            }
        }
    }


    // 演奏中の曲が mediaItems の何番目か調べる
    private int getMediaItemPos(String filename, List<MediaBrowserCompat.MediaItem> mediaItems) {
        int result = -1;
        if(mediaItems == null) {
            return result;
        }

        for(int i = 0; i < mediaItems.size(); i++) {
            if (mediaItems.get(i).isPlayable() && mediaItems.get(i).getDescription().getMediaId().equals(filename)) {
                result = i;
                break;
            }
        }

        return result;
    }


    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.pause_button) {
/*
            listener.onMusicPause();

        } else if(v.getId() == R.id.stop_button) {
            listener.onMusicStop();
*/
        } else if(v.getId() == R.id.backward_button) {
            listener.onMusicSkipToPrevious();

        } else if(v.getId() == R.id.forward_button) {
            listener.onMusicSkipToNext();
        }
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String directory = getArguments().getString(Common.KEY_ACTIVITY_TO_CONTROL_BROWSEDIRECTORY);

        if(position == 0 && directory.length() > getArguments().getString(Common.KEY_ACTIVITY_TO_CONTROL_ROOTDIRECTORY).length()) {
            // 親ディレクトリに戻る
            directory = DrivePath.getParentDirectory(directory);

            // ダイアログとビューを更新
            Bundle lBundle = new Bundle();
            lBundle.putString(Common.KEY_CONTROL_TO_ACTIVITY_BROWSEDIRECTORY, directory);
            listener.subscribeCache(lBundle);

        } else {
            List<MediaBrowserCompat.MediaItem> mediaItems = suppressAssign(getArguments().getSerializable(Common.KEY_ACTIVITY_TO_CONTROL_SUBSCRIBECHILDREN));

                    MediaBrowserCompat.MediaItem item;
            if(directory.equals(getArguments().getString(Common.KEY_ACTIVITY_TO_CONTROL_ROOTDIRECTORY))) {
                item = mediaItems.get(position);
            } else {
                item = mediaItems.get(position - 1);
            }
            directory = item.getDescription().getMediaId();

            if(item.isBrowsable()) {
                // ディレクトリの場合はその中へ移動
                Bundle lBundle = new Bundle();
                lBundle.putString(Common.KEY_CONTROL_TO_ACTIVITY_BROWSEDIRECTORY, directory);
                listener.subscribeCache(lBundle);

            } else {
                // ファイルが確定
                Bundle lBundle = new Bundle();
                lBundle.putString(Common.KEY_CONTROL_TO_ACTIVITY_PLAYMEDIAID, directory);
                listener.onMusicPlay(lBundle);
            }
        }
    }


    @Override
    // SeekBar 変更
    public void onStopTrackingTouch(SeekBar seekBar) {
        Bundle lBundle = new Bundle();
        lBundle.putInt(Common.KEY_CONTROL_TO_ACTIVITY_SEEKPOSITION, seekBar.getProgress());
        listener.onMusicSeek(lBundle);
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }


    @SuppressWarnings("unchecked")
    private <T> T suppressAssign(Serializable value) {
        return (T)value;
    }
}
