package com.e2ew.auditor.aop.aspects.enums;

import lombok.Getter;

@Getter
public enum AppName {

    HB_AFILIATION("HB_AFILIATION"),
    HB_REQUEST("HB_REQUEST"),
    HB_TRANSACTIONS("HB_TRANSACTIONS"),
    HB_QUERYS("HB_QUERYS"),
    HB_HOST("HB_HOST")
    ;

    private final String serviceName;

    private AppName(String serviceName) {
        this.serviceName = serviceName;
    }
}
