package com.e2ew.auditor.aop.aspects;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.catalina.connector.RequestFacade;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.e2ew.auditor.aop.aspects.annotations.Audit;
import com.e2ew.auditor.aop.aspects.models.AuditLog;
import com.e2ew.dtoebanking.BasicRequestHB;
import com.e2ew.dtoebanking.GenericInfoHB;
import com.e2ew.dtoebanking.Response;
import com.e2ew.auditor.aop.aspects.repository.AuditorRepository;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class ServiceAuditor {
    private static final List<Class<? extends Annotation>> MAPPING_ANNOTATIONS = Arrays.asList(
            RequestMapping.class, GetMapping.class, PostMapping.class, PutMapping.class, DeleteMapping.class,
            PatchMapping.class);
    private static final List<Class<? extends Annotation>> FIELD_ANNOTATIONS = Arrays.asList(NotEmpty.class,
            NotBlank.class, NotNull.class);
    private final AuditorRepository auditorRepository;
    @Around("@annotation(audit)")
    public Object auditLogsCtrlIN(ProceedingJoinPoint joinPoint, Audit audit) throws Throwable {
        boolean isDataComplete = true;
        AuditLog auditLog;
        validateAnnotationLocation(joinPoint);

        var args = joinPoint.getArgs();

        var genericInfoDTOs = Arrays.stream(args)
                .filter(arg -> arg instanceof GenericInfoHB)
                .map(arg -> (GenericInfoHB) arg)
                .toList();
        var basicRequestHBDTOs = Arrays.stream(args)
                .filter(arg -> arg instanceof BasicRequestHB)
                .map(arg -> (BasicRequestHB) arg)
                .toList();

        var httpRequest = Arrays.stream(args)
                .filter(arg -> arg instanceof HttpServletRequest)
                .map(arg -> (HttpServletRequest) arg)
                .toList();

        if (genericInfoDTOs.isEmpty() && basicRequestHBDTOs.isEmpty()) {
            isDataComplete = false;
            log.warn(
                    "No se encontraron objetos de tipo GenericInfoHB o BasicRequestHB en los argumentos del método {}.",
                    joinPoint.getSignature().getName());
        }

        HashMap<String, Object> infoRequest = null;

        if (!httpRequest.isEmpty()) {
            var infoRequestOpt = getDataRequest(httpRequest.get(0));
            if (infoRequestOpt.isPresent()) {
                infoRequest = infoRequestOpt.get();
            }
        } else {
            log.error("No se encontró el objeto HttpServletRequest en los argumentos del método {}.",
                    joinPoint.getSignature().getName());
        }

        if (isDataComplete) {

            LocalDateTime nowTime = LocalDateTime.now();
            int company = 0;
            String channel = "UNK";
            String requestDestination = "UNK";
            String serviceAction = "UNK";
            String internalUsername = "UNK";
            String serverIp = "UNK";
            String consumerHost = "UNK";
            Integer consumerPort = 0;
            String idType = "UNK";
            String idNumber = "UNK";
            String service = "UNK";
            String externalUsername = "UNK";
            HashMap<String, Object> requiredFields = null;
            //obtener los campos necesarios para el log de auditoría de los objetos GenericInfoHB o BasicRequestHB
            if (!genericInfoDTOs.isEmpty()) {
                requiredFields = getImplicitData(genericInfoDTOs);
            }

            if (!basicRequestHBDTOs.isEmpty()) {
                requiredFields = getImplicitData(basicRequestHBDTOs);
            }

            //Crear campos para el objeto de auditoría
            try {
                company = Integer.parseInt((String) requiredFields.getOrDefault("company", "0"));
                channel = (String) requiredFields.getOrDefault("channel", "UNK");
                requestDestination = audit.appName().getServiceName();
                serviceAction = audit.action().getActionName();
                internalUsername = (String) requiredFields.getOrDefault("internalUsername", "UNK");
                idType = (String) requiredFields.getOrDefault("idType", "UNK");
                idNumber = (String) requiredFields.getOrDefault("idNumber", "UNK");
                externalUsername = (String) requiredFields.getOrDefault("externalUsername", "UNK");

            } catch (Exception e) {
                log.error("Error al obtener los datos necesarios para el log de auditoría.");
            }
            //obtener datos de la petición http
            if (infoRequest != null) {
                try {
                    serverIp = (String) infoRequest.getOrDefault("serverIp", "UNK");
                    consumerHost = (String) infoRequest.getOrDefault("consumerHost", "UNK");
                    service = (String) infoRequest.getOrDefault("service", "UNK");
                    consumerPort = (Integer) infoRequest.getOrDefault("consumerPort", "UNK");
                } catch (Exception e) {
                    log.error("Error al obtener los datos del objeto infoRequest");
                }
            }
            //crear el internalMessageId
            String internalMessageId =
                    company + channel + requestDestination + serviceAction + LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

            auditLog = AuditLog.builder()
                    .dateLog(nowTime)
                    .company(company)
                    .serverIp(serverIp)
                    .idenType(idType)
                    .identifier(idNumber)
                    .consumerApp("UNK")
                    .consumerChannel(channel)
                    .consumerHost(consumerHost)
                    .consumerPort(consumerPort)
                    .deviceAttributes("UNK")
                    .externalUsername(externalUsername)
                    .externalMessageId("N/A") //calcular
                    .externalSessionId("N/A") //calcular
                    .reason(serviceAction)
                    .geolocLatitude(0.0)
                    .geolocLongitude(0.0)
                    .destinationApp(requestDestination)
                    .service(service)
                    .internalMessageId(internalMessageId) //calcular
                    .responseCode("UNK") //calcular
                    .responseDescription("UNK") //calcular
                    .deviceId("UNK") //calcular
                    .scoreRisk(0)
                    .internalUsername(internalUsername)
                    .e2inOut("IN") //calcular
                    .counter(0)
                    .idRequest(0) //calcular
                    .build();

            auditorRepository.auditLogMessageE2di(auditLog);
            //Proceso de auditoría de salida
            try {
                Object result = joinPoint.proceed();
                if (result instanceof ResponseEntity<?> response) {
                    if (response.getStatusCode().is2xxSuccessful()) {
                        log.info("Respuesta exitosa");
                    } else {
                        log.error("Respuesta fallida");
                    }
                    if (response.getBody() instanceof Response responseDTO) {
                        auditLog = AuditLog.builder()
                                .dateLog(LocalDateTime.now())
                                .company(company)
                                .serverIp(serverIp)
                                .idenType(idType)
                                .identifier(idNumber)
                                .consumerApp("UNK")
                                .consumerChannel(channel)
                                .consumerHost(consumerHost)
                                .consumerPort(consumerPort)
                                .deviceAttributes("UNK")
                                .externalUsername(externalUsername)
                                .externalMessageId("N/A") //calcular
                                .externalSessionId("N/A") //calcular
                                .reason(serviceAction)
                                .geolocLatitude(0.0)
                                .geolocLongitude(0.0)
                                .destinationApp(requestDestination)
                                .service(service)
                                .internalMessageId(internalMessageId) //calcular
                                .responseCode(responseDTO.getResponse().getResponseCode()) //calcular
                                .responseDescription(responseDTO.getResponse().getResponseDesc()) //calcular
                                .deviceId("UNK") //calcular
                                .scoreRisk(0)
                                .internalUsername(internalUsername)
                                .e2inOut("OUT") //calcular
                                .counter(0)
                                .idRequest(0) //calcular
                                .build();

                        auditorRepository.auditLogMessageE2di(auditLog);
                    }

                } else {
                    log.error("No se pudo obtener la respuesta del servicio {}", joinPoint.getSignature().getName());
                    log.error("La respuesta del servicio: {} no es de tipo ResponseEntity<?>",
                            joinPoint.getSignature().getName());
                }
                return result;

            } catch (Exception e) {

                auditLog = AuditLog.builder()
                        .dateLog(LocalDateTime.now())
                        .company(company)
                        .serverIp(serverIp)
                        .idenType(idType)
                        .identifier(idNumber)
                        .consumerApp("UNK")
                        .consumerChannel(channel)
                        .consumerHost(consumerHost)
                        .consumerPort(consumerPort)
                        .deviceAttributes("UNK")
                        .externalUsername(externalUsername)
                        .externalMessageId("N/A") //calcular
                        .externalSessionId("N/A") //calcular
                        .reason(serviceAction)
                        .geolocLatitude(0.0)
                        .geolocLongitude(0.0)
                        .destinationApp(requestDestination)
                        .service(service)
                        .internalMessageId(internalMessageId) //calcular
                        .responseCode("UNK") //calcular
                        .responseDescription(e.getMessage()) //calcular
                        .deviceId("UNK") //calcular
                        .scoreRisk(0)
                        .internalUsername(internalUsername)
                        .e2inOut("OUT") //calcular
                        .counter(0)
                        .idRequest(0) //calcular
                        .build();

                auditorRepository.auditLogMessageE2di(auditLog);
                throw e;
            }

        } else {
            log.error("No se pudo obtener la información necesaria para el log de auditoría.");
        }

        return joinPoint.proceed();
    }



    private Optional<HashMap<String, Object>> getDataRequest(HttpServletRequest httpRequest) {

        try {

            var request = ((HttpServletRequestWrapper) httpRequest).getRequest();
            var data = new HashMap<String, Object>();

            data.put("consumerHost", request.getRemoteAddr() == null ? "UNK" : request.getRemoteAddr());
            data.put("consumerPort", request.getRemotePort() == 0 ? "UNK" : request.getRemotePort());
            data.put("serverIp", request.getServerName() == null ? "UNK" : request.getServerName());
            data.put("service", ((RequestFacade) request).getRequestURI() == null ?
                    "UNK" :
                    ((RequestFacade) request).getRequestURI());
            return Optional.of(data);
        } catch (Exception e) {
            log.error("Error al castear el objeto HttpServletRequest a CachedBodyHttpServletRequest");
            return Optional.empty();
        }
    }

    private HashMap<String, Object> getImplicitData(List<?> baseDTORequestHB) {
        HashMap<String, Object> requiredFields = new HashMap<>();

        baseDTORequestHB.forEach(baseRequest -> {
            //obtener los datos que no pueden ser nulos
            Arrays.stream(baseRequest.getClass().getSuperclass().getDeclaredFields())
                    .filter(field -> FIELD_ANNOTATIONS.stream().anyMatch(field::isAnnotationPresent))
                    .forEach(field -> {
                        try {
                            field.setAccessible(true);
                            requiredFields.put(field.getName(), field.get(baseRequest));
                        } catch (Exception e) {
                            log.error("Error al acceder al campo {} de la clase {}", field.getName(),
                                    baseRequest.getClass().getName());
                        }
                    });
            //minado de datos implicitos
            var fieldsDocumentType = Arrays.asList("idenType", "typeId", "typeid", "identificatorType", "idType",
                    "IdType", "documentType", "typeDocument", "tdocument", "typeDoc", "typeDocument");
            var fieldsDocumentNumber = Arrays.asList("identificator", "identificatorNum", "idNumber", "numberId",
                    "numberid", "idnumber", "DocumentNumber", "documentNumber", "numberDocument", "idDocument",
                    "documentId", "nDocument", "nDoc", "numDoc", "numdocument", "id");
            var fieldExternalUsername = Arrays.asList("externalUsername", "externalUser", "externalUser",
                    "externalName",
                    "external", "externalUser", "externalUserName");
            String patternFindDocumentType = "(?i)(" + String.join("|", fieldsDocumentType) + ")";
            String patternFindDocumentNumber = "(?i)(" + String.join("|", fieldsDocumentNumber) + ")";
            String patternFindExternalUsername = "(?i)(" + String.join("|", fieldExternalUsername) + ")";
            var patternDocumentType = Pattern.compile(patternFindDocumentType);
            var patternDocumentNumber = Pattern.compile(patternFindDocumentNumber);
            var patternExternalUsername = Pattern.compile(patternFindExternalUsername);
            //obtiene los campos de la clase y de la superclase
            var fieldsSuper = Arrays.asList(baseRequest.getClass().getSuperclass().getDeclaredFields());
            var fieldsObj = Arrays.asList(baseRequest.getClass().getDeclaredFields());
            var allFields = new ArrayList<>(fieldsObj);
            allFields.addAll(fieldsSuper);

            allFields.forEach(field -> {

                Matcher matcher = patternDocumentType.matcher(field.getName());
                if (matcher.matches()) {
                    field.setAccessible(true);
                    try {
                        requiredFields.put("idType", field.get(baseRequest));
                    } catch (IllegalAccessException e) {
                        log.warn("Error al acceder al campo idType de la clase {}", baseRequest.getClass().getName());
                    }
                }

                matcher = patternDocumentNumber.matcher(field.getName());
                if (matcher.matches()) {
                    field.setAccessible(true);
                    try {
                        requiredFields.put("idNumber", field.get(baseRequest));
                    } catch (IllegalAccessException e) {
                        log.warn("Error al acceder al campo idNumber de la clase {}", baseRequest.getClass().getName());
                    }
                }
                matcher = patternExternalUsername.matcher(field.getName());
                if (matcher.matches()) {
                    field.setAccessible(true);
                    try {
                        requiredFields.put("externalUsername", field.get(baseRequest));
                    } catch (IllegalAccessException e) {
                        log.warn("Error al acceder al campo externalUsername de la clase {}",
                                baseRequest.getClass().getName());
                    }
                }

            });

        });
        return requiredFields;
    }

    private void validateAnnotationLocation(JoinPoint joinPoint) {
        Class<?> targetClass = joinPoint.getTarget().getClass();
        Method method = Arrays.stream(targetClass.getDeclaredMethods())
                .filter(m -> m.getName().equals(joinPoint.getSignature().getName()))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException("Method not found: " + joinPoint.getSignature().getName()));

        // Verificar si la clase está anotada con @RestController o @Controller
        if (!targetClass.isAnnotationPresent(RestController.class) && !targetClass.isAnnotationPresent(
                Controller.class)) {
            throw new IllegalArgumentException(
                    "La anotación @Audit solo puede ser utilizada en métodos de controladores.");
        }
        // Verificar si el método tiene una de las anotaciones de mapeo
        boolean hasMappingAnnotation = MAPPING_ANNOTATIONS.stream().anyMatch(method::isAnnotationPresent);

        if (!hasMappingAnnotation) {
            hasMappingAnnotation = Arrays.stream(targetClass.getInterfaces())
                    .flatMap(interf -> Arrays.stream(interf.getDeclaredMethods()))
                    .filter(m -> m.getName().equals(joinPoint.getSignature().getName()))
                    .anyMatch(m -> MAPPING_ANNOTATIONS.stream().anyMatch(m::isAnnotationPresent));
        }

        if (!hasMappingAnnotation) {
            throw new IllegalArgumentException(
                    "La anotación @Audit solo puede ser utilizada en métodos con anotaciones de mapeo como @RequestMapping, @GetMapping, etc.");
        }
    }

}
