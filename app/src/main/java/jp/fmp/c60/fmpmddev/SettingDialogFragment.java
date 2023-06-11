package jp.fmp.c60.fmpmddev;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;


class ArrayAdapterExtDir extends ArrayAdapter<ExtDirItem> {
    public ArrayAdapterExtDir(Context context, int resource) {
        super(context, resource);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View rowView = convertView;
        if (rowView == null) {
            // 現在の View の取得
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService
                    (Context.LAYOUT_INFLATER_SERVICE);
            rowView = inflater.inflate(R.layout.list_item, parent, false);
        }

        ExtDirItem extDirItem = this.getItem(position);

        TextView v;
        v = rowView.findViewById(R.id.extension);
        v.setText(extDirItem.getExtension());

        v = rowView.findViewById(R.id.directory);
        v.setText(extDirItem.getDirectory());

        return rowView;
    }
}


public class SettingDialogFragment extends DialogFragment implements AdapterView.OnItemClickListener, DirectoryDialogFragment.DirectoryDialogFragmentListener {

    // SettingDialog Fragment Tag
    public static final String SETTINGDIALOG_FRAGMENT_TAG   = "SettingDialogFragment";

    private static final String KEY_LOCAL_ROOTDIRECTORY     = "localRootDirectory";

    private static final String KEY_LOCAL_PCMEXTDIRECTORY   = "localPCMExtDirectory";


    // Setting Dialog Fragment から呼び出す Callback
    public interface SettingDialogFragmentListener {
        void onDialogPositiveClick(Bundle bundle);
    }


    // MainActivity への Callback
    SettingDialogFragmentListener listener;

    // アダプター
    private ArrayAdapterExtDir adapter = null;

    // ダイアログで保持する必要のある Bundle
    private final Bundle bundle = new Bundle();

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            // Instantiate the SettingDialogListener so we can send events to the host
            listener = (SettingDialogFragmentListener)context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(getActivity().toString()
                    + " must implement SettingDialogFragmentListener");
        }
    }


    @Override
    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        bundle.putString(KEY_LOCAL_ROOTDIRECTORY, getArguments().getString(Common.KEY_ACTIVITY_TO_SETTING_ROOTDIRECTORY));
        bundle.putSerializable(KEY_LOCAL_PCMEXTDIRECTORY, getArguments().getSerializable(Common.KEY_ACTIVITY_TO_SETTING_PCMEXTDIRECTORY));

        // ダイアログのメインビュー設定、及び、ListView 取得
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View settingView = inflater.inflate(R.layout.fragment_setting, null);
        ListView listView = settingView.findViewById(R.id.listview_setting);
        listView.setOnItemClickListener(this);

        adapter = new ArrayAdapterExtDir(SettingDialogFragment.this.getActivity(), R.layout.list_item);
        listView.setAdapter(adapter);

        // ListView に表示するデータ
        ExtDirItem[] extDirItem = (ExtDirItem[])(bundle.getSerializable(KEY_LOCAL_PCMEXTDIRECTORY));

        // ダイアログ表示用のデータ作成
        ExtDirItem[] extDirItem2 = new ExtDirItem[extDirItem.length + 1];
        extDirItem2[0] = new ExtDirItem(getString(R.string.root_directory_name), bundle.getString(KEY_LOCAL_ROOTDIRECTORY).replace("|", "/"));
        for(int i = 0; i < extDirItem.length; i++) {
            extDirItem2[i + 1] = new ExtDirItem(extDirItem[i].getExtension(), extDirItem[i].getDirectory().replace("|", "/"));
        }

        adapter.addAll(extDirItem2);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
            .setTitle(R.string.setting_dialog_title)

            .setView(settingView)

            .setPositiveButton(R.string.setting_dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // OK
                    Bundle lBundle = new Bundle();
                    lBundle.putString(Common.KEY_SETTING_TO_ACTIVITY_ROOTDIRECTORY, bundle.getString(KEY_LOCAL_ROOTDIRECTORY));
                    lBundle.putSerializable(Common.KEY_SETTING_TO_ACTIVITY_PCMEXTDIRECTORY, bundle.getSerializable(KEY_LOCAL_PCMEXTDIRECTORY));
                    listener.onDialogPositiveClick(lBundle);
                }
            })
            .setNegativeButton(R.string.setting_dialog_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // Cancel
                }
            });

        // Create the AlertDialog object and return it
        return builder.create();
    }


    // 行をクリックしたときのイベント
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        ExtDirItem[] extDirItem = (ExtDirItem[])(bundle.getSerializable(KEY_LOCAL_PCMEXTDIRECTORY));
        if(position > extDirItem.length) {
            return;
        }

        String extension;
        String directory;

        if(position == 0) {
            // root directory
            extension = getString(R.string.root_directory_name);
            directory = bundle.getString(KEY_LOCAL_ROOTDIRECTORY);
        } else {
            extension = extDirItem[position - 1].getExtension();
            directory = extDirItem[position - 1].getDirectory();
        }

        Bundle lBundle = new Bundle();
        lBundle.putString(Common.KEY_SETTING_TO_DIRECTORY_PCMEXT, extension);
        lBundle.putString(Common.KEY_SETTING_TO_DIRECTORY_PCMEXTDIRECTORY, directory);

        //　ダイアログを開く
        DialogFragment directoryDialogFragment = new DirectoryDialogFragment();
        directoryDialogFragment.setArguments(lBundle);

        directoryDialogFragment.setTargetFragment(SettingDialogFragment.this, 0);
        directoryDialogFragment.show(getActivity().getSupportFragmentManager(), DirectoryDialogFragment.DIRECTORYDIALOG_FRAGMENT_TAG);
    }


    public void onDialogPositiveClick(Bundle bundle) {
        String extension = bundle.getString(Common.KEY_DIRECTORY_TO_SETTING_PCMEXT);
        String directory = bundle.getString(Common.KEY_DIRECTORY_TO_SETTING_PCMEXTDIRECTORY);

        ExtDirItem[] extDirItem = (ExtDirItem[])(this.bundle.getSerializable(KEY_LOCAL_PCMEXTDIRECTORY));
        if(extension.equals(getString(R.string.root_directory_name))) {
            this.bundle.putString(KEY_LOCAL_ROOTDIRECTORY, directory);

        } else {
            for (ExtDirItem edi : extDirItem) {
                if (edi.getExtension().equals(extension)) {
                    edi.setDirectory(directory);
                }
            }
        }

        adapter.clear();

        // ダイアログ表示用のデータ作成
        ExtDirItem[] extDirItem2 = new ExtDirItem[extDirItem.length + 1];
        extDirItem2[0] = new ExtDirItem(getString(R.string.root_directory_name), this.bundle.getString(KEY_LOCAL_ROOTDIRECTORY).replace("|", "/"));
        for(int i = 0; i < extDirItem.length; i++) {
            extDirItem2[i + 1] = new ExtDirItem(extDirItem[i].getExtension(), extDirItem[i].getDirectory().replace("|", "/"));
        }

        adapter.addAll(extDirItem2);
        adapter.notifyDataSetChanged();
    }
}
