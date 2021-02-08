package io.bluetrace.opentrace.herald;

import android.content.Context;

import com.vmware.herald.sensor.Device;
import com.vmware.herald.sensor.ble.BLEDevice;
import com.vmware.herald.sensor.ble.BLESensorConfiguration;
import com.vmware.herald.sensor.ble.BLETimer;
import com.vmware.herald.sensor.ble.BLETimerDelegate;
import com.vmware.herald.sensor.data.ConcreteSensorLogger;
import com.vmware.herald.sensor.data.SensorLogger;
import com.vmware.herald.sensor.datatype.Data;
import com.vmware.herald.sensor.datatype.Int8;
import com.vmware.herald.sensor.datatype.LegacyPayloadData;
import com.vmware.herald.sensor.datatype.PayloadData;
import com.vmware.herald.sensor.datatype.PayloadTimestamp;
import com.vmware.herald.sensor.datatype.RSSI;
import com.vmware.herald.sensor.datatype.UInt16;
import com.vmware.herald.sensor.payload.DefaultPayloadDataSupplier;

import java.util.ArrayList;
import java.util.List;

import io.bluetrace.opentrace.BuildConfig;
import io.bluetrace.opentrace.idmanager.TempIDManager;
import io.bluetrace.opentrace.idmanager.TemporaryID;
import io.bluetrace.opentrace.protocol.v2.BlueTraceV2;
import io.bluetrace.opentrace.protocol.v2.V2WriteRequestPayload;
import io.bluetrace.opentrace.services.BluetoothMonitoringService;
import io.bluetrace.opentrace.streetpass.CentralDevice;

public class BluetracePayloadDataSupplier extends DefaultPayloadDataSupplier implements BLETimerDelegate {
    private final SensorLogger logger = new ConcreteSensorLogger("Herald", "BluetracePayloadDataSupplier");
    private final String deviceModel = android.os.Build.MODEL;
    private final Context context;
    private final BLETimer timer;
    private final TempIDManager tempIDManager;
    private TemporaryID tempId = null;
    private long tempIdLastUpdatedAtMillis = 0;

    public BluetracePayloadDataSupplier(final Context context) {
        this.context = context;
        this.timer = new BLETimer(context);
        this.timer.add(this);
        this.tempIDManager = TempIDManager.INSTANCE;
        updateTempId();
    }

    // MARK: - BLETimerDelegate

    @Override
    public void bleTimer(long currentTimeMillis) {
        if (currentTimeMillis - tempIdLastUpdatedAtMillis > 2000) {
            updateTempId();
        }
    }

    // Update tempID at regular intervals for use by payload data supplier
    private synchronized void updateTempId() {
        final TemporaryID newTempId = tempIDManager.retrieveTemporaryID(context);
        if (newTempId != null && !equals(tempId, newTempId)) {
            logger.debug("tempId updated (from={},to={})",
                    (tempId == null ? tempId : tempId.getTempID()),
                    newTempId.getTempID());
            tempId = newTempId;
        }
    }

    private final static boolean equals(TemporaryID a, TemporaryID b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null || a.getTempID() == null || b.getTempID() == null) {
            return false;
        }
        return a.getTempID().equals(b.getTempID()) &&
                a.getStartTime() == b.getStartTime() &&
                a.getExpiryTime() == b.getExpiryTime();
    }

    // MARK: - PayloadDataSupplier

    @Override
    public PayloadData payload(PayloadTimestamp payloadTimestamp, Device device) {
        if (tempId == null || tempId.getTempID() == null) {
            logger.fault("payload, missing tempId");
            return null;
        }
        // Get device TX power, or use 0
        final UInt16 txPower = (device instanceof BLEDevice && ((BLEDevice) device).txPower() != null ?
                new UInt16(((BLEDevice) device).txPower().value) : new UInt16(0));
        // Get device RSSI, or use 0
        final Int8 rssi = (device instanceof BLEDevice && ((BLEDevice) device).rssi() != null ?
                new Int8(((BLEDevice) device).rssi().value) : new Int8(0));
        // Get Herald encoded Bluetrace payload
        final BluetracePayload bluetracePayload = new BluetracePayload(tempId.getTempID(), deviceModel, txPower, rssi);
        return bluetracePayload.heraldPayloadData();
    }


    @Override
    public List<PayloadData> payload(Data data) {
        final List<PayloadData> payloads = new ArrayList<>();
        int index = 0;
        while (true) {
            final UInt16 innerPayloadLength = data.uint16(index + 5);
            if (innerPayloadLength == null) {
                break;
            }
            final Data extractedPayload = data.subdata(index, 7 + innerPayloadLength.value);
            if (extractedPayload == null || extractedPayload.value.length == 0) {
                break;
            }
            payloads.add(new PayloadData(extractedPayload.value));
            index += extractedPayload.value.length;
        }
        return payloads;
    }

    @Override
    public LegacyPayloadData legacyPayload(PayloadTimestamp timestamp, Device device) {
        if (tempId == null || tempId.getTempID() == null) {
            logger.fault("payload, missing tempId");
            return null;
        }
        if (!(device instanceof BLEDevice)) {
            return null;
        }
        final BLEDevice bleDevice = (BLEDevice) device;
        final RSSI rssi = bleDevice.rssi();
        if (rssi == null) {
            return null;
        }
        try {
            final V2WriteRequestPayload dataToWrite = new V2WriteRequestPayload(
                    2,
                    tempId.getTempID(),
                    BuildConfig.ORG,
                    new CentralDevice(deviceModel, null),
                    bleDevice.rssi().value
            );
            final byte[] encodedData = dataToWrite.getPayload();
            final LegacyPayloadData legacyPayloadData = new LegacyPayloadData(BLESensorConfiguration.interopOpenTraceServiceUUID, encodedData);
            return legacyPayloadData;
        } catch (Throwable e) {
            return null;
        }
    }
}
