package com.e2ew.auditor.aop.aspects.models;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuditLog {

    private Long id;
    private LocalDateTime dateLog;
    private Integer company;
    private String serverIp;
    private String idenType;
    private String identifier;
    private String consumerApp;
    private String consumerChannel;
    private String consumerHost;
    private Integer consumerPort;
    private String deviceAttributes;
    private String externalUsername;
    private String externalMessageId;
    private String externalSessionId;
    private String reason;
    private Double geolocLatitude;
    private Double geolocLongitude;
    private String destinationApp;
    private String service;
    private String internalMessageId;
    private String responseCode;
    private String responseDescription;
    private String deviceId;
    private Integer scoreRisk;
    private String internalUsername;
    private String e2inOut;
    private Integer counter;
    private Integer idRequest;
}
