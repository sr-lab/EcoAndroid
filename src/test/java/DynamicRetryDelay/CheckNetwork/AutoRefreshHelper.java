package DynamicRetryDelay.CheckNetwork;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.*;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.util.Log;
//import org.jamienicol.episodes.RefreshShowUtil;
//import org.jamienicol.episodes.db.ShowsProvider;
//import org.jamienicol.episodes.db.ShowsTable;

public class AutoRefreshHelper
        implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static final String TAG = AutoRefreshHelper.class.getName();

    private static final String KEY_PREF_AUTO_REFRESH_ENABLED =
            "pref_auto_refresh_enabled";
    private static final String KEY_PREF_AUTO_REFRESH_PERIOD =
            "pref_auto_refresh_period";

    private static final String KEY_LAST_AUTO_REFRESH_TIME =
            "last_auto_refresh_time";

    private static AutoRefreshHelper instance;

    private final Context context;

    public AutoRefreshHelper(Context context) {
        this.context = context;

   //     preferences = PreferenceManager.getDefaultSharedPreferences(context);
   //     preferences.registerOnSharedPreferenceChangeListener(this);
    }

    public static synchronized AutoRefreshHelper getInstance(Context context) {
        if (instance == null) {
            instance = new AutoRefreshHelper(context);
        }
        return instance;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        if (key.equals(KEY_PREF_AUTO_REFRESH_ENABLED)) {
            onAutoRefreshEnabledChanged();
        } else if (key.equals(KEY_PREF_AUTO_REFRESH_PERIOD)) {
            onAutoRefreshPeriodChanged();
        }
    }

    private void onAutoRefreshEnabledChanged() {
        rescheduleAlarm();
    }

    private void onAutoRefreshPeriodChanged() {
        rescheduleAlarm();
    }

    private boolean getAutoRefreshEnabled() {
       // return preferences.getBoolean(KEY_PREF_AUTO_REFRESH_ENABLED, false);
        return true;
    }

    private long getAutoRefreshPeriod() {
      //  final String hours = preferences.getString(KEY_PREF_AUTO_REFRESH_PERIOD, "0");
        final String hours  = "";

        // convert hours to milliseconds
        return Long.parseLong(hours) * 60 * 60 * 1000;
    }

    private long getPrevAutoRefreshTime() {
    //    return preferences.getLong(KEY_LAST_AUTO_REFRESH_TIME, 0);
        return (long) 0.0;
    }

    private void setPrevAutoRefreshTime(long time) {
      //  final SharedPreferences.Editor editor = preferences.edit();
        final SharedPreferences.Editor editor = null;
        editor.putLong(KEY_LAST_AUTO_REFRESH_TIME, time);
        editor.commit();
    }


    public void rescheduleAlarm() {

        final AlarmManager alarmManager =
                (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

        final Intent intent =
                new Intent(context, AutoRefreshHelper.Service.class);
        final PendingIntent pendingIntent =
                PendingIntent.getService(context, 0, intent, 0);

        if (getAutoRefreshEnabled() && getAutoRefreshPeriod() != 0) {
            final long alarmTime =
                    getPrevAutoRefreshTime() + getAutoRefreshPeriod();

            Log.i(TAG, String.format("Scheduling auto refresh alarm for %d.", alarmTime));

            alarmManager.set(AlarmManager.RTC,
                    alarmTime,
                    pendingIntent);
        } else {
            Log.i(TAG, "Cancelling auto refresh alarm.");

            alarmManager.cancel(pendingIntent);
        }
    }

    public static class Service
            extends IntentService
    {
        private static final String TAG = Service.class.getName();

        public Service() {
            super(Service.class.getName());
        }

        @Override
        protected void onHandleIntent(Intent intent) {
            Log.i(TAG, "Refreshing all shows.");

            final ContentResolver contentResolver = getContentResolver();
            final Cursor cursor = getShowsCursor(contentResolver);

            while (cursor.moveToNext()) {
             // final int showIdColumnIndex = cursor.getColumnIndexOrThrow(ShowsTable.COLUMN_ID);
                final int showIdColumnIndex = 0;
                final int showId = cursor.getInt(showIdColumnIndex);
             // RefreshShowUtil.refreshShow(showId, contentResolver);
            }

            final AutoRefreshHelper helper =  AutoRefreshHelper.getInstance(getApplicationContext());

            helper.setPrevAutoRefreshTime(System.currentTimeMillis());
            helper.rescheduleAlarm();
        }

        private static Cursor getShowsCursor(ContentResolver contentResolver) {
            final String[] projection = {
               //     ShowsTable.COLUMN_ID
            };

           // final Cursor cursor =
           //         contentResolver.query(ShowsProvider.CONTENT_URI_SHOWS,
           //                 projection,
           //                 null,
           //                 null,
           //                 null);

         //   return cursor;
            return null;
        }
    }

    public static class BootReceiver
            extends BroadcastReceiver
    {
        private static final String TAG = BootReceiver.class.getName();

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
                Log.i(TAG, "Boot received.");

                // ensure that the auto-refresh alarm is scheduled.
                AutoRefreshHelper.getInstance(context.getApplicationContext())
                        .rescheduleAlarm();
            }
        }
    }
}