package com.mushrooming.activities;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.antonl.mushrooming.BuildConfig;
import com.example.antonl.mushrooming.R;
import com.mushrooming.base.App;
import com.mushrooming.base.Logger;
import com.mushrooming.base.Position;
import com.mushrooming.map.MapModule;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.OverlayItem;

import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "MainActivity";
    private static final int REQUEST_FOR_OSMDROID = 10;

    // overlay
    private final ItemizedIconOverlay.OnItemGestureListener listen = new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
        @Override
        public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
            // some function from example invoked on tap, think what to do here
            return true;
        }
        @Override
        public boolean onItemLongPress(final int index, final OverlayItem item) {
            return false;
        }
    };

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get local Bluetooth adapter
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            this.finish();
        }




        // need to request "dangerous permissions" at runtime since android 6.0
        requestPermissionsForOsmdroid();

        Context ctx = getApplicationContext();

        configClientForOSM(ctx);
        setContentView(R.layout.activity_main); // has to be before App.instance().init because latter uses 'map' from layout
        App.instance().init(this);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_open_team) {
            Intent intent = new Intent(MainActivity.this, TeamActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.menu_connect_device) {
            App.instance().getBluetooth().newConnection();
        }
        else if (id == R.id.menu_mark_postion){
            App.instance().testMarkPosition();
        }
        else if (id == R.id.menu_make_discoverable) {
            App.instance().getBluetooth().ensureDiscoverable();
        }
        else if (id == R.id.menu_send_rand_pos) {
            sendRandomPosition();
        }
        else if (id == R.id.menu_send_connections) {
            App.instance().getBluetooth().sendConnections();
        }
        else if (id == R.id.menu_send_name) {
            App.instance().getBluetooth().sendName();
        }
        else if (id == R.id.menu_open_settings) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        }
        else if (id == R.id.menu_open_debug) {
            Intent intent = new Intent(MainActivity.this, DebugActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }



    private void sendRandomPosition() {
        Random gen = new Random();
        double x = gen.nextGaussian();
        double y = gen.nextGaussian();
        Logger.debug(this, "Sending random position: " + x + " " + y);

        App.instance().getBluetooth().sendPosition( new Position(x,y) );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        App.instance().finish();
    }

    public void onResume() {
        super.onResume();
        // more if changes to configuration made
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        //This is necessary, because BluetoothModule starts activities in context of Main Activity
        //so the results will be passed to this method, but need to be handled in BluetoothModule
        App.instance().getBluetooth().onActivityResult(requestCode,resultCode,data);
    }

    private void requestPermissionsForOsmdroid() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, REQUEST_FOR_OSMDROID);
        }
    }

    private void configClientForOSM(Context ctx) {
        // needed because of OSM ban rules or sth
        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID); //ctx.getPackageName()

        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
    }

}