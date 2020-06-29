package Cache.PassiveProviderLocation;

import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

class ActionBarActivity {
}

public class BaseActivity extends ActionBarActivity {

    private static final String LOCATION_SERVICE = "" ;
    private final boolean TEST = true;

    // protected Tracker tracker;
    protected SharedPreferences prefs;

    // Location variables
    protected LocationManager locationManager;
    protected LocationListener locationListener;
    protected Location currentLocation;
    protected double currentLatitude;
    protected double currentLongitude;
    protected double noteLatitude;
    protected double noteLongitude;

    private void setLocationManager() {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                updateLocation(location);
            }

            public void onStatusChanged(String provider, int status,
                                        Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60000, 50, locationListener);
    }
    void updateLocation(Location location){
        currentLocation = location;
        currentLatitude = currentLocation.getLatitude();
        currentLongitude = currentLocation.getLongitude();
    }

    LocationManager getSystemService(String str) {
        return null;
    }

}
