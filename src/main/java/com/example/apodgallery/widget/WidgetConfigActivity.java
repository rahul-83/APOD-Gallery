package com.example.apodgallery.widget;

import android.Manifest;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.NASAapi.R;

public class WidgetConfigActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {

    final static String TAG_MAX_IMAGES = "pref_max_images";
    private static final int READ_PERMISSION = 103;
    private static NumberPickerPreference maxImages;
    private int widgetId;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);

        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                new ConfigFragment()).commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();

        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        LinearLayout root;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            root = (LinearLayout) findViewById(android.R.id.list).getParent().getParent();
        } else {
            root = (LinearLayout) findViewById(android.R.id.list).getParent();
        }

        Toolbar bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.view_toolbar, root,
                false);

        root.addView(bar, 0); // insert at top
        bar.setNavigationIcon(null);
        bar.showOverflowMenu();
        setSupportActionBar(bar);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        SharedPreferences.Editor editor = prefs.edit();

        switch (key) {
            case TAG_MAX_IMAGES:
                if (maxImages != null) {
                    maxImages.setSummary(String.valueOf(maxImages.getValue()));
                    // editor.putInt(String.valueOf(widgetId) + key, maxImages.getValue());
                    editor.putInt(key, maxImages.getValue());
                }
                break;
            default:
                break;
        }
        editor.apply();
    }

    // Inflate options menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.config_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        MenuItem menuItem = menu.findItem(R.id.action_widget_save);
        Drawable drawable = menuItem.getIcon();

        if (drawable != null) {
            drawable.mutate();
            drawable.setColorFilter(ContextCompat.getColor(WidgetConfigActivity.this,
                    R.color.colorWhite), PorterDuff.Mode.SRC_ATOP);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_widget_save:
                int androidVersion = Build.VERSION.SDK_INT;
                if (androidVersion >= Build.VERSION_CODES.M) {
                    if (!checkPermission()) {
                        ActivityCompat.requestPermissions(WidgetConfigActivity.this,
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_PERMISSION);
                    } else {
                        finishIntent();
                    }
                } else {
                    finishIntent();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void finishIntent() {
        SharedPreferences.Editor editor = prefs.edit();

        Log.i("maxImages", "" + maxImages.getValue());
        if (maxImages != null) {
            editor.putInt("pref_max_images", maxImages.getValue()).apply();
        }

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        resultValue.putExtra(TAG_MAX_IMAGES, maxImages.getValue());
        setResult(RESULT_OK, resultValue);
        finish();
    }

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Display permission granted toast
     *
     * @param requestCode  permission request code
     * @param permissions  array of permissions asked
     * @param grantResults results if permissions were granted or revoked
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case READ_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    finishIntent();
                    return;
                } else {
                    Toast.makeText(WidgetConfigActivity.this, R.string.toast_storage, Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public static class ConfigFragment extends PreferenceFragment {
        private int DEFAULT_MAX_IMAGES = 10;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.widget_config);
            maxImages = (NumberPickerPreference) findPreference(TAG_MAX_IMAGES);
            maxImages.setValue(DEFAULT_MAX_IMAGES);
        }
    }

    /*
    public static String getSharedPreferencesNameForAppWidget(Context context, int appWidgetId) {
        return context.getPackageName() + "_preferences_" + appWidgetId;
    }

    public static SharedPreferences getSharedPreferencesForAppWidget(Context context, int appWidgetId) {
        return context.getSharedPreferences(
                getSharedPreferencesNameForAppWidget(context, appWidgetId), 0);
    }
    */
}