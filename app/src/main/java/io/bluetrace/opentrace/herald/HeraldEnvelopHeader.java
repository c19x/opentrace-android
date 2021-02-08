package io.bluetrace.opentrace.herald;

import com.vmware.herald.sensor.datatype.Data;
import com.vmware.herald.sensor.datatype.UInt16;
import com.vmware.herald.sensor.datatype.UInt8;

public class HeraldEnvelopHeader {
    public final UInt8 protocolAndVersion;
    public final UInt16 countryCode;
    public final UInt16 stateCode;

    public HeraldEnvelopHeader(UInt8 protocolAndVersion, UInt16 countryCode, UInt16 stateCode) {
        this.protocolAndVersion = protocolAndVersion;
        this.countryCode = countryCode;
        this.stateCode = stateCode;
    }

    public Data data() {
        final Data data = new Data();
        data.append(protocolAndVersion);
        data.append(countryCode);
        data.append(stateCode);
        return data;
    }
}