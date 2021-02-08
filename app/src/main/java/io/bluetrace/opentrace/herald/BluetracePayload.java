package io.bluetrace.opentrace.herald;

import com.vmware.herald.sensor.datatype.Data;
import com.vmware.herald.sensor.datatype.Int8;
import com.vmware.herald.sensor.datatype.PayloadData;
import com.vmware.herald.sensor.datatype.UInt16;
import com.vmware.herald.sensor.datatype.UInt8;
import com.vmware.herald.sensor.payload.extended.ConcreteExtendedDataSectionV1;
import com.vmware.herald.sensor.payload.extended.ConcreteExtendedDataV1;

public class BluetracePayload {
    public final String tempId;
    public final String modelC;
    public final UInt16 txPower;
    public final Int8 rssi;
    public final static HeraldEnvelopHeader header =
            new HeraldEnvelopHeader(new UInt8(0x91), new UInt16(124), new UInt16(48));

    public BluetracePayload(String tempId, String modelC, UInt16 txPower, Int8 rssi) {
        this.tempId = tempId;
        this.modelC = modelC;
        this.txPower = txPower;
        this.rssi = rssi;
    }

    public PayloadData heraldPayloadData() {
        final PayloadData payloadData = new PayloadData();
        payloadData.append(BluetracePayload.header.data());
        final Data innerData = new Data();
        innerData.append(tempId, Data.StringLengthEncodingOption.UINT16);
        final ConcreteExtendedDataV1 extendedData = new ConcreteExtendedDataV1();
        extendedData.addSection(new UInt8(0x40), rssi);
        extendedData.addSection(new UInt8(0x41), txPower);
        extendedData.addSection(new UInt8(0x42), modelC);
        innerData.append(extendedData.payload());
        payloadData.append(new UInt16(innerData.value.length));
        payloadData.append(innerData);
        return payloadData;
    }

    public final static BluetracePayload parse(PayloadData heraldPayloadData) {
        if (!header.data().equals(heraldPayloadData.subdata(0, 5))) {
            return null;
        }
        final UInt16 tempIdLength = heraldPayloadData.uint16(7);
        if (tempIdLength == null)  {
            return null;
        }
        final Data.DecodedString decodedTempIdString = heraldPayloadData.string(7, Data.StringLengthEncodingOption.UINT16);
        final String decodedTempId = (decodedTempIdString == null || decodedTempIdString.value == null ? "" : decodedTempIdString.value);
        String modelC = "";
        Int8 rssi = new Int8(0);
        UInt16 txPower = new UInt16(0);
        final ConcreteExtendedDataV1 extendedData = new ConcreteExtendedDataV1(new PayloadData(heraldPayloadData.subdata(9+tempIdLength.value).value));
        for (ConcreteExtendedDataSectionV1 section : extendedData.getSections()) {
            switch (section.code.value) {
                case 0x40: {
                    final Int8 value = section.data.int8(0);
                    if (value != null) {
                        rssi = value;
                    }
                    break;
                }
                case 0x41: {
                    final UInt16 value = section.data.uint16(0);
                    if (value != null) {
                        txPower = value;
                    }
                    break;
                }
                case 0x42: {
                    final Data.DecodedString value = section.data.string(0);
                    if (value != null && value.value != null) {
                        modelC = value.value;
                    }
                    break;
                }
            }
        }
        final BluetracePayload bluetracePayload = new BluetracePayload(decodedTempId, modelC, txPower, rssi);
        return bluetracePayload;
    }
}
