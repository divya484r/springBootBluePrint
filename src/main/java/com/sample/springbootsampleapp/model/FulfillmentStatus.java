package com.sample.springbootsampleapp.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Modified by vjayas on 5/21/18.
 */
@AllArgsConstructor
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "fulfillmentStatus")
public class FulfillmentStatus {
    @XmlElement(required = true)
    private String shipFromLocation;
    @XmlElement(required = true)
    private String sellerOrganizationCode;
    @XmlElement(required = true)
    private String transactionReference;
    @XmlElement(required = true)
    private String fulfillmentRequestNumber;
    @XmlElement
    private String externalOrderNumber;
    @XmlElement(required = true)
    private String orderType;
    @XmlElement
    private String customerOrderNumber;
    @XmlElement(required = true)
    private String creationDate;
    @XmlElement(required = true)
    private String userId;
    @Setter
    @Getter
    @XmlElement
    private String externalDeliveryNumber;
    @XmlElement
    private String messageID;
    @XmlElement
    private String transactionDate;
    @XmlElement(required = true)
    private boolean isCompleteFRUpdate;
    @XmlElement(required = true)
    private String fulfillmentStatus;

    @XmlElement(name = "lines")
    private List<FulfillmentStatus.Lines> lines;
    @XmlElement(name = "containerDetails", required = true)
    private List<FulfillmentStatus.ContainerDetails> containerDetails;

    public List<FulfillmentStatus.ContainerDetails> getContainerDetails() {
        return containerDetails;
    }

    public void setContainerDetails(List<FulfillmentStatus.ContainerDetails> containerDetails) {
        this.containerDetails = containerDetails;
    }

    private String workOrderNumber;

    public String getExternalOrderNumber() {
        return externalOrderNumber;
    }

    public void setExternalOrderNumber(String externalOrderNumber) {
        this.externalOrderNumber = externalOrderNumber;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getCustomerOrderNumber() {
        return customerOrderNumber;
    }

    public void setCustomerOrderNumber(String customerOrderNumber) {
        this.customerOrderNumber = customerOrderNumber;
    }

    public String getWorkOrderNumber() {
        return workOrderNumber;
    }

    public void setWorkOrderNumber(String workOrderNumber) {
        this.workOrderNumber = workOrderNumber;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMessageID() {
        return messageID;
    }

    public void setMessageID(String messageID) {
        this.messageID = messageID;
    }

    public String getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(String transactionDate) {
        this.transactionDate = transactionDate;
    }

    public boolean isCompleteFRUpdate() {
        return isCompleteFRUpdate;
    }

    public void setCompleteFRUpdate(boolean completeFRUpdate) {
        isCompleteFRUpdate = completeFRUpdate;
    }

    public String getFulfillmentStatus() {
        return fulfillmentStatus;
    }

    public void setFulfillmentStatus(String fulfillmentStatus) {
        this.fulfillmentStatus = fulfillmentStatus;
    }

    public List<FulfillmentStatus.Lines> getLines() {
        return lines;
    }

    public void setLines(List<FulfillmentStatus.Lines> lines) {
        this.lines = lines;
    }

    public String getShipFromLocation() {
        return shipFromLocation;
    }

    public void setShipFromLocation(String shipFromLocation) {
        this.shipFromLocation = shipFromLocation;
    }

    public String getSellerOrganizationCode() {
        return sellerOrganizationCode;
    }

    public void setSellerOrganizationCode(String sellerOrganizationCode) {
        this.sellerOrganizationCode = sellerOrganizationCode;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getFulfillmentRequestNumber() {
        return fulfillmentRequestNumber;
    }

    public void setFulfillmentRequestNumber(String fulfillmentRequestNumber) {
        this.fulfillmentRequestNumber = fulfillmentRequestNumber;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ContainerDetails {
        private List<FulfillmentStatus.ContainerDetails.ContainerDetail> containerDetail;

        public List<FulfillmentStatus.ContainerDetails.ContainerDetail> getContainerDetail() {
            if (containerDetail == null) {
                containerDetail = new ArrayList<FulfillmentStatus.ContainerDetails.ContainerDetail>();
            }
            return this.containerDetail;
        }

        @AllArgsConstructor
        @NoArgsConstructor
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "", propOrder = {"dimensions"})
        public static class ContainerDetail {
            @XmlElement(required = true)
            private FulfillmentStatus.ContainerDetails.ContainerDetail.Dimensions dimensions;

            public FulfillmentStatus.ContainerDetails.ContainerDetail.Dimensions getDimensions() {
                return dimensions;
            }

            public void setDimensions(FulfillmentStatus.ContainerDetails.ContainerDetail.Dimensions value) {
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
    // @XmlType(name = "", propOrder = { "containers", "orderLineKey", "lineNumber", "serialNumbers" })
    public static class Lines {

        @XmlElement(name = "line")
        private List<FulfillmentStatus.Lines.Line> line;

        public List<FulfillmentStatus.Lines.Line> getLine() {
            return line;
        }

        public void setLine(List<FulfillmentStatus.Lines.Line> line) {
            this.line = line;
        }

        @AllArgsConstructor
        @NoArgsConstructor
        @XmlAccessorType(XmlAccessType.FIELD)
        public static class Line {
            @XmlElement(name = "orderLineIdentifier")
            private String orderLineIdentifier;
            @XmlElement(name = "lineNumber")
            private String lineNumber;
            @XmlElement(name = "transactionDate")
            private String transactionDate;
            @XmlElement(name = "confirmedQuantity")
            private String confirmedQuantity;
            @XmlElement(name = "rejectedQuantity")
            private String rejectedQuantity;
            @XmlElement(name = "reasonText")
            private String reasonText;
            @Getter
            @Setter
            @XmlElement(name = "externalDeliveryLineNumber")
            private String externalDeliveryLineNumber;
            @XmlElement(name = "orderLineStatus")
            private String orderLineStatus;
            @XmlElement(name = "storageType")
            private String storageType;
            @XmlElement(name = "containers")
            private List<FulfillmentStatus.Lines.Line.Containers> containers;

            public String getReasonText() {
                return reasonText;
            }

            public void setReasonText(String reasonText) {
                this.reasonText = reasonText;
            }

            public List<FulfillmentStatus.Lines.Line.Containers> getContainers() {
                return containers;
            }

            public void setContainers(List<FulfillmentStatus.Lines.Line.Containers> containers) {
                this.containers = containers;
            }

            public String getStorageType() {
                return storageType;
            }

            public void setStorageType(String storageType) {
                this.storageType = storageType;
            }

            @AllArgsConstructor
            @NoArgsConstructor
            @XmlAccessorType(XmlAccessType.FIELD)
            @XmlType(name = "", propOrder = {"container"})
            public static class Containers {
                @XmlElement(name = "container")
                private List<FulfillmentStatus.Lines.Line.Containers.Container> container;

                public List<FulfillmentStatus.Lines.Line.Containers.Container> getContainer() {
                    return container;
                }

                public void setContainer(List<FulfillmentStatus.Lines.Line.Containers.Container> container) {
                    this.container = container;
                }

                @AllArgsConstructor
                @NoArgsConstructor
                @XmlAccessorType(XmlAccessType.FIELD)
                @XmlType(name = "", propOrder = {"number", "quantity", "trackingNumber", "universalProductCode"})
                public static class Container {
                    private String number;
                    private String quantity;
                    private String trackingNumber;
                    private int universalProductCode;

                    public String getNumber() {
                        return number;
                    }

                    public void setNumber(String value) {
                        this.number = value;
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

                }
            }

            public String getOrderLineStatus() {
                return orderLineStatus;
            }

            public void setOrderLineStatus(String orderLineStatus) {
                this.orderLineStatus = orderLineStatus;
            }

            public String getOrderLineIdentifier() {
                return orderLineIdentifier;
            }

            public void setOrderLineIdentifier(String orderLineIdentifier) {
                this.orderLineIdentifier = orderLineIdentifier;
            }

            public String getLineNumber() {
                return lineNumber;
            }

            public void setLineNumber(String lineNumber) {
                this.lineNumber = lineNumber;
            }

            public String getTransactionDate() {
                return transactionDate;
            }

            public void setTransactionDate(String transactionDate) {
                this.transactionDate = transactionDate;
            }

            public String getConfirmedQuantity() {
                return confirmedQuantity;
            }

            public void setConfirmedQuantity(String confirmedQuantity) {
                this.confirmedQuantity = confirmedQuantity;
            }

            public String getRejectedQuantity() {
                return rejectedQuantity;
            }

            public void setRejectedQuantity(String rejectedQuantity) {
                this.rejectedQuantity = rejectedQuantity;
            }

        }
    }
}
