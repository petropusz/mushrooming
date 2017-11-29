package com.example.antonl.mushrooming;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.mushrooming.base.App;

public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "MainActivity";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get local Bluetooth adapter
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            this.finish();
        }

        App.instance().init(this);

        setContentView(R.layout.activity_main);

        Button button = findViewById(R.id.open_team);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DisplayUsersActivity.class);
                startActivity(intent);
            }
        });
    }


    @Override
    public void onDestroy() {
        super.onDestroy();

        App.instance().finish();
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //This is necessary, because BluetoothModule starts activities in context of Main Activity
        //so the results will be passed to this method, but need to be handled in BluetoothModule
        App.instance().getBluetooth().onActivityResult(requestCode,resultCode,data);
    }

}