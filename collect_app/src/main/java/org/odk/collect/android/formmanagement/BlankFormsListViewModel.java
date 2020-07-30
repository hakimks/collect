package org.odk.collect.android.formmanagement;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import org.odk.collect.android.formmanagement.matchexactly.ServerFormsSynchronizer;
import org.odk.collect.android.formmanagement.matchexactly.SyncStatusRepository;
import org.odk.collect.android.notifications.Notifier;
import org.odk.collect.android.openrosa.api.FormApiException;
import org.odk.collect.android.preferences.GeneralKeys;
import org.odk.collect.android.preferences.PreferencesProvider;
import org.odk.collect.async.Scheduler;

import javax.inject.Inject;

public class BlankFormsListViewModel extends ViewModel {

    private final Application application;
    private final Scheduler scheduler;
    private final SyncStatusRepository syncRepository;
    private final ServerFormsSynchronizer serverFormsSynchronizer;
    private final PreferencesProvider preferencesProvider;
    private final Notifier notifier;

    public BlankFormsListViewModel(Application application, Scheduler scheduler, SyncStatusRepository syncRepository, ServerFormsSynchronizer serverFormsSynchronizer, PreferencesProvider preferencesProvider, Notifier notifier) {
        this.application = application;
        this.scheduler = scheduler;
        this.syncRepository = syncRepository;
        this.serverFormsSynchronizer = serverFormsSynchronizer;
        this.preferencesProvider = preferencesProvider;
        this.notifier = notifier;
    }

    public boolean isSyncingAvailable() {
        return isMatchExactlyEnabled();
    }

    public LiveData<Boolean> isSyncing() {
        return syncRepository.isSyncing();
    }

    public LiveData<Boolean> isOutOfSync() {
        return syncRepository.isOutOfSync();
    }

    public void syncWithServer() {
        if (!syncRepository.startSync()) {
            return;
        }

        scheduler.immediate(() -> {
            try {
                serverFormsSynchronizer.synchronize();
                syncRepository.finishSync(true);
            } catch (FormApiException e) {
                syncRepository.finishSync(false);
                notifier.onSyncFailure(e);
            }

            return null;
        }, ignored -> { });
    }

    private boolean isMatchExactlyEnabled() {
        FormUpdateMode formUpdateMode = FormUpdateMode.parse(application, preferencesProvider.getGeneralSharedPreferences().getString(GeneralKeys.KEY_FORM_UPDATE_MODE, null));
        return formUpdateMode == FormUpdateMode.MATCH_EXACTLY;
    }

    public static class Factory implements ViewModelProvider.Factory {

        private final Application application;
        private final Scheduler scheduler;
        private final SyncStatusRepository syncRepository;
        private final ServerFormsSynchronizer serverFormsSynchronizer;
        private final PreferencesProvider preferencesProvider;
        private final Notifier notifier;

        @Inject
        public Factory(Application application, Scheduler scheduler, SyncStatusRepository syncRepository, ServerFormsSynchronizer serverFormsSynchronizer, PreferencesProvider preferencesProvider, Notifier notifier) {
            this.application = application;
            this.scheduler = scheduler;
            this.syncRepository = syncRepository;
            this.serverFormsSynchronizer = serverFormsSynchronizer;
            this.preferencesProvider = preferencesProvider;
            this.notifier = notifier;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new BlankFormsListViewModel(application, scheduler, syncRepository, serverFormsSynchronizer, preferencesProvider, notifier);
        }
    }
}
