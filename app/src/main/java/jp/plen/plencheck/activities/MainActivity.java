package jp.plen.plencheck.activities;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.eccyan.optional.Optional;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.util.List;

import jp.plen.plencheck.R;
import jp.plen.plencheck.fragments.dialog.LocationSettingRequestDialogFragment;
import jp.plen.plencheck.fragments.dialog.LocationSettingRequestDialogFragment_;
import jp.plen.plencheck.fragments.dialog.OpenSourceLicensesDialogFragment;
import jp.plen.plencheck.fragments.dialog.PlenScanningDialogFragment;
import jp.plen.plencheck.fragments.dialog.PlenScanningDialogFragment_;
import jp.plen.plencheck.fragments.dialog.SelectPlenDialogFragment;
import jp.plen.plencheck.fragments.dialog.SelectPlenDialogFragment_;
import jp.plen.plencheck.models.preferences.MainPreferences_;
import jp.plen.plencheck.services.PlenConnectionService_;
import jp.plen.plencheck.services.PlenScanService_;
import jp.plen.rx.utils.Operators;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

@EActivity
public class MainActivity extends Activity implements IMainActivity {
    private String TAG = "MainActivity";
    private static final String SCANNING_DIALOG = PlenScanningDialogFragment.class.getSimpleName();
    private static final String SELECT_PLEN_DIALOG = SelectPlenDialogFragment.class.getSimpleName();
    private static final String OSS_LICENSES_DIALOG = OpenSourceLicensesDialogFragment.class.getSimpleName();
    private static final String LOCATION_SETTING_DIALOG = LocationSettingRequestDialogFragment.class.getSimpleName();
    private boolean isClearChecked = false;
    private int checkedNum = 0;
    // private BLEDevice bleDevice;

    private final ServiceConnection mPlenConnectionService = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };
    private final ServiceConnection mPlenScanService = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    @NonNull private Optional<FragmentManager> mFragmentManager = Optional.empty();
    @Pref MainPreferences_ mPref;
    private final CompositeSubscription mSubscriptions = new CompositeSubscription();
    @Bean PlenConnectionActivityPresenter mPresenter;

    private int map[] = {0, 1, 2, 3, 4, 5, 6, 7, 8, 12, 13, 14, 15, 16, 17, 18, 19, 20};
    private int default_position[] = {
            -40, 245, 470, -100, -205, 50, 445, 245, -75, 15, -70, -390, 250, 195, -105, -510, -305, 60
    };

    private float JointButtonLocation[][] = {
            {653, 345},
            {665, 491},
            {858, 259},
            {950, 483},
            {729, 649},
            {817, 776},
            {645, 905},
            {820, 1096},
            {707, 1195},
            {449, 350},
            {430, 517},
            {259, 232},
            {159, 486},
            {382, 669},
            {270, 799},
            {441, 915},
            {281, 1097},
            {377, 1184}
    };

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    private boolean isInRange(double centerX, double centerY, double pushedX, double pushedY, double margin) {
        return      (centerX+margin >= pushedX && centerX-margin <= pushedX)
                &&  (centerY+margin >= pushedY && centerY-margin <= pushedY);
    }

    private int convertToJointNum(double x, double y) {
        for(int i = 0; i < 18; i++) {
            if(isInRange(JointButtonLocation[i][0]/1080, JointButtonLocation[i][1]/1296, x, y, 0.05)) {
                return i;
            }
        }
        return 0;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final SeekBar vs = (SeekBar) findViewById(R.id.seekBar);
        final TextView tv = (TextView) findViewById(R.id.textView);
        final ImageView iv = (ImageView) findViewById(R.id.plen);

        vs.setProgress(default_position[0]+900);
        tv.setText(String.valueOf(vs.getProgress() ));

        // 通信用Service起動
        bindService(new Intent(this, PlenConnectionService_.class), mPlenConnectionService, BIND_AUTO_CREATE);
        bindService(new Intent(this, PlenScanService_.class), mPlenScanService, BIND_AUTO_CREATE);

        iv.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        switch(event.getAction()){
                            case MotionEvent.ACTION_DOWN:
                                Log.d(TAG, String.valueOf(convertToJointNum(event.getX() / iv.getWidth(), event.getY() / iv.getHeight())));
                                checkedNum = convertToJointNum(event.getX() / iv.getWidth(), event.getY() / iv.getHeight());

                                int value = map[checkedNum];
                                vs.setProgress(default_position[checkedNum]+900);
                                tv.setText(String.valueOf(vs.getProgress() -900));
                                String hexNum = String.format("%02x", value);
                                String deg = String.format("%03x", (vs.getProgress() -900 ) & 0xFFF);
                                default_position[checkedNum] = vs.getProgress() -900;
                                Log.d(TAG, "$an" + hexNum + deg);
                                // bleDevice.write("$an" + hexNum + deg);
                                break;
                        }
                        return false;
                  }
              }
        );


        vs.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        tv.setText(String.valueOf(vs.getProgress() -900 ));
                        // now debuging
                        //int i = Integer.parseInt(((RadioButton)findViewById(checkedNum)).getText().toString());
                        int i = checkedNum+1;
                        int value = map[i - 1];
                        String hexNum = String.format("%02x", value);
                        String deg = String.format("%03x", (vs.getProgress() -900 ) & 0xFFF);
                        default_position[i - 1] = vs.getProgress() -900;
                        Log.d(TAG, "$an" + hexNum + deg);
                        //bleDevice.write("$an" + hexNum + deg);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        tv.setText(String.valueOf(vs.getProgress() -900 ));
                        //int i = Integer.parseInt(((RadioButton)findViewById(checkedNum)).getText().toString());
                        int i = checkedNum+1;
                        int value = map[i - 1];
                        String hexNum = String.format("%02x", value);
                        String deg = String.format("%03x", (vs.getProgress() -900 ) & 0xFFF);
                        default_position[i - 1] = vs.getProgress() -900;
                        Log.d(TAG, "$an" + hexNum + deg);
                        //bleDevice.write("$an" + hexNum + deg);
                    }
                }
        );

        final Button bup = (Button) findViewById(R.id.buttonup);
        final Button dup = (Button) findViewById(R.id.buttondown);

        bup.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        vs.setProgress(vs.getProgress() + 1);
                        tv.setText(String.valueOf(vs.getProgress() -900 ));
                        //int i = Integer.parseInt(((RadioButton)findViewById(checkedNum)).getText().toString());
                        int i = checkedNum+1;
                        int value = map[i - 1];
                        String hexNum = String.format("%02x", value);
                        String deg = String.format("%03x", (vs.getProgress() -900 ) & 0xFFF);
                        default_position[i - 1] = vs.getProgress() -900;
                        Log.d(TAG, "$an" + hexNum + deg);
                        // bleDevice.write("$an" + hexNum + deg);
                    }
                }
        );

        dup.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        vs.setProgress(vs.getProgress() - 1);
                        tv.setText(String.valueOf(vs.getProgress() -900 ));
                        //int i = Integer.parseInt(((RadioButton)findViewById(checkedNum)).getText().toString());
                        int i = checkedNum+1;
                        int value = map[i - 1];
                        String hexNum = String.format("%02x", value);
                        String deg = String.format("%03x", (vs.getProgress() -900 ) & 0xFFF);
                        default_position[i - 1] = vs.getProgress() -900;
                        Log.d(TAG, "$an" + hexNum + deg);
                        // bleDevice.write("$an" + hexNum + deg);
                    }
                }
        );

        Button homeButton = (Button) findViewById(R.id.button);
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //int i = Integer.parseInt(((RadioButton)findViewById(checkedNum)).getText().toString());
                int i = checkedNum + 1;
                int value = map[i - 1];
                String hexNum = String.format("%02x", value);
                String deg = String.format("%03x", (vs.getProgress() -900 ) & 0xFFF);
                Log.d(TAG, ">ho" + hexNum + deg);
                // bleDevice.write(">ho" + hexNum + deg);
            }
        });
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /*
    @Override
    public void onConnected(String deviceName) {
        Toast.makeText(this, "Connected " + deviceName, Toast.LENGTH_SHORT).show();
    }
    */

    /*
    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://jp.plen.plencheck/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://jp.plen.plencheck/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }
    */

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume ");
        mFragmentManager = Optional.ofNullable(getFragmentManager());
        dismissDialogFragment(SCANNING_DIALOG);
        mFragmentManager
                .map(fm -> (SelectPlenDialogFragment) fm.findFragmentByTag(SELECT_PLEN_DIALOG))
                .map(SelectPlenDialogFragment::getAddresses) // .map(f -> f.getAddress())
                .ifPresent(this::notifyPlenScanComplete);
        mFragmentManager
                .map(fm -> fm.findFragmentByTag(LOCATION_SETTING_DIALOG))
                .ifPresent(fm -> notifyLocationUnavailable());
        mPresenter.bind(this);
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause ");
        mFragmentManager = Optional.empty();
        mPresenter.unbind();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        mSubscriptions.unsubscribe();
        unbindService(mPlenConnectionService);
        unbindService(mPlenScanService);
        super.onDestroy();
    }

    @Override
    public void notifyBluetoothUnavailable() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        int requestCode = 1;
        startActivityForResult(intent, requestCode);
    }

    @Override
    public void notifyLocationUnavailable() {
        CompositeSubscription subscriptions = new CompositeSubscription();
        LocationSettingRequestDialogFragment fragment = LocationSettingRequestDialogFragment_.builder()
                .build();
        fragment.allowEvent()
                .lift(Operators.composite(subscriptions))
                .subscribe(v -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    int requestCode = 1;
                    startActivityForResult(intent, requestCode);
                });
        mFragmentManager.ifPresent(m -> fragment.show(m, LOCATION_SETTING_DIALOG));
        mSubscriptions.add(subscriptions);
    }

    @Override
    public void notifyPlenScanning() {
        dismissDialogFragment(SCANNING_DIALOG);

        PlenScanningDialogFragment fragment = PlenScanningDialogFragment_.builder().build();
        Subscription subscription = fragment.cancelEvent().subscribe(v -> mPresenter.cancelScan());
        mFragmentManager.ifPresent(fm -> fragment.show(fm, SCANNING_DIALOG));
        mSubscriptions.add(subscription);
    }

    @Override
    public void notifyPlenScanCancel() {
        dismissDialogFragment(SCANNING_DIALOG);
    }

    @Override
    public void notifyPlenScanComplete(@NonNull List<String> addresses) {
        dismissDialogFragment(SCANNING_DIALOG);
        dismissDialogFragment(SELECT_PLEN_DIALOG);

        String defaultAddress = mPref.defaultPlenAddress().get();
        CompositeSubscription subscriptions = new CompositeSubscription();

        SelectPlenDialogFragment fragment = SelectPlenDialogFragment_.builder()
                .addresses(addresses.toArray(new String[addresses.size()]))
                .defaultAddress(defaultAddress)
                .build();

        fragment.onDeviceSelectEvent()
                .lift(Operators.composite(subscriptions))
                .subscribe(i -> mPref.edit()
                        .defaultPlenAddress().put(addresses.get(i))
                        .apply());

        fragment.cancelEvent()
                .lift(Operators.composite(subscriptions))
                .subscribe(v -> mPref.edit()
                        .defaultPlenAddress().put(defaultAddress)
                        .apply());

        fragment.okEvent()
                .lift(Operators.composite(subscriptions))
                .subscribe(v -> Optional
                        .ofNullable(mPref.defaultPlenAddress().get())
                        .filter(address -> !address.isEmpty())
                        .ifPresent(mPresenter::connectPlen));

        fragment.rescanEvent()
                .lift(Operators.composite(subscriptions))
                .subscribe(v -> mPresenter.startScan());

        mFragmentManager.ifPresent(m -> fragment.show(m, SELECT_PLEN_DIALOG));

        mSubscriptions.add(subscriptions);
    }

    @UiThread
    @Override
    public void notifyPlenConnectionChanged(boolean connected, boolean now) {
        if (now) {
            if (connected) {
                Toast.makeText(this, R.string.plen_connected, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.plen_disconnected, Toast.LENGTH_SHORT).show();
            }
        }
        // updateToolbar();
    }

    @Override
    public void notifyWriteTxDataCompleted() {
        // updateToolbar();
    }

    @Override
    public void notifyConnectionError(@NonNull Throwable e) {
        Toast.makeText(this, R.string.connection_error, Toast.LENGTH_SHORT).show();
        Log.e(TAG, "onConnectionError ", e);
    }

    @UiThread
    void dismissDialogFragment(@NonNull String tag) {
        mFragmentManager.map(fm -> fm.findFragmentByTag(tag))
                .filter(f -> f instanceof DialogFragment)
                .map(f -> (DialogFragment) f)
                .filter(DialogFragment::getShowsDialog)
                .ifPresent(f -> f.onDismiss(f.getDialog()));
    }
}
