package com.az.payment.domain;

public enum ServiceType {
    INQUIRY,//no payment. TODO: should doesn't allow next step
    PAYMENT,//next step must only be CHECKSTATUS
    BOTH,//allow next step
    CHECKSTATUS,
    DATASOURCE
}
