package com.e2ew.auditor.aop.aspects.repository.impl;

import java.sql.Timestamp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.e2ew.auditor.aop.aspects.models.AuditLog;
import com.e2ew.auditor.aop.aspects.repository.AuditorRepository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class AuditorRepositoryImpl implements AuditorRepository {

    private final JdbcTemplate eicJdbcTemplate;

    @Override
    public void auditLogMessageE2di(AuditLog auditLog) {

        String sql = """
                INSERT INTO LOG_MESSAGE_E2DI
                (ID, DATE_LOG, COMPANY, SERVER_IP, IDEN_TYPE, IDENTIFIER, CONSUMER_APP, CONSUMER_CHANNEL, CONSUMER_HOST,
                CONSUMER_PORT, DEVICE_ATTRIBUTES, EXTERNAL_USERNAME, EXTERNAL_MESSAGE_ID, EXTERNAL_SESSION_ID, REASON,
                GEOLOC_LATITUDE, GEOLOC_LONGITUDE, DESTINATION_APP, SERVICE, INTERNAL_MESSAGE_ID, RESPONSE_CODE,
                RESPONSE_DESCRIPTION, DEVICE_ID, SCORE_RISK, INTERNAL_USERNAME, E2IN_OUT, COUNTER, ID_REQUEST)
                VALUES(LOG_MESSAGE_E2DI_SEQ.nextval, ? , ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try{

            eicJdbcTemplate.update(sql, Timestamp.valueOf(auditLog.getDateLog()), auditLog.getCompany(),
                    auditLog.getServerIp(),
                    auditLog.getIdenType(), auditLog.getIdentifier(), auditLog.getConsumerApp(),
                    auditLog.getConsumerChannel(),
                    auditLog.getConsumerHost(), auditLog.getConsumerPort(), auditLog.getDeviceAttributes(),
                    auditLog.getExternalUsername(), auditLog.getExternalMessageId(), auditLog.getExternalSessionId(),
                    auditLog.getReason(), auditLog.getGeolocLatitude(), auditLog.getGeolocLongitude(),
                    auditLog.getDestinationApp(), auditLog.getService(), auditLog.getInternalMessageId(),
                    auditLog.getResponseCode(), auditLog.getResponseDescription(), auditLog.getDeviceId(),
                    auditLog.getScoreRisk(), auditLog.getInternalUsername(), auditLog.getE2inOut(),
                    auditLog.getCounter(),
                    auditLog.getIdRequest());
        } catch (Exception e) {
            log.error("Error al insertar en la tabla LOG_MESSAGE_E2DI", e);
        }
    }

}
