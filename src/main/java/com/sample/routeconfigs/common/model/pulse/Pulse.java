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
public class Pulse {

    @NonNull
    private EventContext eventContext;

    @NonNull
    private EventData data;
}
