package com.marco97pa.puntiburraco;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;
import com.marco97pa.puntiburraco.util.IabHelper;
import com.marco97pa.puntiburraco.util.IabResult;
import com.marco97pa.puntiburraco.util.Inventory;
import com.marco97pa.puntiburraco.util.Purchase;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

import static android.R.attr.id;
import static android.widget.Toast.LENGTH_SHORT;


/**
 * MAIN ACTIVITY
 * The main activity contains a fragment corresponding to the actual mode selected by the user (2,3 or 4 players mode)
 * Mode can be changed by the navigation drawer
 * Specific comments will be provided near relative instruction
 *
 * @author Marco Fantauzzo
 */


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static Context contextOfApplication;
    final String TEST_ID="android.test.purchased";
    //Test ID for In-App Billing
    final String PRO_ID="com.marco97pa.puntiburraco.pro";
    //Real ID for In-App Billing
    IInAppBillingService mService;
    final String base64EncodedPublicKey="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA1sDQLIixxrs2UY51TxvoMpxmd3GTLd3cOEx0BOC73S2AyzRERmI2mDdfdzVUEtRVDC+iH0xAkdyFNIaeVU1Yy5eKpT8haFAdWAHRhTgSXCHr6hrTkcPiOkcszIdm4zkzZEqw3R6H16dUOuNxRWwgO2qel4Z6tzxDdKpQdi1QsGYOzG916nvmz3VhwdvYD74D9+UK2bblNHMl6PB7xPWZa2Uf8cPKSvLVhLSXg6i6bsXHKyabczQfj0a+0H7MacAmrBOQcK6CDYPs/6qqPsOiaPCAQelWldArVyTn5MocjCQBXyqvbocOqB6r4eitNK2TqlxUcFYJ4HblA6foi6/LyQIDAQAB";
    /* Public Key for In-App Billing
     * I know that Google says to not make this value easy to read and sharing with others
     * But this app works with donations, so this key will "unlock" no new features, so it is useless to attackers
     */

    //CREATING ACTIVITY AND FAB BUTTON
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        /* SETTING APP THEME
         * Here I set the app theme according to the user choice
         * I do it here because it MUST stay before the "setContentViev(...)"
         */
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Boolean isNight = sharedPreferences.getBoolean("night", false) ;
        if(isNight){
            setTheme(R.style.DarkMode);
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.barBlack));
            }
        }

        /* CREATING ACTIVITY
         * Creating activity and setting its contents, the toolbar, the fab and the first Fragment
         */
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Setting first Fragment to display as DoubleFragment (aka 2 players mode)
        Fragment fragment = new DoubleFragment();
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment, "2").commit();

        //Setting the FAB button - It launches the openStart method of the active Fragment inside activity
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Fragment currentFragment = getFragmentManager().findFragmentById(R.id.content_frame);
                String tag = currentFragment.getTag();
                switch (tag) {
                    case "2":
                        ((DoubleFragment) currentFragment).openStart();
                        break;
                    case "3":
                        ((TripleFragment) currentFragment).openStart();
                        break;
                    case "4":
                        ((QuadFragment) currentFragment).openStart();
                        break;
                }
            }
        });

        /* CREATING NAVIGATION DRAWER
         * Creating navigation drawer and setting its contents
         */
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close){

            //FIX The following methods close the (eventually) opened keyboard on opening the navigation bar
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                InputMethodManager inputMethodManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                InputMethodManager inputMethodManager = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        };
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        //Sets navigation drawer listener
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //Loading In-App Service
        Intent serviceIntent =
                new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);


        //Keep the screen always on if the user has set it true
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Boolean isWakeOn = sharedPreferences.getBoolean("wake", false) ;
        if(isWakeOn){
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

    }


    /* CLOSING ACTIVITY
     * Press back two times to close the app
     */
    private Boolean exit = false;
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (exit) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//***Change Here***
                startActivity(intent);
                finish();
                System.exit(0); // finish activity
            } else {
                Toast.makeText(this, getString(R.string.back_to_exit), Toast.LENGTH_SHORT).show();
                exit = true;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        exit = false;
                    }
                }, 3 * 1000);

            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            unbindService(mServiceConn);
        }
    }

    //On return from another activity
    @Override
    public void onResume(){
        super.onResume();
        //Check if there was a setting change
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Boolean settingsChanged = sharedPreferences.getBoolean("setChange", false) ;
        if(settingsChanged){
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("setChange", false);
            editor.commit();
            recreate();
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


        return super.onOptionsItemSelected(item);
    }

    /* HANDLE NAVIGATION DRAWER SELECTION
     * It changes the fragment when changing mode
     */
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        Fragment fragment = null;
        int id = item.getItemId();

        if (id == R.id.nav_2_player) {
            fragment = new DoubleFragment();
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment,"2").commit();
        } else if (id == R.id.nav_3_player) {
            fragment = new TripleFragment();
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment,"3").commit();

        } else if (id == R.id.nav_4_player) {
            fragment = new QuadFragment();
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment, "4").commit();

        } else if (id == R.id.nav_setting) {
            //Launches Settings Activity
            Intent myIntent = new Intent(this, SettingActivity.class);
            this.startActivity(myIntent);

        } else if (id == R.id.nav_guide) {
            //Check if the device is connected
            ConnectivityManager cm = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

            if(isConnected) {
                //I am using CustomTabs
                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                builder.setToolbarColor(ContextCompat.getColor(this, R.color.colorPrimary));
                builder.setShowTitle(true);
                CustomTabsIntent customTabsIntent = builder.build();
                //Takes the user to the localized page of the user guide
                Locale lang;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                      lang = getResources().getConfiguration().getLocales().get(0);
                } else{
                    //noinspection deprecation
                      lang = getResources().getConfiguration().locale;
                }
                String localeId = lang.toString();
                Log.i("LINGUA", localeId);
                //If lang is italian it will be redirected to guide-it.html., else to guide-en.html
                if(localeId.equals("it_IT")) {
                    customTabsIntent.launchUrl(this, Uri.parse("http://marco97pa.altervista.org/android/puntiburraco/guide-it.html"));
                    Log.i("PASSA", "true");
                }
                else{
                    customTabsIntent.launchUrl(this, Uri.parse("http://marco97pa.altervista.org/android/puntiburraco/guide-en.html"));
                    Log.i("PASSA", "false");
                }
            }
            else{
                //If user is not connected, shows an alert
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder .setTitle(getString(R.string.nav_guide))
                        .setMessage(getString(R.string.errore_connection))
                        .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //do things
                                dialog.cancel();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }

        } else if (id == R.id.nav_info) {
            //It opens Contributions Activity
            Intent myIntent = new Intent(this, Contributions.class);
            this.startActivity(myIntent);

        } else if (id == R.id.nav_upgrade) {
            //It starts in-app process
            try {
                Bundle buyIntentBundle = mService.getBuyIntent(3, getPackageName(),
                        PRO_ID, "inapp", "y");
                PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
                startIntentSenderForResult(pendingIntent.getIntentSender(),
                        1001, new Intent(), Integer.valueOf(0), Integer.valueOf(0),
                        Integer.valueOf(0));

            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    //Simple Method to returno the Context of App
    public static Context getContextOfApplication(){
        return contextOfApplication;
    }

    //Handles the Results of In-App Billing process
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1001) {
            int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");

            if (resultCode == RESULT_OK) {
                        Toast.makeText(this, getString(R.string.buy_success), Toast.LENGTH_LONG).show();
                        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putBoolean("pro", true);
                        editor.commit();
            }
            if(resultCode == 1){
                Toast.makeText(this, getString(R.string.buy_cancel), Toast.LENGTH_LONG).show();
            }
            if(resultCode == 2){
                Toast.makeText(this, getString(R.string.buy_network), Toast.LENGTH_LONG).show();
            }
            if(resultCode == 3){
                Toast.makeText(this, getString(R.string.buy_playservices), Toast.LENGTH_LONG).show();
            }
            if(resultCode == 4){
                Toast.makeText(this, getString(R.string.buy_notavaible), Toast.LENGTH_LONG).show();
            }
            if(resultCode == 5){
                Toast.makeText(this, getString(R.string.buy_dev), Toast.LENGTH_LONG).show();
            }
            if(resultCode == 6){
                Toast.makeText(this, getString(R.string.buy_api), Toast.LENGTH_LONG).show();
            }
            if(resultCode == 7){
                Toast.makeText(this, getString(R.string.buy_already), Toast.LENGTH_LONG).show();
                SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean("pro", true);
                editor.commit();
            }

        }
    }

    ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name,
                                       IBinder service) {
            mService = IInAppBillingService.Stub.asInterface(service);
        }
    };



}
