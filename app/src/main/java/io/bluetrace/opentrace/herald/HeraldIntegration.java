package io.bluetrace.opentrace.herald;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.HttpsCallableResult;
import com.vmware.herald.sensor.DefaultSensorDelegate;
import com.vmware.herald.sensor.SensorArray;
import com.vmware.herald.sensor.ble.BLESensorConfiguration;
import com.vmware.herald.sensor.data.ConcreteSensorLogger;
import com.vmware.herald.sensor.data.SensorLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

import io.bluetrace.opentrace.BuildConfig;
import io.bluetrace.opentrace.MainActivity;
import io.bluetrace.opentrace.idmanager.TemporaryID;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class HeraldIntegration extends DefaultSensorDelegate {
    private final static SensorLogger logger = new ConcreteSensorLogger("Herald", "HeraldIntegration");
    private final SensorArray sensorArray;

    /// Enable test mode for evaluation with fair efficacy formula
    public static boolean testMode = true;

    public HeraldIntegration(final Context context) {
        if (HeraldIntegration.testMode) {
            logger.info("test mode enabled");
        }
        // Enable interoperability with devices running legacy OpenTrace only protocol
        BLESensorConfiguration.interopOpenTraceEnabled = true;
        if (BLESensorConfiguration.interopOpenTraceEnabled) {
            BLESensorConfiguration.interopOpenTraceServiceUUID = UUID.fromString(BuildConfig.BLE_SSID);
            BLESensorConfiguration.interopOpenTracePayloadCharacteristicUUID = UUID.fromString(BuildConfig.V2_CHARACTERISTIC_ID);
            logger.info("interop enabled (protocol=OpenTrace,serviceUUID={},characteristicUUID={})",
                    BLESensorConfiguration.interopOpenTraceServiceUUID,
                    BLESensorConfiguration.interopOpenTracePayloadCharacteristicUUID);
        }
        // Enable OpenTrace protocol running over Herald transport
        sensorArray = new SensorArray(context, new BluetracePayloadDataSupplier(context));
        sensorArray.add(this);
        // Enable test mode instrumentation
        if (HeraldIntegration.testMode) {
            sensorArray.add(new HeraldTestInstrumentation(context));
        }
    }

    // MARK: - OpenTrace replacement functions for normal operation

    public void bluetraceMonitoringService_actionStart() {
        logger.debug("bluetraceMonitoringService_actionStart");
        sensorArray.start();
    }

    public void bluetraceMonitoringService_actionStop() {
        logger.debug("bluetraceMonitoringService_actionStop");
        sensorArray.stop();
    }

    public void bluetraceMonitoringService_teardown() {
        logger.debug("bluetraceMonitoringService_teardown");
        sensorArray.stop();
    }

    // MARK: - OpenTrace replacement functions for test mode operation

    private final static int permissionRequestCode = 1294839287;

    public static TemporaryID tempIDManager_retrieveTemporaryID() {
        return new TemporaryID(0, HeraldTestInstrumentation.payloadData.base64EncodedString(), Long.MAX_VALUE);
    }

    public static Task<HttpsCallableResult> tempIDManager_getTemporaryIDs() {
        // Task will always be successful because retrieveTemporaryID is being performed locally
        // using a fixed procedure, rather than relying on Firebase services.
        return new TempIDManager_GetTemporaryIDsTask();
    }

    /// Request all application permissions for sensor array, rather than via on-boarding process.
    public static void splashActivity_requestPermissions(final Activity activity) {
        // Check and request permissions
        final List<String> requiredPermissions = new ArrayList<>();
        requiredPermissions.add(Manifest.permission.BLUETOOTH);
        requiredPermissions.add(Manifest.permission.BLUETOOTH_ADMIN);
        requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        requiredPermissions.add(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            requiredPermissions.add(Manifest.permission.FOREGROUND_SERVICE);
        }
        final String[] requiredPermissionsArray = requiredPermissions.toArray(new String[0]);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            activity.requestPermissions(requiredPermissionsArray, permissionRequestCode);
        } else {
            ActivityCompat.requestPermissions(activity, requiredPermissionsArray, permissionRequestCode);
        }
    }

    /// Handle permission request results, then start main activity
    public static void splashActivity_onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults, final Activity activity) {
        if (requestCode == permissionRequestCode) {
            boolean permissionsGranted = true;
            for (int i = 0; i < permissions.length; i++) {
                final String permission = permissions[i];
                if (grantResults[i] != PERMISSION_GRANTED) {
                    logger.fault("Permission denied (permission=" + permission + ")");
                    permissionsGranted = false;
                } else {
                    logger.info("Permission granted (permission=" + permission + ")");
                }
            }

            if (!permissionsGranted) {
                logger.fault("Application does not have all required permissions to start (permissions=" + Arrays.asList(permissions) + ")");
            }

            logger.info("Starting main activity, bypassing on-boarding");
            activity.startActivity(new Intent(activity, MainActivity.class));
        }
    }

    // MARK: - Mock task for GetTemporaryIDs

    /// Task always succeeds as Firebase is being bypassed and temporary ID is
    /// obtained locally and deterministically
    private final static class TempIDManager_GetTemporaryIDsTask extends Task<HttpsCallableResult> {
        @Override
        public boolean isComplete() {
            return true;
        }

        @Override
        public boolean isSuccessful() {
            return true;
        }

        @Override
        public boolean isCanceled() {
            return false;
        }

        @Nullable
        @Override
        public HttpsCallableResult getResult() {
            return null;
        }

        @Nullable
        @Override
        public <X extends Throwable> HttpsCallableResult getResult(@NonNull Class<X> aClass) throws X {
            return null;
        }

        @Nullable
        @Override
        public Exception getException() {
            return null;
        }

        @NonNull
        @Override
        public Task<HttpsCallableResult> addOnSuccessListener(@NonNull OnSuccessListener<? super HttpsCallableResult> onSuccessListener) {
            return this;
        }

        @NonNull
        @Override
        public Task<HttpsCallableResult> addOnSuccessListener(@NonNull Executor executor, @NonNull OnSuccessListener<? super HttpsCallableResult> onSuccessListener) {
            return this;
        }

        @NonNull
        @Override
        public Task<HttpsCallableResult> addOnSuccessListener(@NonNull Activity activity, @NonNull OnSuccessListener<? super HttpsCallableResult> onSuccessListener) {
            return this;
        }

        @NonNull
        @Override
        public Task<HttpsCallableResult> addOnFailureListener(@NonNull OnFailureListener onFailureListener) {
            return this;
        }

        @NonNull
        @Override
        public Task<HttpsCallableResult> addOnFailureListener(@NonNull Executor executor, @NonNull OnFailureListener onFailureListener) {
            return this;
        }

        @NonNull
        @Override
        public Task<HttpsCallableResult> addOnFailureListener(@NonNull Activity activity, @NonNull OnFailureListener onFailureListener) {
            return this;
        }

        @NonNull
        @Override
        public Task<HttpsCallableResult> addOnCompleteListener(@NonNull OnCompleteListener<HttpsCallableResult> onCompleteListener) {
            onCompleteListener.onComplete(this);
            return this;
        }
    }

}
