/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * <p/>
 * Contributors:
 * Denis Solonenko - initial API and implementation
 ******************************************************************************/
package tw.tib.financisto.service;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import tw.tib.financisto.R;
import tw.tib.financisto.activity.AccountWidget;
import tw.tib.financisto.activity.MassOpActivity;
import tw.tib.financisto.blotter.BlotterFilter;
import tw.tib.financisto.filter.WhereFilter;
import tw.tib.financisto.db.DatabaseAdapter;
import tw.tib.financisto.model.Transaction;
import tw.tib.financisto.model.TransactionInfo;
import tw.tib.financisto.model.TransactionStatus;
import tw.tib.financisto.utils.NotificationUtils;

import static tw.tib.financisto.utils.MyPreferences.getSmsTransactionStatus;
import static tw.tib.financisto.utils.MyPreferences.shouldSaveSmsToTransactionNote;

public class FinancistoService extends JobIntentService {
    public static final int JOB_ID = 1000;

    public static final String ACTION_SCHEDULE_ALL = "tw.tib.financisto.SCHEDULE_ALL";
    public static final String ACTION_NEW_TRANSACTION_SMS = "tw.tib.financisto.NEW_TRANSACTON_SMS";
    public static final String SMS_TRANSACTION_NUMBER = "SMS_TRANSACTION_NUMBER";
    public static final String SMS_TRANSACTION_BODY = "SMS_TRANSACTION_BODY";

    private static final int RESTORED_NOTIFICATION_ID = 0;

    private DatabaseAdapter db;
    private RecurrenceScheduler scheduler;
    private SmsTransactionProcessor smsProcessor;
    private IntentTransactionProcessor intentTransactionProcessor;

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, FinancistoService.class, JOB_ID, work);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        db = new DatabaseAdapter(this);
        db.open();
        scheduler = new RecurrenceScheduler(db);
        smsProcessor = new SmsTransactionProcessor(db);
        intentTransactionProcessor = new IntentTransactionProcessor(db);
    }

    @Override
    public void onDestroy() {
        if (db != null) {
            db.close();
        }
        super.onDestroy();
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        final String action = intent.getAction();
        if (action != null) {
            switch (action) {
                case ACTION_SCHEDULE_ALL:
                    scheduleAll();
                    break;
                case ACTION_NEW_TRANSACTION_SMS:
                    processSmsTransaction(intent);
                    break;
            }
        }
    }

    private void processSmsTransaction(Intent intent) {
        String number = intent.getStringExtra(SMS_TRANSACTION_NUMBER);
        String body = intent.getStringExtra(SMS_TRANSACTION_BODY);
        if (number != null && body != null) {
            Transaction t = smsProcessor.createTransactionBySms(number, body, getSmsTransactionStatus(this),
                    shouldSaveSmsToTransactionNote(this));
            if (t != null) {
                TransactionInfo transactionInfo = db.getTransactionInfo(t.id);
                Notification notification = createSmsTransactionNotification(transactionInfo, number);
                NotificationUtils.notifyUser(this, notification, (int) t.id);
                AccountWidget.updateWidgets(this);
            }
        }
    }

    private void scheduleAll() {
        int restoredTransactionsCount = scheduler.scheduleAll(this);
        if (restoredTransactionsCount > 0) {
            NotificationUtils.notifyUser(this, createRestoredNotification(restoredTransactionsCount), RESTORED_NOTIFICATION_ID);
        }
    }

    private Notification createRestoredNotification(int count) {
        long when = System.currentTimeMillis();
        String text = getString(R.string.scheduled_transactions_have_been_restored, count);
        String contentTitle = getString(R.string.scheduled_transactions_restored);

        Intent notificationIntent = new Intent(this, MassOpActivity.class);
        WhereFilter filter = new WhereFilter("");
        filter.eq(BlotterFilter.STATUS, TransactionStatus.RS.name());
        filter.toIntent(notificationIntent);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, NotificationChannelService.TRANSACTIONS_CHANNEL)
                .setContentIntent(contentIntent)
                .setSmallIcon(R.mipmap.a_icon_notify)
                .setWhen(when)
                .setTicker(text)
                .setContentText(text)
                .setContentTitle(contentTitle)
                .setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .build();
    }

    private Notification createSmsTransactionNotification(TransactionInfo t, String number) {
        String tickerText = getString(R.string.new_sms_transaction_text, number);
        String contentTitle = getString(R.string.new_sms_transaction_title, number);
        String text = t.getNotificationContentText(this);

        return NotificationUtils.generateNotification(this, t, tickerText, contentTitle, text);
    }

}
