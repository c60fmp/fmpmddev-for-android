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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentResultListener;


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


public class SettingDialogFragment extends DialogFragment implements LinearLayout.OnClickListener, AdapterView.OnItemClickListener, FragmentResultListener {

    // SettingDialog Fragment Tag
    public static final String SETTINGDIALOG_FRAGMENT_TAG       = "SettingDialogFragment";

    private static final String KEY_LOCAL_LOOPCOUNT             = "localLoopCount";

    private static final String KEY_LOCAL_ROOTDIRECTORY         = "localRootDirectory";

    private static final String KEY_LOCAL_PCMEXTDIRECTORY       = "localPCMExtDirectory";

    private static final String KEY_LOCAL_SAVELOOPCOUNT         = "localSaveLoopCount";

    private static final String KEY_LOCAL_SAVEROOTDIRECTORY     = "localSaveRootDirectory";

    private static final String KEY_LOCAL_SAVEPCMEXTDIRECTORY   = "localSavePCMExtDirectory";

    // Setting Dialog Fragment から呼び出す Callback
    public interface SettingDialogFragmentListener {
        void onSelectRootDirectory(Bundle bundle);
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

        if(savedInstanceState == null) {
            bundle.putInt(KEY_LOCAL_LOOPCOUNT, getArguments().getInt(Common.KEY_ACTIVITY_TO_SETTING_LOOPCOUNT));
            bundle.putString(KEY_LOCAL_ROOTDIRECTORY, getArguments().getString(Common.KEY_ACTIVITY_TO_SETTING_ROOTDIRECTORY));
            bundle.putSerializable(KEY_LOCAL_PCMEXTDIRECTORY, Common.suppressSerializable(getArguments(), Common.KEY_ACTIVITY_TO_SETTING_PCMEXTDIRECTORY, new ExtDirItem[0]));

        } else {
            bundle.putInt(KEY_LOCAL_LOOPCOUNT, savedInstanceState.getInt(KEY_LOCAL_SAVELOOPCOUNT));
            bundle.putString(KEY_LOCAL_ROOTDIRECTORY, savedInstanceState.getString(KEY_LOCAL_SAVEROOTDIRECTORY));
            bundle.putSerializable(KEY_LOCAL_PCMEXTDIRECTORY, Common.suppressSerializable(savedInstanceState, KEY_LOCAL_SAVEPCMEXTDIRECTORY, new ExtDirItem[0]));
        }

        // ダイアログのメインビュー設定、及び、ListView 取得
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View settingView = inflater.inflate(R.layout.fragment_setting, null);

        LinearLayout linearLayoutLoopCount = settingView.findViewById(R.id.linearLayoutLoopCount);
        linearLayoutLoopCount.setOnClickListener(this);

        TextView textView = settingView.findViewById(R.id.textViewLoopCount);
        textView.setText(String.valueOf(bundle.getInt(KEY_LOCAL_LOOPCOUNT)));

        ListView listView = settingView.findViewById(R.id.listview_setting);
        listView.setOnItemClickListener(this);

        adapter = new ArrayAdapterExtDir(SettingDialogFragment.this.getActivity(), R.layout.list_item);
        listView.setAdapter(adapter);

        setAdapterData();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
            .setView(settingView)

            .setPositiveButton(R.string.setting_dialog_ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // OK
                    Bundle lBundle = new Bundle();
                    lBundle.putInt(Common.KEY_SETTING_TO_ACTIVITY_LOOPCOUNT, bundle.getInt(KEY_LOCAL_LOOPCOUNT));
                    lBundle.putString(Common.KEY_SETTING_TO_ACTIVITY_ROOTDIRECTORY, bundle.getString(KEY_LOCAL_ROOTDIRECTORY));
                    lBundle.putSerializable(Common.KEY_SETTING_TO_ACTIVITY_PCMEXTDIRECTORY, Common.suppressSerializable(bundle, KEY_LOCAL_PCMEXTDIRECTORY, new ExtDirItem[0]));
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


    @Override
    // Loop Count をタップしたときのイベント
    public void onClick(View view) {
        Bundle lBundle = new Bundle();

        lBundle.putInt(Common.KEY_SETTING_TO_NUMPICKER_VALUE, bundle.getInt(KEY_LOCAL_LOOPCOUNT));

        //　ダイアログを開く
        DialogFragment numPickerDialogFragment = new NumPickerDialogFragment();
        numPickerDialogFragment.setArguments(lBundle);

        getParentFragmentManager().setFragmentResultListener(Common.KEY_NUMPICKER_TO_SETTING_FRAGMENTRESULT, this, this);
        numPickerDialogFragment.show(getActivity().getSupportFragmentManager(), NumPickerDialogFragment.NUMPICKERDIALOG_FRAGMENT_TAG);
    }


    // ListView の行をクリックしたときのイベント
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        ExtDirItem[] extDirItem = Common.suppressSerializable(bundle, KEY_LOCAL_PCMEXTDIRECTORY, new ExtDirItem[0]);
        if(position > extDirItem.length) {
            return;
        }

        String extension;
        String directory;

        if(position == 0) {
            // root directory
            Bundle lBundle = new Bundle();
            lBundle.putString(Common.KEY_SETTING_TO_ACTIVITY_ROOTDIRECTORY, bundle.getString(KEY_LOCAL_ROOTDIRECTORY));
            listener.onSelectRootDirectory(bundle);

        } else {
            extension = extDirItem[position - 1].getExtension();
            directory = extDirItem[position - 1].getDirectory();

            Bundle lBundle = new Bundle();

            lBundle.putString(Common.KEY_SETTING_TO_DIRECTORY_ROOTDIRECTORY, bundle.getString(KEY_LOCAL_ROOTDIRECTORY));
            lBundle.putString(Common.KEY_SETTING_TO_DIRECTORY_PCMEXT, extension);
            lBundle.putString(Common.KEY_SETTING_TO_DIRECTORY_PCMEXTDIRECTORY, directory);

            //　ダイアログを開く
            DialogFragment directoryDialogFragment = new DirectoryDialogFragment();
            directoryDialogFragment.setArguments(lBundle);

            getParentFragmentManager().setFragmentResultListener(Common.KEY_DIRECTORY_TO_SETTING_FRAGMENTRESULT, this, this);
            directoryDialogFragment.show(getActivity().getSupportFragmentManager(), DirectoryDialogFragment.DIRECTORYDIALOG_FRAGMENT_TAG);
        }
    }


    @Override
    public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle bundle) {
        switch(requestKey) {
            case Common.KEY_DIRECTORY_TO_SETTING_FRAGMENTRESULT:
                String extension = bundle.getString(Common.KEY_DIRECTORY_TO_SETTING_PCMEXT);
                String directory = bundle.getString(Common.KEY_DIRECTORY_TO_SETTING_PCMEXTDIRECTORY);

                ExtDirItem[] extDirItem = Common.suppressSerializable(this.bundle, KEY_LOCAL_PCMEXTDIRECTORY, new ExtDirItem[0]);
                if (extension.equals(getString(R.string.root_directory_name))) {
                    this.bundle.putString(KEY_LOCAL_ROOTDIRECTORY, directory);

                } else {
                    for (ExtDirItem edi : extDirItem) {
                        if (edi.getExtension().equals(extension)) {
                            edi.setDirectory(directory);
                        }
                    }
                }

                setAdapterData();
                break;

            case Common.KEY_NUMPICKER_TO_SETTING_FRAGMENTRESULT:
                this.bundle.putInt(KEY_LOCAL_LOOPCOUNT, bundle.getInt(Common.KEY_NUMPICKER_TO_SETTING_VALUE));
                TextView textView = getDialog().findViewById(R.id.textViewLoopCount);
                textView.setText(String.valueOf(this.bundle.getInt(KEY_LOCAL_LOOPCOUNT)));
                break;
        }
    }


    public void onSetRootDirectory(Bundle lBundle) {
        bundle.putString(KEY_LOCAL_ROOTDIRECTORY, lBundle.getString(Common.KEY_ACTIVITY_TO_SETTING_ROOTDIRECTORY));

        // PCM ディレクトリ が root directory と同一 tree でない場合、root directory に強制変更
        ExtDirItem[] extDirItem = Common.suppressSerializable(bundle, KEY_LOCAL_PCMEXTDIRECTORY, new ExtDirItem[0]);
        for(ExtDirItem it : extDirItem) {
            if(!PathUtil.isSameTree(bundle.getString(KEY_LOCAL_ROOTDIRECTORY), it.getDirectory())) {
                it.setDirectory(bundle.getString(KEY_LOCAL_ROOTDIRECTORY));
            }
        }

        setAdapterData();
    }


    public void setAdapterData() {
        String extension = bundle.getString(Common.KEY_DIRECTORY_TO_SETTING_PCMEXT);
        String directory = bundle.getString(Common.KEY_DIRECTORY_TO_SETTING_PCMEXTDIRECTORY);

        ExtDirItem[] extDirItem = Common.suppressSerializable(this.bundle, KEY_LOCAL_PCMEXTDIRECTORY, new ExtDirItem[0]);
        if(extension != null && extension.equals(getString(R.string.root_directory_name))) {
            this.bundle.putString(KEY_LOCAL_ROOTDIRECTORY, directory);

        } else {
            for (ExtDirItem edi : extDirItem) {
                if (edi.getExtension().equals(extension)) {
                    edi.setDirectory(directory);
                }
            }
        }

        // ダイアログ表示用のデータ作成
        ExtDirItem[] extDirItem2 = new ExtDirItem[extDirItem.length + 1];
        extDirItem2[0] = new ExtDirItem(getString(R.string.root_directory_name), PathUtil.getDisplayPath(this.bundle.getString(KEY_LOCAL_ROOTDIRECTORY)).replace("|", "/"));
        for(int i = 0; i < extDirItem.length; i++) {
            extDirItem2[i + 1] = new ExtDirItem(extDirItem[i].getExtension(), PathUtil.getDisplayPath(extDirItem[i].getDirectory()).replace("|", "/"));
        }

        adapter.clear();
        adapter.addAll(extDirItem2);
        adapter.notifyDataSetChanged();
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_LOCAL_SAVELOOPCOUNT, bundle.getInt(KEY_LOCAL_LOOPCOUNT));
        outState.putString(KEY_LOCAL_SAVEROOTDIRECTORY, bundle.getString(KEY_LOCAL_ROOTDIRECTORY));
        outState.putSerializable(KEY_LOCAL_SAVEPCMEXTDIRECTORY, Common.suppressSerializable(bundle, KEY_LOCAL_PCMEXTDIRECTORY, new ExtDirItem[0]));
    }
}
