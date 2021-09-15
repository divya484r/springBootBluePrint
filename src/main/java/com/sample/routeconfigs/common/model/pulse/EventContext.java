package com.sample.routeconfigs.common.model.pulse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventContext {

    /**
     * Event context businessKeyValue
     */
    public static final String BUSINESS_KEY_VALUE = "EventContextBusinessKeyValue";

    /**
     * The business key name
     *
     * Example: "purchaseOrderNumber"
     */
    public static final String BUSINESS_KEY_NAME = "EventContextBusinessKeyName";

    /**
     * Corresponds to a topic name.
     *
     * Example: "ce_fmgr_shiprequest"
     */
    public static final String NAME = "EventContextName";

    /**
     * How many days Pulse will retain the data.
     *
     * Should be set on the header as a string.
     */
    public static final String RETENTION_DAYS = "EventContextRetentionDays";

    /**
     * Event context type
     */
    public static final String TYPE = "EventContextType";

    /**
     * Event context version
     */
    public static final String VERSION = "EventContextVersion";

    /**
     *  The keys and values will be set on the Pulse EventContext filterMap.
     *
     * Not required as pre-set header
     */
    public static final String EVENT_CONTEXT_FILTER_MAP_ENTRY_SETS = "EventContextFilterMapEntrySets";

    private String version;

    private String type;

    private String name;

    private String businessKeyName;

    private String businessKeyValue;

    private String date;

    private String retentionDays;

    private Map<String, Object> filterMap = new HashMap<>(1);

    private Map<String, Object> metaData = new HashMap<>(1);

    /**
     * Enum specifying valid Pulse EventContext versions
     */
    public enum Version {
        V1_0("1.0");

        private String value;

        private Version(String value) {
            this.value = value;
        }

        public String value() {
            return this.value;
        }
    }
}
