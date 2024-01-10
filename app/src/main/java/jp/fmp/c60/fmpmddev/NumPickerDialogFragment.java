package jp.fmp.c60.fmpmddev;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

public class NumPickerDialogFragment extends DialogFragment implements NumberPicker.OnValueChangeListener {

    // NumPickerDialog Fragment Tag
    public static final String NUMPICKERDIALOG_FRAGMENT_TAG = "NumPickerDialogFragment";

    public static final String KEY_LOCAL_VALUE              = "localValue";

    // ダイアログで保持する必要のある Bundle
    Bundle bundle = new Bundle();


    public @NonNull Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);

        bundle.putInt(KEY_LOCAL_VALUE, getArguments().getInt(Common.KEY_SETTING_TO_NUMPICKER_VALUE));

        // ダイアログのメインビュー設定、及び、ListView 取得
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View numPickerView = inflater.inflate(R.layout.fragment_numpicker, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
                .setView(numPickerView)

                .setTitle(R.string.numpicker_dialog_title)

                .setPositiveButton(R.string.setting_dialog_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // OK
                        Bundle lBundle = new Bundle();
                        lBundle.putInt(Common.KEY_NUMPICKER_TO_SETTING_VALUE, bundle.getInt(KEY_LOCAL_VALUE));
                        getParentFragmentManager().setFragmentResult(Common.KEY_NUMPICKER_TO_SETTING_FRAGMENTRESULT, lBundle);
                    }
                })
                .setNegativeButton(R.string.setting_dialog_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Cancel
                    }
                });

        NumberPicker np = numPickerView.findViewById(R.id.numberPicker);
        np.setMinValue(1);
        np.setMaxValue(9);
        np.setValue(getArguments().getInt(Common.KEY_SETTING_TO_NUMPICKER_VALUE));
        np.setOnValueChangedListener(this);

        // Create the AlertDialog object and return it
        return builder.create();
    }


    @Override
    public void onValueChange(NumberPicker numberPicker, int oldVal, int newVal) {
          bundle.putInt(KEY_LOCAL_VALUE, newVal);
    }
}
