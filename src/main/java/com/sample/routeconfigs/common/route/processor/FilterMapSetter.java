package com.sample.routeconfigs.common.route.processor;

import com.sample.routeconfigs.common.model.pulse.EventContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Handler;
import org.apache.camel.Message;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Sets a filter on the Pulse event context filter map.
 *
 * Uses an already existing map stored in the Camel header {@link EventContext#EVENT_CONTEXT_FILTER_MAP_ENTRY_SETS}
 * or, if the map does not already exist, creates a new map and stores it in that header.
 *
 * To use the FilterMapSetter in a Camel route:
 *
 *      private static final String SET_FILTER_KEY1_VALUE1 = String.format(FilterMapSetter.SET_FILTER, "key1", "value1");
 *
 *      ...
 *
 *      from(uri)
 *          ...
 *          .bean(FilterMapSetter.class, SET_FILTER_KEY1_VALUE1)
 *          ...
 *          .end();
 *
 * To set multiple filters, repeat the bean method call:
 *
 *      from(uri)
 *          ...
 *          .bean(FilterMapSetter.class, SET_FILTER_KEY1_VALUE1)
 *          .bean(FilterMapSetter.class, SET_FILTER_KEY2_VALUE2)
 *          ...
 *          .end();
 *
 */
@Slf4j
@Component
public class FilterMapSetter {

    public static final String SET_FILTER = "setFilter(${exchange}, %s, %s)";

    @Handler
    public void setFilter(Exchange exchange, String key, String value) {

        validate(key, value);

        Message message = exchange.getIn();
        String businessKeyName = (String) message.getHeader(EventContext.BUSINESS_KEY_NAME);
        String businessKeyValue = (String) message.getHeader(EventContext.BUSINESS_KEY_VALUE);

        Map<String, Object> filterMap = (Map<String, Object>) message.getHeader(EventContext.EVENT_CONTEXT_FILTER_MAP_ENTRY_SETS);
        if (filterMap == null) {
            filterMap = new HashMap<>();
        }
        filterMap.put(key, value);
        message.setHeader(EventContext.EVENT_CONTEXT_FILTER_MAP_ENTRY_SETS, filterMap);

        log.info(new StringBuilder().append("Successfully set [\"").append(key).append("\": \"").append(value)
                .append("\"] on filter map for ").append(businessKeyName).append(" = ").append(businessKeyValue).toString());
    }

    private void validate(String key, String value) {
        if(StringUtils.isEmpty(key)) {
            throw new IllegalArgumentException("A non-empty filter map key must be provided.");
        }
        if(StringUtils.isEmpty(value)) {
            throw new IllegalArgumentException("A non-empty filter map value must be provided.");
        }
    }
}
