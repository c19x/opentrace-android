package com.vmware.herald.sensor;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.functions.HttpsCallableResult;
import com.vmware.herald.sensor.SensorDelegate;
import com.vmware.herald.sensor.data.BatteryLog;
import com.vmware.herald.sensor.data.ConcreteSensorLogger;
import com.vmware.herald.sensor.data.ContactLog;
import com.vmware.herald.sensor.data.DetectionLog;
import com.vmware.herald.sensor.data.SensorLogger;
import com.vmware.herald.sensor.data.SensorLoggerLevel;
import com.vmware.herald.sensor.datatype.Data;
import com.vmware.herald.sensor.datatype.PayloadData;
import com.vmware.herald.sensor.datatype.Proximity;
import com.vmware.herald.sensor.datatype.ProximityMeasurementUnit;
import com.vmware.herald.sensor.datatype.SensorType;
import com.vmware.herald.sensor.datatype.TargetIdentifier;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/// Enables instrumentation of OpenTrace for evaluation with the Fair Efficacy Formula
public class FairEfficacyInstrumentation {
    // Parameters
    /// Log level for HERALD logger
    public static SensorLoggerLevel logLevel = SensorLoggerLevel.debug;
    /// Set test mode to TRUE for evaluation, FALSE for normal operation
    /// Evaluation mode will
    /// - use a fixed payload to enable testing
    /// - enable full offline operation without Firebase
    /// - bypass registration
    public static boolean testMode = true;

    // Internals
    private final static SensorLogger logger = new ConcreteSensorLogger("Sensor", "Data.FairEfficacyInstrumentation");
    private final static int permissionRequestCode = 1294839287;
    private final String deviceDescription = android.os.Build.MODEL + " (Android " + android.os.Build.VERSION.SDK_INT + ")";
    private final Queue<SensorDelegate> delegates = new ConcurrentLinkedQueue<>();
    public final PayloadData payloadData = generatePayloadData();


    public FairEfficacyInstrumentation(final Context context) {
        // Log contacts and battery usage
        delegates.add(new ContactLog(context, "contacts.csv"));
        delegates.add(new DetectionLog(context, "detection.csv", payloadData));
        new BatteryLog(context, "battery.csv");

        logger.info("DEVICE (payloadPrefix={},description={})", payloadData.shortName(), deviceDescription);
    }

    /// REQUIRED : Request application permissions for sensor operation.
    public static void requestPermissions(final Activity activity) {
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

    /// REQUIRED : Handle permission results.
    public static void onRequestPermissionsResult(final int requestCode, @NonNull final String[] permissions, @NonNull final int[] grantResults) {
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
        }
    }


    private final static PayloadData generatePayloadData() {
        // Get device specific identifier
        final String text = Build.MODEL + ":" + Build.BRAND;
        final int identifier = text.hashCode();
        // Convert identifier to data
        final ByteBuffer identifierByteBuffer = ByteBuffer.allocate(4);
        identifierByteBuffer.putInt(0, identifier);
        final Data identifierData = new Data(identifierByteBuffer.array());
        // Convert to payload
        final ByteBuffer payloadByteBuffer = ByteBuffer.allocate(3 + identifierData.value.length);
        payloadByteBuffer.position(3);
        payloadByteBuffer.put(identifierData.value);
        return new PayloadData(payloadByteBuffer.array());
    }

    // MARK:- Intrumentation functions

    public void instrument(final Long timestampMillis, final String base64EncodedPayload, final Integer rssi) {
        if (base64EncodedPayload == null) {
            return;
        }
        if (rssi == null) {
            return;
        }
        final TargetIdentifier targetIdentifier = new TargetIdentifier(base64EncodedPayload);
        final PayloadData payloadData = new PayloadData(base64EncodedPayload);
        final Proximity proximity = new Proximity(ProximityMeasurementUnit.RSSI, (double) rssi);
        final Date timestamp = (timestampMillis == null ? new Date() : new Date(timestampMillis));
        logger.debug("instrument (encounter,timestamp={},payload={},rssi={})", timestamp, payloadData.shortName(), rssi);
        for (SensorDelegate delegate : delegates) {
            delegate.sensor(SensorType.BLE, targetIdentifier);
            delegate.sensor(SensorType.BLE, payloadData, targetIdentifier);
            delegate.sensor(SensorType.BLE, proximity, targetIdentifier);
            delegate.sensor(SensorType.BLE, proximity, targetIdentifier, payloadData);
        }
    }


    // MARK:- Emulated task for bypassing Firebase

    public final static class EmulatedTask extends Task<HttpsCallableResult> {
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
