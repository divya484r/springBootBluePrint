package com.sample.springbootsampleapp.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "fulfillmentStatus")

public class FulfillmentStatusTarget {

    @XmlElement(name = "fulfillmentRequestNumber", required = true)
    private String fulfillmentRequestNumber;
    @XmlElement(name = "transactionReference")
    private String transactionReference;
    @XmlElement(name = "creationDate")
    private String creationDate;
    @XmlElement(name = "shipFromLocation")
    private String shipFromLocation;
    @XmlElement(name = "sellerOrganizationCode")
    private String sellerOrganizationCode;
    @XmlElement(name = "customerOrderNumber")
    private String customerOrderNumber;
    @XmlElement(name = "orderType", required = true)
    private String orderType;
    @XmlElement(name = "lines")
    private List<Lines> lines;
    @XmlElement(name = "transactionDate")
    private String transactionDate;
    @XmlElement(name = "messageID", required = true)
    private String messageID;

    public String getFulfillmentRequestNumber() {
        return fulfillmentRequestNumber;
    }

    public void setFulfillmentRequestNumber(String fulfillmentRequestNumber) {
        this.fulfillmentRequestNumber = fulfillmentRequestNumber;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
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

    public String getCustomerOrderNumber() {
        return customerOrderNumber;
    }

    public void setCustomerOrderNumber(String customerOrderNumber) {
        this.customerOrderNumber = customerOrderNumber;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public List<Lines> getLines() {
        return lines;
    }

    public void setLines(List<Lines> lines) {
        this.lines = lines;
    }

    public String getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(String transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getMessageID() {
        return messageID;
    }

    public void setMessageID(String messageID) {
        this.messageID = messageID;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "lines", propOrder = {"line"})
    public static class Lines {
        @XmlElement(name = "line")
        private List<Line> line;

        public List<Line> getLine() {
            return line;
        }

        public void setLine(List<Line> line) {
            this.line = line;
        }

        @AllArgsConstructor
        @NoArgsConstructor
        @XmlAccessorType(XmlAccessType.FIELD)
        @XmlType(name = "line", propOrder = {"lineNumber", "orderLineKey", "productCode", "sizeCode", "universalProductCode", "orderLineStatus", "rejectedQuantity", "confirmedQuantity"})
        public static class Line {
            @XmlElement(name = "lineNumber")
            private String lineNumber;
            @XmlElement(name = "orderLineKey")
            private String orderLineKey;
            @XmlElement(name = "productCode", required = true)
            private String productCode;
            @XmlElement(name = "sizeCode")
            private String sizeCode;
            @XmlElement(name = "universalProductCode")
            private int universalProductCode;
            @XmlElement(name = "orderLineStatus", required = true)
            private String orderLineStatus;
            @XmlElement(name = "rejectedQuantity")
            private String rejectedQuantity;
            @XmlElement(name = "confirmedQuantity")
            private String confirmedQuantity;


            public String getLineNumber() {
                return lineNumber;
            }

            public void setLineNumber(String lineNumber) {
                this.lineNumber = lineNumber;
            }

            public String getOrderLineKey() {
                return orderLineKey;
            }

            public void setOrderLineKey(String orderLineKey) {
                this.orderLineKey = orderLineKey;
            }

            public String getProductCode() {
                return productCode;
            }

            public void setProductCode(String productCode) {
                this.productCode = productCode;
            }

            public String getSizeCode() {
                return sizeCode;
            }

            public void setSizeCode(String sizeCode) {
                this.sizeCode = sizeCode;
            }

            public int getUniversalProductCode() {
                return universalProductCode;
            }

            public void setUniversalProductCode(int universalProductCode) {
                this.universalProductCode = universalProductCode;
            }

            public String getOrderLineStatus() {
                return orderLineStatus;
            }

            public void setOrderLineStatus(String orderLineStatus) {
                this.orderLineStatus = orderLineStatus;
            }

            public String getRejectedQuantity() {
                return rejectedQuantity;
            }

            public void setRejectedQuantity(String rejectedQuantity) {
                this.rejectedQuantity = rejectedQuantity;
            }

            public String getConfirmedQuantity() {
                return confirmedQuantity;
            }

            public void setConfirmedQuantity(String confirmedQuantity) {
                this.confirmedQuantity = confirmedQuantity;
            }
        }
    }
}




