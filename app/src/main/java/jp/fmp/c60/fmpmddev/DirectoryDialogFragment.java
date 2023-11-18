package jp.fmp.c60.fmpmddev;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.media.MediaBrowserCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.io.File;
import java.io.Serializable;
import java.util.List;


public class DirectoryDialogFragment extends DialogFragment implements ListView.OnItemClickListener, OnSubscribedListener {

    // DirectoryDialog Fragment Tag
    public static final String DIRECTORYDIALOG_FRAGMENT_TAG = "DirectoryDialogFragment";

    private static final String KEY_LOCAL_ROOTDIRECTORY     = "localRootDirectory";

    private static final String KEY_LOCAL_EXTENSION         =  "localExtension";

    private static final String KEY_LOCAL_DIRECTORY         = "localDirectory";

    private static final String KEY_LOCAL_SUBSCRIBECHILDREN = "localSubscribeChildren";

    // Directory Dialog Fragment から呼び出す Callback
    public interface DirectoryDialogFragmentListener {
        void onDialogPositiveClick(Bundle bundle);
    }

    // SettingDialogFragment への Callback
    DirectoryDialogFragmentListener listener;

    // アダプター
    private ArrayAdapter<String> adapter = null;

    // ダイアログで保持する必要のある Bundle
    Bundle bundle = new Bundle();


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            // Instantiate the SettingDialogListener so we can send events to the host
            listener = (DirectoryDialogFragmentListener)getTargetFragment();
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(getActivity().toString()
                    + " must implement DirectoryDialogFragmentListener");
        }
    }


    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        bundle.putString(KEY_LOCAL_ROOTDIRECTORY, getArguments().getString(Common.KEY_SETTING_TO_DIRECTORY_ROOTDIRECTORY));
        bundle.putString(KEY_LOCAL_EXTENSION, getArguments().getString(Common.KEY_SETTING_TO_DIRECTORY_PCMEXT));
        bundle.putString(KEY_LOCAL_DIRECTORY, getArguments().getString(Common.KEY_SETTING_TO_DIRECTORY_PCMEXTDIRECTORY));

        // ダイアログのメインビュー設定、及び、ListView取得
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View directoryView = inflater.inflate(R.layout.fragment_directory, null);
        ListView listView = directoryView.findViewById(R.id.listview_directorydialog);
        listView.setOnItemClickListener(this);

        adapter = new ArrayAdapter<>(DirectoryDialogFragment.this.getActivity(), android.R.layout.simple_list_item_1);
        listView.setAdapter(adapter);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
            // .setTitle(R.string.directory_dialog_title)

            .setView(directoryView)

            .setPositiveButton(R.string.directory_dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // OK
                    Bundle lBundle = new Bundle();
                    lBundle.putString(Common.KEY_DIRECTORY_TO_SETTING_PCMEXT, bundle.getString(KEY_LOCAL_EXTENSION));
                    lBundle.putString(Common.KEY_DIRECTORY_TO_SETTING_PCMEXTDIRECTORY, bundle.getString(KEY_LOCAL_DIRECTORY));
                    listener.onDialogPositiveClick(lBundle);
                }
            })
            .setNegativeButton(R.string.directory_dialog_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // Cancel
                }
            });

        // Create the AlertDialog object and return it
        return builder.create();
    }


    @Override
    public void onResume() {
        super.onResume();
        Bundle lBundle = new Bundle();
        lBundle.putString(Common.KEY_DIRECTORY_TO_ACTIVITY_BROWSEDIRECTORY, bundle.getString(KEY_LOCAL_DIRECTORY));
        ((MainActivity)getActivity()).subscribecached(lBundle);
    }


    // View を更新
    @Override
    public void onSubscribed() {
        Bundle lBundle = getArguments();
        String rootDirectory = bundle.getString(KEY_LOCAL_ROOTDIRECTORY);
        String parentId = lBundle.getString(Common.KEY_ACTIVITY_TO_DIRECTORY_BROWSEDIRECTORY);
        bundle.putSerializable(KEY_LOCAL_SUBSCRIBECHILDREN, lBundle.getSerializable(Common.KEY_ACTIVITY_TO_DIRECTORY_SUBSCRIBECHILDREN));

        adapter.clear();

        if (!parentId.equals(rootDirectory)) {
            adapter.add(getString(R.string.privious_directory));
        }

        String directory = this.bundle.getString(KEY_LOCAL_DIRECTORY);
        if(directory.equals("")) {
            directory = File.separator;
        }

        List<MediaBrowserCompat.MediaItem> children = suppressAssign(bundle.getSerializable(KEY_LOCAL_SUBSCRIBECHILDREN));
        for(MediaBrowserCompat.MediaItem child : children) {
            if(child.isBrowsable()) {
                adapter.add(child.getDescription().getTitle() + File.separator);
            }
        }

        TextView textView = getDialog().findViewById(R.id.directoryname_directorydialog);
        textView.setText(DrivePath.getDisplayPath(directory).replace("|", "/"));

        adapter.notifyDataSetChanged();
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String rootDirectory = bundle.getString(KEY_LOCAL_ROOTDIRECTORY);
        String directory = bundle.getString(KEY_LOCAL_DIRECTORY);

        if(position == 0 && !directory.equals(rootDirectory)) {
            // 親ディレクトリに戻る
            directory = DrivePath.getParentDirectory(directory);

            bundle.putString(KEY_LOCAL_DIRECTORY, directory);

            // ダイアログとビューを更新
            Bundle lBundle = new Bundle();
            lBundle.putString(Common.KEY_DIRECTORY_TO_ACTIVITY_BROWSEDIRECTORY, directory);
            ((MainActivity)getActivity()).subscribecached(lBundle);

        } else {
            List<MediaBrowserCompat.MediaItem> children = suppressAssign(bundle.getSerializable(KEY_LOCAL_SUBSCRIBECHILDREN));
            MediaBrowserCompat.MediaItem item;
            if(directory.equals(rootDirectory)) {
                item = children.get(position);
            } else {
                item = children.get(position - 1);
            }
            directory = item.getDescription().getMediaId();

            if(item.isBrowsable()) {
                // ディレクトリの場合はその中へ移動
                bundle.putString(KEY_LOCAL_DIRECTORY, directory);

                // ダイアログとビューを更新
                Bundle lBundle = new Bundle();
                lBundle.putString(Common.KEY_DIRECTORY_TO_ACTIVITY_BROWSEDIRECTORY, directory);
                ((MainActivity)getActivity()).subscribecached(lBundle);
            }
        }
    }


    @SuppressWarnings("unchecked")
    private <T> T suppressAssign(Serializable value) {
        return (T)value;
    }
}
