package com.sample.springbootsampleapp.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@AllArgsConstructor
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"actualShipmentDate", "billOfLading", "orderClassification",
        "fulfillmentRequestNumber", "sellerOrganizationCode", "shipFromLocation", "shippingMethod",
        "standardizedActualShippingMethod", "shortShipFlag",
        "splitShipFlag", "standardCarrierAlphaCode", "workOrderNumber", "countryOfOrigin", "shipTo", "lines",
        "containerDetails"})
@XmlRootElement(name = "shipment")

public class Shipment {
    @XmlElement(required = true)
    // @XmlSchemaType(name = "dateTime")
    private String actualShipmentDate;
    @XmlElement(required = true)
    private String billOfLading;
    @XmlElement(required = true)
    private String orderClassification;
    /* @XmlElement(required = true)
     private String orderType;*/
    @XmlElement(required = true)
    private String fulfillmentRequestNumber;
    @XmlElement(required = true)
    private String sellerOrganizationCode;
    @XmlElement(required = true)
    private String shipFromLocation;
    @XmlElement(required = true)
    private String shippingMethod;
    @XmlElement(required = true)
    private String standardizedActualShippingMethod;
    @XmlElement(required = true)
    private boolean shortShipFlag;
    @XmlElement(required = true)
    private String splitShipFlag;
    @XmlElement(required = true)
    private String standardCarrierAlphaCode;
    @XmlElement(required = true)
    private String workOrderNumber;
    @XmlElement(required = true)
    private String countryOfOrigin;
    @XmlElement(required = true)
    private Shipment.ShipTo shipTo;
    @XmlElement(required = true)
    private Shipment.Lines lines;
    @XmlElement(required = true)
    private Shipment.ContainerDetails containerDetails;

    public String getActualShipmentDate() {
        // return new Date(actualShipmentDate.getTime());
        return actualShipmentDate;
    }

    public void setActualShipmentDate(String actualShipmentDate) {
        // this.actualShipmentDate = new Date(actualShipmentDate.getTime());
        this.actualShipmentDate = actualShipmentDate;
    }

    public String getBillOfLading() {
        return billOfLading;
    }

    public void setBillOfLading(String value) {
        this.billOfLading = value;
    }

    public String getOrderClassification() {
        return orderClassification;
    }

    public void setOrderClassification(String value) {
        this.orderClassification = value;
    }

/*    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String value) {
        this.orderType = value;
    }*/

    public String getFulfillmentRequestNumber() {
        return fulfillmentRequestNumber;
    }

    public void setFulfillmentRequestNumber(String value) {
        this.fulfillmentRequestNumber = value;
    }

    public String getSellerOrganizationCode() {
        return sellerOrganizationCode;
    }

    public void setSellerOrganizationCode(String value) {
        this.sellerOrganizationCode = value;
    }

    public String getShipFromLocation() {
        return shipFromLocation;
    }

    public void setShipFromLocation(String value) {
        this.shipFromLocation = value;
    }

    public String getShippingMethod() {
        return shippingMethod;
    }

    public void setShippingMethod(String value) {
        this.shippingMethod = value;
    }

    public String getStandardizedActualShippingMethod() {
        return standardizedActualShippingMethod;
    }

    public void setStandardizedActualShippingMethod(String value) {
        this.standardizedActualShippingMethod = value;
    }

    public boolean getShortShipFlag() {
        return shortShipFlag;
    }

    public void setShortShipFlag(boolean shortShipFlag) {
        this.shortShipFlag = shortShipFlag;
    }

    public String getSplitShipFlag() {
        return splitShipFlag;
    }

    public void setSplitShipFlag(String value) {
        this.splitShipFlag = value;
    }

    public String getStandardCarrierAlphaCode() {
        return standardCarrierAlphaCode;
    }

    public void setStandardCarrierAlphaCode(String value) {
        this.standardCarrierAlphaCode = value;
    }

    public String getWorkOrderNumber() {
        return workOrderNumber;
    }

    public void setWorkOrderNumber(String value) {
        this.workOrderNumber = value;
    }

    public String getCountryOfOrigin() {
        return countryOfOrigin;
    }

    public void setCountryOfOrigin(String value) {
        this.countryOfOrigin = value;
    }

    public Shipment.ShipTo getShipTo() {
        return shipTo;
    }

    public void setShipTo(Shipment.ShipTo value) {
        this.shipTo = value;
    }

    public Shipment.Lines getLines() {
        return lines;
    }

    public void setLines(Shipment.Lines value) {
        this.lines = value;
    }

    public Shipment.ContainerDetails getContainerDetails() {
        return containerDetails;
    }

    public void setContainerDetails(Shipment.ContainerDetails value) {
        this.containerDetails = value;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {"containerDetail"})
    public static class ContainerDetails {
        private List<Shipment.ContainerDetails.ContainerDetail> containerDetail;

        public List<Shipment.ContainerDetails.ContainerDetail> getContainerDetail() {
            if (containerDetail == null) {
                containerDetail = new ArrayList<Shipment.ContainerDetails.ContainerDetail>();
            }
            return this.containerDetail;
        }

        public void setContainerDetail(List<Shipment.ContainerDetails.ContainerDetail> containerDetail) {
            this.containerDetail = containerDetail;
        }

        @AllArgsConstructor
        @NoArgsConstructor
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {"dimensions"})
        public static class ContainerDetail {
            @XmlElement(required = true)
            private Shipment.ContainerDetails.ContainerDetail.Dimensions dimensions;

            public Shipment.ContainerDetails.ContainerDetail.Dimensions getDimensions() {
                return dimensions;
            }

            public void setDimensions(Shipment.ContainerDetails.ContainerDetail.Dimensions value) {
                this.dimensions = value;
            }

            @AllArgsConstructor
            @NoArgsConstructor
            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {"grossWeight", "height", "length", "linearUnitOfMeasure", "netWeight",
                    "number", "weightUnitOfMeasure", "width"})
            public static class Dimensions {
                private double grossWeight;
                private double height;
                private double length;
                @XmlElement(required = true)
                private String linearUnitOfMeasure;
                private double netWeight;
                @XmlElement(required = true)
                private String number;
                @XmlElement(required = true)
                private String weightUnitOfMeasure;
                private double width;

                public double getGrossWeight() {
                    return grossWeight;
                }

                public void setGrossWeight(double value) {
                    this.grossWeight = value;
                }

                public double getHeight() {
                    return height;
                }

                public void setHeight(double value) {
                    this.height = value;
                }

                public double getLength() {
                    return length;
                }

                public void setLength(double value) {
                    this.length = value;
                }

                public String getLinearUnitOfMeasure() {
                    return linearUnitOfMeasure;
                }

                public void setLinearUnitOfMeasure(String value) {
                    this.linearUnitOfMeasure = value;
                }

                public double getNetWeight() {
                    return netWeight;
                }

                public void setNetWeight(double value) {
                    this.netWeight = value;
                }

                public String getNumber() {
                    return number;
                }

                public void setNumber(String value) {
                    this.number = value;
                }

                public String getWeightUnitOfMeasure() {
                    return weightUnitOfMeasure;
                }

                public void setWeightUnitOfMeasure(String value) {
                    this.weightUnitOfMeasure = value;
                }

                public double getWidth() {
                    return width;
                }

                public void setWidth(double value) {
                    this.width = value;
                }

            }

        }

    }

    @AllArgsConstructor
    @NoArgsConstructor
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {"line"})
    public static class Lines {
        @XmlElement(required = true)
        private List<Shipment.Lines.Line> line;

        public List<Shipment.Lines.Line> getLine() {
            return line;
        }

        public void setLine(List<Shipment.Lines.Line> value) {
            this.line = value;
        }

        @AllArgsConstructor
        @NoArgsConstructor
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {"containers", "orderLineIdentifier", "lineNumber", "serialNumbers"})
        public static class Line {
            @XmlElement(required = true)
            private Shipment.Lines.Line.Containers containers;
            @XmlElement(required = true)
            private BigInteger orderLineIdentifier;
            private String lineNumber;
            @XmlElement(required = true)
            private Shipment.Lines.Line.SerialNumbers serialNumbers;

            public Shipment.Lines.Line.Containers getContainers() {
                return containers;
            }

            public void setContainers(Shipment.Lines.Line.Containers value) {
                this.containers = value;
            }

            public BigInteger getOrderLineIdentifier() {
                return orderLineIdentifier;
            }

            public void setOrderLineIdentifier(BigInteger value) {
                this.orderLineIdentifier = value;
            }

            public String getLineNumber() {
                return lineNumber;
            }

            public void setLineNumber(String value) {
                this.lineNumber = value;
            }

            public Shipment.Lines.Line.SerialNumbers getSerialNumbers() {
                return serialNumbers;
            }

            public void setSerialNumbers(Shipment.Lines.Line.SerialNumbers value) {
                this.serialNumbers = value;
            }

            @AllArgsConstructor
            @NoArgsConstructor
            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {"container"})
            public static class Containers {
                private List<Shipment.Lines.Line.Containers.Container> container;

                public List<Shipment.Lines.Line.Containers.Container> getContainer() {
                    if (container == null) {
                        container = new ArrayList<Shipment.Lines.Line.Containers.Container>();
                    }
                    return this.container;
                }

                @AllArgsConstructor
                @NoArgsConstructor
                @XmlAccessorType(XmlAccessType.FIELD)
                @XmlType(name = "", propOrder = {"number", "externalDeliveryDetails", "quantity", "trackingNumber", "universalProductCode"})
                public static class Container {
                    @XmlElement(required = true)
                    private String number;
                    @XmlElement(required = true)
                    private Shipment.Lines.Line.Containers.Container.ExternalDeliveryDetails externalDeliveryDetails;
                    private String quantity;
                    @XmlElement(required = true)
                    private String trackingNumber;
                    private int universalProductCode;

                    public String getNumber() {
                        return number;
                    }

                    public void setNumber(String value) {
                        this.number = value;
                    }

                    public Shipment.Lines.Line.Containers.Container.ExternalDeliveryDetails getExternalDeliveryDetails() {
                        return externalDeliveryDetails;
                    }

                    public void setExternalDeliveryDetails(Shipment.Lines.Line.Containers.Container.ExternalDeliveryDetails value) {
                        this.externalDeliveryDetails = value;
                    }

                    public String getQuantity() {
                        return quantity;
                    }

                    public void setQuantity(String value) {
                        this.quantity = value;
                    }

                    public String getTrackingNumber() {
                        return trackingNumber;
                    }

                    public void setTrackingNumber(String value) {
                        this.trackingNumber = value;
                    }

                    public int getUniversalProductCode() {
                        return universalProductCode;
                    }

                    public void setUniversalProductCode(int value) {
                        this.universalProductCode = value;
                    }

                    @AllArgsConstructor
                    @NoArgsConstructor
                    @XmlAccessorType(XmlAccessType.FIELD)
                    @XmlType(name = "", propOrder = {"externalDeliveryDetail"})
                    public static class ExternalDeliveryDetails {
                        private List<Shipment.Lines.Line.Containers.Container.ExternalDeliveryDetails.ExternalDeliveryDetail> externalDeliveryDetail;

                        public List<Shipment.Lines.Line.Containers.Container.ExternalDeliveryDetails.ExternalDeliveryDetail> getExternalDeliveryDetail() {
                            if (externalDeliveryDetail == null) {
                                externalDeliveryDetail = new ArrayList<Shipment.Lines.Line.Containers.Container.ExternalDeliveryDetails.ExternalDeliveryDetail>();
                            }
                            return this.externalDeliveryDetail;
                        }


                        @AllArgsConstructor
                        @NoArgsConstructor
                        @XmlAccessorType(XmlAccessType.FIELD)
                        @XmlType(name = "", propOrder = {"externalDeliveryNumber", "externalDeliveryLineNumber", "quantity"})
                        public static class ExternalDeliveryDetail {
                            @XmlElement(required = true)
                            private String externalDeliveryNumber;
                            private String externalDeliveryLineNumber;
                            private String quantity;

                            public String getExternalDeliveryNumber() {
                                return externalDeliveryNumber;
                            }

                            public void setExternalDeliveryNumber(String value) {
                                this.externalDeliveryNumber = value;
                            }

                            public String getExternalDeliveryLineNumber() {
                                return externalDeliveryLineNumber;
                            }

                            public void setExternalDeliveryLineNumber(String value) {
                                this.externalDeliveryLineNumber = value;
                            }

                            public String getQuantity() {
                                return quantity;
                            }

                            public void setQuantity(String value) {
                                this.quantity = value;
                            }
                        }


                    }

                }


            }

            @AllArgsConstructor
            @NoArgsConstructor
            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {"serialNumber"})
            public static class SerialNumbers {
                private List<String> serialNumber;

                public List<String> getSerialNumber() {
                    if (serialNumber == null) {
                        serialNumber = new ArrayList<String>();
                    }
                    return this.serialNumber;
                }

                public void setSerialNumber(List<String> serialNumber) {
                    this.serialNumber = serialNumber;
                }

            }

        }

    }

    @AllArgsConstructor
    @NoArgsConstructor
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {"address", "contactInformation", "recipient"})
    public static class ShipTo {
        @XmlElement(required = true)
        private Shipment.ShipTo.Address address;
        @XmlElement(required = true)
        private Shipment.ShipTo.ContactInformation contactInformation;
        @XmlElement(required = true)
        private Shipment.ShipTo.Recipient recipient;

        public Shipment.ShipTo.Address getAddress() {
            return address;
        }

        public void setAddress(Shipment.ShipTo.Address value) {
            this.address = value;
        }

        public Shipment.ShipTo.ContactInformation getContactInformation() {
            return contactInformation;
        }

        public void setContactInformation(Shipment.ShipTo.ContactInformation value) {
            this.contactInformation = value;
        }

        public Shipment.ShipTo.Recipient getRecipient() {
            return recipient;
        }

        public void setRecipient(Shipment.ShipTo.Recipient value) {
            this.recipient = value;
        }

        @AllArgsConstructor
        @NoArgsConstructor
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {"address1", "address2", "address3", "address4", "address5", "shipToAddressId",
                "city", "shipToCountry", "pickUpLocation", "pickUpLocationType", "state", "zipCode"})
        public static class Address {
            @XmlElement(required = true)
            private String address1;
            @XmlElement(required = true)
            private String address2;
            @XmlElement(required = true)
            private String address3;
            @XmlElement(required = true)
            private String address4;
            @XmlElement(required = true)
            private String address5;
            private int shipToAddressId;
            @XmlElement(required = true)
            private String city;
            @XmlElement(required = true)
            private String shipToCountry;
            @XmlElement(required = true)
            private String pickUpLocation;
            @XmlElement(required = true)
            private String pickUpLocationType;
            @XmlElement(required = true)
            private String state;
            private short zipCode;

            public String getAddress1() {
                return address1;
            }

            public void setAddress1(String value) {
                this.address1 = value;
            }

            public String getAddress2() {
                return address2;
            }

            public void setAddress2(String value) {
                this.address2 = value;
            }

            public String getAddress3() {
                return address3;
            }

            public void setAddress3(String value) {
                this.address3 = value;
            }

            public String getAddress4() {
                return address4;
            }

            public void setAddress4(String value) {
                this.address4 = value;
            }

            public String getAddress5() {
                return address5;
            }

            public void setAddress5(String value) {
                this.address5 = value;
            }

            public int getAddressId() {
                return shipToAddressId;
            }

            public void setShipToAddressId(int value) {
                this.shipToAddressId = value;
            }

            public String getCity() {
                return city;
            }

            public void setCity(String value) {
                this.city = value;
            }

            public String getShipToCountry() {
                return shipToCountry;
            }

            public void setShipToCountry(String value) {
                this.shipToCountry = value;
            }

            public String getPickUpLocation() {
                return pickUpLocation;
            }

            public void setPickUpLocation(String value) {
                this.pickUpLocation = value;
            }

            public String getPickUpLocationType() {
                return pickUpLocationType;
            }

            public void setPickUpLocationType(String value) {
                this.pickUpLocationType = value;
            }

            public String getState() {
                return state;
            }

            public void setState(String value) {
                this.state = value;
            }

            public short getZipCode() {
                return zipCode;
            }

            public void setZipCode(short value) {
                this.zipCode = value;
            }
        }

        @AllArgsConstructor
        @NoArgsConstructor
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {"dayPhone", "email", "eveningPhoneNumber", "shipToCountry"})
        public static class ContactInformation {
            private long dayPhone;
            @XmlElement(required = true)
            private String email;
            private long eveningPhoneNumber;
            private byte shipToCountry;

            public long getDayPhone() {
                return dayPhone;
            }

            public void setDayPhone(long value) {
                this.dayPhone = value;
            }

            public String getEmail() {
                return email;
            }

            public void setEmail(String value) {
                this.email = value;
            }

            public long getEveningPhoneNumber() {
                return eveningPhoneNumber;
            }

            public void setEveningPhoneNumber(long value) {
                this.eveningPhoneNumber = value;
            }

            public byte getShipToCountry() {
                return shipToCountry;
            }

            public void setShipToCountry(byte value) {
                this.shipToCountry = value;
            }
        }

        @AllArgsConstructor
        @NoArgsConstructor
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {"firstName", "lastName", "middleName"})
        public static class Recipient {
            @XmlElement(required = true)
            private String firstName;
            @XmlElement(required = true)
            private String lastName;
            @XmlElement(required = true)
            private String middleName;

            public String getFirstName() {
                return firstName;
            }

            public void setFirstName(String value) {
                this.firstName = value;
            }

            public String getLastName() {
                return lastName;
            }

            public void setLastName(String value) {
                this.lastName = value;
            }

            public String getMiddleName() {
                return middleName;
            }

            public void setMiddleName(String value) {
                this.middleName = value;
            }

        }

    }

}
