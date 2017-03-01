package jp.plen.plencheck.activities;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.eccyan.optional.Optional;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.jakewharton.rxbinding.widget.RxSeekBar;
import com.jakewharton.rxbinding.widget.SeekBarProgressChangeEvent;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Bean;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.greenrobot.event.EventBus;
import jp.plen.plencheck.R;
import jp.plen.plencheck.fragments.dialog.LocationSettingRequestDialogFragment;
import jp.plen.plencheck.fragments.dialog.LocationSettingRequestDialogFragment_;
import jp.plen.plencheck.fragments.dialog.OpenSourceLicensesDialogFragment;
import jp.plen.plencheck.fragments.dialog.OpenSourceLicensesDialogFragment_;
import jp.plen.plencheck.fragments.dialog.PlenScanningDialogFragment;
import jp.plen.plencheck.fragments.dialog.PlenScanningDialogFragment_;
import jp.plen.plencheck.fragments.dialog.SelectPlenDialogFragment;
import jp.plen.plencheck.fragments.dialog.SelectPlenDialogFragment_;
import jp.plen.plencheck.models.preferences.MainPreferences_;
import jp.plen.plencheck.services.PlenConnectionService;
import jp.plen.plencheck.services.PlenConnectionService_;
import jp.plen.plencheck.services.PlenScanService_;
import jp.plen.rx.utils.Operators;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subscriptions.CompositeSubscription;

@EActivity(R.layout.activity_main)
public class MainActivity extends Activity implements IMainActivity {
    private String TAG = "MainActivity";
    private static final String SCANNING_DIALOG = PlenScanningDialogFragment.class.getSimpleName();
    private static final String SELECT_PLEN_DIALOG = SelectPlenDialogFragment.class.getSimpleName();
    private static final String OSS_LICENSES_DIALOG = OpenSourceLicensesDialogFragment.class.getSimpleName();
    private static final String LOCATION_SETTING_DIALOG = LocationSettingRequestDialogFragment.class.getSimpleName();
    private int checkedNum = 0;

    @ViewById(R.id.seekBar) SeekBar vs;
    @ViewById(R.id.textView)TextView tv;
    @ViewById(R.id.plen) ImageView iv;
    @ViewById(R.id.buttonup) Button bup;
    @ViewById(R.id.buttondown) Button dup;
    @ViewById(R.id.button) Button homeButton;
    @ViewById(R.id.toolbar) Toolbar mToolbar;

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

    private int convertToJointNum(double x, double y, int prevNum) {
        for(int i = 0; i < 18; i++) {
            if(isInRange(JointButtonLocation[i][0]/1080, JointButtonLocation[i][1]/1296, x, y, 0.05)) {
                return i;
            }
        }
        return prevNum;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 通信用Service起動
        bindService(new Intent(this, PlenConnectionService_.class), mPlenConnectionService, BIND_AUTO_CREATE);
        bindService(new Intent(this, PlenScanService_.class), mPlenScanService, BIND_AUTO_CREATE);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

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

    @AfterViews
    void afterviews(){

        vs.setProgress(default_position[0] + 900);
        tv.setText(String.valueOf(vs.getProgress()));
        iv.setImageResource(R.drawable.s0);

        // SeekBarの変更イベント管理
        mSubscriptions.add(
                RxSeekBar.changeEvents(vs)
                        .ofType(SeekBarProgressChangeEvent.class)
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext(seekBarChangeEvent -> {
                            int progress = seekBarChangeEvent.progress();
                            tv.setText(String.valueOf(progress - 900));
                            default_position[checkedNum] = progress - 900;
                            Log.d(TAG, "changeEvent");
                        })
                        .throttleFirst(100, TimeUnit.MILLISECONDS)
                        .subscribe(seekBarChangeEvent -> {
                            int progress = seekBarChangeEvent.progress();
                            int value = map[checkedNum];
                            String hexNum = String.format("%02x", value);
                            String deg = String.format("%03x", (progress - 900) & 0xFFF);
                            default_position[checkedNum] = progress - 900;
                            String program = "$an" + hexNum + deg;
                            Log.d(TAG, program);
                            EventBus.getDefault().post(new PlenConnectionService.WriteRequest(program));
                        })
        );

        updateToolbar();

        bup.setOnTouchListener(new RepeatListner(400, 50, v -> vs.setProgress(vs.getProgress() + 1)));

        dup.setOnTouchListener(new RepeatListner(400, 50, v -> vs.setProgress(vs.getProgress() - 1)));

        iv.setOnTouchListener(
                (v, event) -> {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            Log.d(TAG, String.valueOf(convertToJointNum(event.getX() / iv.getWidth(), event.getY() / iv.getHeight(), checkedNum)));

                            checkedNum = convertToJointNum(event.getX() / iv.getWidth(), event.getY() / iv.getHeight(), checkedNum);
                            vs.setProgress(default_position[checkedNum] + 900);

                            try {
                                InputStream istream = getResources().getAssets().open("joint_selected_plen2/s" + map[checkedNum] + ".png");
                                Bitmap bitmap = BitmapFactory.decodeStream(istream);
                                iv.setImageBitmap(bitmap);
                            } catch (IOException e) {
                                Log.d("Assets","Error");
                            }

                            updateToolbar();
                            break;
                    }
                    return false;
                }
        );

        homeButton.setOnClickListener(v -> {
            int progress = vs.getProgress();
            int value = map[checkedNum];
            String hexNum = String.format("%02x", value);
            String deg = String.format("%03x", (progress - 900) & 0xFFF);
            default_position[checkedNum] = progress - 900;
            String program = "$ho" + hexNum + deg;
            Log.d(TAG, program);
            EventBus.getDefault().post(new PlenConnectionService.WriteRequest(program));
        });

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
        updateToolbar();
    }

    @Override
    public void notifyWriteTxDataCompleted() {
        updateToolbar();
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

    @UiThread
    void updateToolbar() {
        // mToolbar.setTitle(R.string.app_name);
        mToolbar.setTitle(getString(R.string.toolbar_text, map[checkedNum]));
        mToolbar.getMenu().clear();
        mToolbar.inflateMenu(R.menu.menu_main);

        mToolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_search_plen) {
                mPresenter.disconnectPlen();
                mPresenter.startScan();
            } else if (id == R.id.action_licenses) {
                mFragmentManager.ifPresent(m -> OpenSourceLicensesDialogFragment_.builder().build()
                        .show(m, OSS_LICENSES_DIALOG));
            }
            return true;
        });
    }
}