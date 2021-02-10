package io.bluetrace.opentrace.herald;

import android.content.Context;
import android.os.Build;

import com.vmware.herald.sensor.DefaultSensorDelegate;
import com.vmware.herald.sensor.SensorDelegate;
import com.vmware.herald.sensor.ble.BLESensorConfiguration;
import com.vmware.herald.sensor.ble.ConcreteBLEDatabase;
import com.vmware.herald.sensor.data.BatteryLog;
import com.vmware.herald.sensor.data.ConcreteSensorLogger;
import com.vmware.herald.sensor.data.ContactLog;
import com.vmware.herald.sensor.data.DetectionLog;
import com.vmware.herald.sensor.data.EventTimeIntervalLog;
import com.vmware.herald.sensor.data.SensorLogger;
import com.vmware.herald.sensor.data.StatisticsLog;
import com.vmware.herald.sensor.datatype.Data;
import com.vmware.herald.sensor.datatype.ImmediateSendData;
import com.vmware.herald.sensor.datatype.Int32;
import com.vmware.herald.sensor.datatype.LegacyPayloadData;
import com.vmware.herald.sensor.datatype.Location;
import com.vmware.herald.sensor.datatype.PayloadData;
import com.vmware.herald.sensor.datatype.Proximity;
import com.vmware.herald.sensor.datatype.SensorState;
import com.vmware.herald.sensor.datatype.SensorType;
import com.vmware.herald.sensor.datatype.TargetIdentifier;
import com.vmware.herald.sensor.datatype.TimeInterval;

import java.util.ArrayList;
import java.util.List;

public class HeraldTestInstrumentation extends DefaultSensorDelegate {
    private final SensorLogger logger = new ConcreteSensorLogger("Herald", "HeraldTestInstrumentation");
    private final List<SensorDelegate> delegates = new ArrayList<>();
    public final static PayloadData payloadData = deviceSpecificPayloadData();

    public HeraldTestInstrumentation(final Context context) {
        logger.debug("device (os=android{},model={})", android.os.Build.VERSION.SDK_INT, Build.MODEL);
        delegates.add(new ContactLog(context, "contacts.csv"));
        delegates.add(new StatisticsLog(context, "statistics.csv", payloadData));
        delegates.add(new DetectionLog(context, "detection.csv", payloadData));
        new BatteryLog(context, "battery.csv");
        if (BLESensorConfiguration.payloadDataUpdateTimeInterval.value != TimeInterval.never.value) {
            delegates.add(new EventTimeIntervalLog(context, "statistics_didRead.csv", payloadData, EventTimeIntervalLog.EventType.read));
        }
        final String deviceDescription = android.os.Build.MODEL + " (Android " + android.os.Build.VERSION.SDK_INT + ")";
        logger.info("DEVICE (payloadPrefix={},description={})", payloadData.shortName(), deviceDescription);
    }

    /// Generate unique and consistent device identifier for testing detection and tracking
    private final static PayloadData deviceSpecificPayloadData() {
        // Generate unique identifier based on phone name
        final String text = Build.MODEL + ":" + Build.BRAND + ":android:" + android.os.Build.VERSION.SDK_INT;
        final int value = text.hashCode();
        // Build HERALD compatible payload data
        final PayloadData payloadData = new PayloadData();
        payloadData.append(new Data((byte) 0, 3));
        payloadData.append(new Int32(value));
        return payloadData;
    }

    /// Parse payload data to distinguish legacy OpenTrace payload (JSON) and
    /// Herald encoded OpenTrace payload (binary).
    private final static PayloadData parsePayloadData(final PayloadData payloadData) {
        if (payloadData instanceof LegacyPayloadData) {
            return payloadData;
        }
        final BluetracePayload bluetracePayload = BluetracePayload.parse(payloadData);
        if (bluetracePayload == null || bluetracePayload.tempId == null) {
            return null;
        }
        final PayloadData embeddedPayloadData = new PayloadData(bluetracePayload.tempId);
        return embeddedPayloadData;
    }

    // MARK: - SensorDelegate


    @Override
    public void sensor(SensorType sensor, final SensorState didUpdateState) {
        logger.debug("sensor={},didUpdateState={}", sensor.name(), didUpdateState.name());
        for (final SensorDelegate delegate : delegates) {
            delegate.sensor(sensor, didUpdateState);
        }
    }

    @Override
    public void sensor(SensorType sensor, TargetIdentifier didDetect) {
        logger.debug("sensor={},didDetect={}", sensor.name(), didDetect);
        for (final SensorDelegate delegate : delegates) {
            delegate.sensor(sensor, didDetect);
        }
    }

    @Override
    public void sensor(SensorType sensor, PayloadData didRead, TargetIdentifier fromTarget) {
        final PayloadData payloadData = parsePayloadData(didRead);
        if (payloadData == null) {
            logger.fault("sensor={},didRead={},fromTarget={},error=failedToParse", sensor.name(), didRead.base64EncodedString(), fromTarget);
            return;
        }
        logger.debug("sensor={},didRead={},fromTarget={}", sensor.name(), payloadData.shortName(), fromTarget);
        for (final SensorDelegate delegate : delegates) {
            delegate.sensor(sensor, payloadData, fromTarget);
        }
    }

    @Override
    public void sensor(SensorType sensor, Proximity didMeasure, TargetIdentifier fromTarget) {
        logger.debug("sensor={},didMeasure={},fromTarget={}", sensor.name(), didMeasure.description(), fromTarget);
        for (final SensorDelegate delegate : delegates) {
            delegate.sensor(sensor, didMeasure, fromTarget);
        }
    }

    @Override
    public void sensor(SensorType sensor, List<PayloadData> didShare, TargetIdentifier fromTarget) {
        final List<PayloadData> didSharePayloadData = new ArrayList<>(didShare.size());
        for (PayloadData didRead : didShare) {
            final PayloadData payloadData = parsePayloadData(didRead);
            didSharePayloadData.add(payloadData != null ? payloadData : didRead);
        }
        logger.debug("sensor={},didShare={},fromTarget={}", sensor.name(), didSharePayloadData, fromTarget);
        for (final SensorDelegate delegate : delegates) {
            delegate.sensor(sensor, didSharePayloadData, fromTarget);
        }
    }

    @Override
    public void sensor(SensorType sensor, Location didVisit) {
        logger.debug("sensor={},didVisit={}", sensor.name(), didVisit.description());
        for (final SensorDelegate delegate : delegates) {
            delegate.sensor(sensor, didVisit);
        }
    }

    @Override
    public void sensor(SensorType sensor, ImmediateSendData didReceive, TargetIdentifier fromTarget) {
        logger.debug("sensor={},didReceive={},fromTarget={}", sensor.name(), didReceive.data.description(), fromTarget);
        for (final SensorDelegate delegate : delegates) {
            delegate.sensor(sensor, didReceive, fromTarget);
        }
    }

    @Override
    public void sensor(SensorType sensor, Proximity didMeasure, TargetIdentifier fromTarget, PayloadData withPayload) {
        final PayloadData payloadData = parsePayloadData(withPayload);
        if (payloadData == null) {
            logger.fault("sensor={},didMeasure={},fromTarget={},withPayload={},error=failedToParse", sensor.name(), didMeasure.description(), fromTarget, withPayload.base64EncodedString());
            return;
        }
        logger.debug("sensor={},didMeasure={},fromTarget={},withPayload={}", sensor.name(), didMeasure.description(), fromTarget, payloadData.shortName());
        for (final SensorDelegate delegate : delegates) {
            delegate.sensor(sensor, didMeasure, fromTarget, payloadData);
        }
    }
}
