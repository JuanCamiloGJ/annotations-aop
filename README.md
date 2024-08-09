## Auditor

Este proyecto me permite da herramientas para auditar los endpoints expuestos de los proyectos.

## Configuración

Para este poder utilizar este proyecto es necesario tener en cuenta los siguientes pasos:
1. Añadir como dependencia el proyecto de auditor en el proyecto que se desea auditar.
```
<dependency>
    <groupId>com.e2ew</groupId>
    <artifactId>auditor</artifactId>
    <version>[0.0.1,)</version>
    <scope>compile</scope>
</dependency>
```
2. Añada la anotación `@Audit` en el método que se desea auditar. Solo se permiten anotar métodos públicos y que sean la entrada de un endpoint, es decir solo metodos que hagan parte de una clase `@RestController, @Controller` y que esté anotado con `@GetMapping, @PostMapping, @RequestMapping` etc.

3. En la anotación `@Audit` debe especificar la acción del endpoint y el nombre del proyecto donde está anotando, use los **Enums** `AppName` y `ServiceAction` para especificar estos valores.
4. Cumpla con los requerimientos de la anotación, inyecte un Objeto de tipo `HttpServletRequest` en los parámetros de su endpoint. El framework se encargará de inyectar este objeto, solo debe declararlo en los parámetros de su método.
5. Verifique que el `@RequestBody` de su método sea un objeto que herede de las clases abstractas `BasicGenericHB` o `GenericInfoHB`, de lo contrario el framework no podrá auditar el endpoint.

### Nota

El no cumplir con los requerimientos verá en sus logs mensajes que le indican el nombre del método que no cumple con los requerimientos de la anotación `@Audit`. Ejemplo:
```
WARN c.e.auditor.aop.aspects.ServiceAuditor   : No se encontraron objetos de tipo GenericInfoHB o BasicRequestHB en los argumentos del método passwordExpirations.
ERROR c.e.auditor.aop.aspects.ServiceAuditor   : No se encontró el objeto HttpServletRequest en los argumentos del método passwordExpirations.
ERROR c.e.auditor.aop.aspects.ServiceAuditor   : No se pudo obtener la información necesaria para el log de auditoría.
```

Puede que no sea necesario auditar cada endpoint por su naturaleza, tome la mejor decisión para su proyecto.
Ejemplo de un método que cumple con los requerimientos de la anotación `@Audit`:

```java
import org.springframework.web.bind.annotation.ModelAttribute;
import com.e2ew.dtoebanking.GenericInfoHB;

@Audit(action = ServiceAction.CONSULTAR, appName = AppName.HB_REQUEST)
@GetMapping("/password-expirations")
public ResponseEntity<List<PasswordExpiration>> passwordExpirations(@ModelAttribute GenericInfoHB datos,HttpServletRequest request) {
    return ResponseEntity.ok(passwordExpirationService.getPasswordExpirations());
}
```

El ejemplo anterior cumple con los requerimientos de la anotación `@Audit`, el método es público, es un endpoint y recibe un objeto de tipo `HttpServletRequest` y un objeto de tipo `GenericInfoHB` en sus parámetros.
Un GET no recibe un `@RequestBody` por lo que puede crear un objeto de tipo `GenericInfoHB` o `BasicGenericHB` y anotarlo con `@ModelAttribute` para que el framework lo inyecte en los parámetros de su método.
# annotations-aop
