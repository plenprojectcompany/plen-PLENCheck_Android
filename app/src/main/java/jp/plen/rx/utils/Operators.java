package jp.plen.rx.utils;

import android.support.annotation.NonNull;

import rx.Observable;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

public final class Operators {
    private Operators() {
    }

    @NonNull
    public static <T> Observable.Operator<T, T> composite(@NonNull CompositeSubscription compositeSubscription) {
        return subscriber -> {
            compositeSubscription.add(subscriber);
            subscriber.add(Subscriptions.create(() -> compositeSubscription.remove(subscriber)));
            return subscriber;
        };
    }
}
