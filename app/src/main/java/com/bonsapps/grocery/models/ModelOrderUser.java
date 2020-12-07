package com.bonsapps.grocery.models;

public class ModelOrderUser {
    String orderId, orderTime, orderCost, orderStatus, orderBy, orderTo;

    public ModelOrderUser() {
    }

    public ModelOrderUser(String orderId, String orderTime, String orderCost, String orderStatus, String orderBy, String orderTo) {
        this.orderId = orderId;
        this.orderTime = orderTime;
        this.orderCost = orderCost;
        this.orderStatus = orderStatus;
        this.orderBy = orderBy;
        this.orderTo = orderTo;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getOrderTime() {
        return orderTime;
    }

    public void setOrderTime(String orderTime) {
        this.orderTime = orderTime;
    }

    public String getOrderCost() {
        return orderCost;
    }

    public void setOrderCost(String orderCost) {
        this.orderCost = orderCost;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public String getOrderTo() {
        return orderTo;
    }

    public void setOrderTo(String orderTo) {
        this.orderTo = orderTo;
    }
}
