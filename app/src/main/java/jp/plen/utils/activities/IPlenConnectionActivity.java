package jp.plen.utils.activities;

import android.support.annotation.NonNull;

import java.util.List;

public interface IPlenConnectionActivity {

    void notifyBluetoothUnavailable();

    void notifyLocationUnavailable();

    void notifyPlenScanning();

    void notifyPlenScanCancel();

    void notifyPlenScanComplete(@NonNull List<String> addresses);

    void notifyPlenConnectionChanged(boolean connected, boolean now);

    void notifyWriteTxDataCompleted();

    void notifyConnectionError(@NonNull Throwable e);
}
