package com.pbartz.heartmonitor.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.pbartz.heartmonitor.R;

/**
 * Created by yura.ilyaev on 10/1/2015.
 */
public class SettingsFragment extends DialogFragment{

    public static final String TAG = "DialogFragment";

    public interface SettingsDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog, int age, int maxHr, int restingHr, String schema);
        public void onDialogNegativeClick(DialogFragment dialog);
        public void onDeviceForgetClick(DialogFragment dialog);
    }

    SettingsDialogListener mListener;

    AlertDialog dialog;
    EditText editAge;
    EditText editMaxHr;
    EditText editRestingHr;
    Spinner spinnerSchema;

    Button btnForget;

    String sAge = "30";
    String sMaxHr = "190";
    String sRestingHr = "60";
    String sDeviceName = null;
    String sDeviceAddr = null;
    String sSchema = "Strava";

    TextView labelDevice;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mListener = (SettingsDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement SettingsDialogListener");
        }
    }

    public String getsAge() {
        return sAge;
    }

    public void setsAge(String sAge) {
        this.sAge = sAge;
    }

    public String getsRestingHr() {
        return sRestingHr;
    }

    public void setsRestingHr(String sRestingHr) {
        this.sRestingHr = sRestingHr;
    }

    public String getsMaxHr() {
        return sMaxHr;
    }

    public void setsMaxHr(String sMaxHr) {
        this.sMaxHr = sMaxHr;
    }

    public String getsDeviceName() {
        return sDeviceName;
    }

    public void setsDeviceName(String sDeviceName) {
        this.sDeviceName = sDeviceName;
    }

    public String getsDeviceAddr() {
        return sDeviceAddr;
    }

    public void setsDeviceAddr(String sDeviceAddr) {
        this.sDeviceAddr = sDeviceAddr;
    }

    public void setAge(String sAge) {
        editAge.setText(sAge);
    }

    public void setRestingHr(String sRestingHr) {
        editRestingHr.setText(sRestingHr);
    }

    public String getsSchema() {
        return sSchema;
    }

    public void setsSchema(String sSchema) {
        this.sSchema = sSchema;
    }

    public void setMaxHr(String sMaxHr) {
        editMaxHr.setText(sMaxHr);
    }

    private int getSpinnerIndex(Spinner spinner, String myString)
    {
        int index = 0;

        for (int i=0;i<spinner.getCount();i++){
            if (spinner.getItemAtPosition(i).toString().equalsIgnoreCase(myString)){
                index = i;
                break;
            }
        }
        return index;
    }

    public void setDevice(String sDeviceName, String sDeviceAddr) {
        if (sDeviceName == null || sDeviceName.equals("")) {

            labelDevice.setVisibility(View.INVISIBLE);
            btnForget.setVisibility(View.INVISIBLE);

        } else {

            labelDevice.setText("Paired with: " + sDeviceName);
            labelDevice.setVisibility(View.VISIBLE);
            btnForget.setVisibility(View.VISIBLE);

        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();

        builder.setView(inflater.inflate(R.layout.settings_fragment, null));

        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mListener.onDialogPositiveClick(SettingsFragment.this, getAge(), getMaxHeartRate(), getRestingHeartRate(), getSchema());
            }
        });


        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mListener.onDialogNegativeClick(SettingsFragment.this);
            }
        });


        dialog = builder.create();

        return dialog;

    }

    public int getRestingHeartRate() {
        int restingHr = 0;

        try {
            restingHr = Integer.parseInt(editRestingHr.getText().toString());
        } catch (Exception e) {
            restingHr = 60;
        }

        if (restingHr < 40) {
            restingHr = 40;
        } else if (restingHr > 150) {
            restingHr = 150;
        }

        return restingHr;
    }

    public int getMaxHeartRate() {
        int maxHr = 0;

        try {

            maxHr = Integer.parseInt(editMaxHr.getText().toString());

        } catch (Exception e) {

            maxHr = 190;

        }

        if (maxHr > 250) {
            maxHr = 250;
        }

        return maxHr;
    }

    public int getAge() {
        int maxHr = 0;

        try {

            maxHr = Integer.parseInt(editAge.getText().toString());

        } catch (Exception e) {

            maxHr = 30;

        }

        if (maxHr > 150) {
            maxHr = 150;
        }

        return maxHr;
    }

    public void setSchema(String schema) {
        spinnerSchema.setSelection(getSpinnerIndex(spinnerSchema, schema));
    }

    public String getSchema() {
        return String.valueOf(spinnerSchema.getSelectedItem());
    }

    @Override
    public void onStart() {
        super.onStart();

        editAge = (EditText) dialog.findViewById(R.id.editAge);
        editMaxHr = (EditText) dialog.findViewById(R.id.editMaxHr);
        editRestingHr = (EditText) dialog.findViewById(R.id.editRestingHr);

        spinnerSchema = (Spinner) dialog.findViewById(R.id.spinnerSchema);

        labelDevice = (TextView) dialog.findViewById(R.id.labelPaired);

        btnForget = (Button) dialog.findViewById(R.id.btnForget);

        setAge(sAge);
        setMaxHr(sMaxHr);
        setRestingHr(sRestingHr);
        setDevice(sDeviceName, sDeviceAddr);
        setSchema(sSchema);

        btnForget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onDeviceForgetClick(SettingsFragment.this);
            }
        });

        editAge.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int age = 30;

                try {

                    age = Integer.parseInt(editAge.getText().toString());

                } catch (Exception e) {
                    Log.i(TAG, e.toString());
                    age = 30;
                }

                if (age > 150) {
                    age = 150;
                    editAge.setText("" + age);
                }

                editMaxHr.setText(Integer.toString(220 - age));

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }
}
