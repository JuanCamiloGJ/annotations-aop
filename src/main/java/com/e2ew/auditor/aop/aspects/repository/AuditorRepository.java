package com.e2ew.auditor.aop.aspects.repository;

import com.e2ew.auditor.aop.aspects.models.AuditLog;

public interface AuditorRepository {

    void auditLogMessageE2di(AuditLog auditLog);


}
