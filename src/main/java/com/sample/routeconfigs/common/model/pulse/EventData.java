package com.sample.routeconfigs.common.model.pulse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventData {

    public static final String ENCODING = "EventDataEncoding";
    public static final String ENCODED_DATA = "EncodedEventData";
    public static final String CONTENT_TYPE = "EventDataContentType";

    private String contentType;

    private String encoding;

    @NonNull
    private String value;
}
