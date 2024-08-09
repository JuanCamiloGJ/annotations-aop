package com.e2ew.auditor.aop.aspects.enums;

import lombok.Getter;

@Getter
public enum ServiceAction {

    VALIDAR("VAL"),
    CREAR("CRE"),
    ELIMINAR("ELI"),
    ACTUALIZAR("ACT"),
    CONSULTAR("CON"),
    NOTIFICAR("NOT")
    ;

    private final String actionName;

    private ServiceAction(String actionName) {
        this.actionName = actionName;
    }

}
