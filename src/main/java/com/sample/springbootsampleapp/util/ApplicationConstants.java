package com.sample.springbootsampleapp.util;

public class ApplicationConstants {

    public static final String SHIP_CANCEL_ROUTE_ID = "ShipCancelUpdateRouter";
    public static final String SHIP_CANCEL_ROUTER = "direct:" + SHIP_CANCEL_ROUTE_ID;
    public static final String SHIP_CANCEL_ROUTE_DESCRIPTION = "Router to generate ship cancel NSP payload";

    public static final String SHIP_CONFIRM_ROUTE_ID = "ShipConfirmRouter";
    public static final String SHIP_CONFIRM_ROUTER = "direct:" + SHIP_CONFIRM_ROUTE_ID;
    public static final String SHIP_CONFIRM_ROUTE_DESCRIPTION = "Router to generate ship confirm NSP payload";

    public static final String NSP_ROUTE_ID = "PulseRouter";
    public static final String NSP_ROUTER = "direct:" + NSP_ROUTE_ID;
    public static final String NSP_ROUTE_DESCRIPTION = "Router to post messages to NSP";

    public static final String MESSAGE_ID = "messageID";
}
