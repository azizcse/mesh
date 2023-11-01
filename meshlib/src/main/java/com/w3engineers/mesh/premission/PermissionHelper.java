package com.w3engineers.mesh.premission;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.w3engineers.ext.strom.util.collections.CollectionUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides permission related information
 */
public class PermissionHelper {

    /**
     * @param context
     * @param permissions
     * @return true if provided list empty or all item has granted permissions
     */
    public boolean hasPermissions(Context context, List<String> permissions) {
        return !CollectionUtil.hasItem(permissions) || !CollectionUtil.hasItem(getNotGrantedPermissions
                (context, permissions));
    }

    public List<String> getNotGrantedPermissions(Context context, List<String> permissions) {

        if(CollectionUtil.hasItem(permissions)) {

            List<String> permissionsNotGranted = new ArrayList<>();

            for(String permission : permissions) {

                if(ContextCompat.checkSelfPermission(context, permission) != PackageManager.
                        PERMISSION_GRANTED) {

                    permissionsNotGranted.add(permission);
                }
            }

            return permissionsNotGranted;

        }

        return null;

    }

    public List<String> getShouldShowPermissions(Activity activity, List<String> permissions) {

        if(CollectionUtil.hasItem(permissions)) {

            List<String> permissionsNotGranted = new ArrayList<>();

            for(String permission : permissions) {

                if(ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {

                    permissionsNotGranted.add(permission);
                }
            }

            return permissionsNotGranted;

        }

        return null;
    }

    public boolean isLocationProviderEnabled(Context context) {
        if(context == null) {
            return false;
        }

        LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return manager != null && manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

}
