# Informe de Aporte Individual y Colaboración en GitHub

**Alumno:** Tolaba Marcelo Fabian  
**Usuario GitHub:** `marceloft2026`  
**Módulo Asignado:** S04 – Informes de Resolución  
**Módulos de Colaboración / Avance:** S03 – Gestión de Incidentes  
**Proyecto:** HogarYA – API REST (TP Integrador DAOS - Actividad 02)  

---

## 1. Resumen Ejecutivo del Aporte Individual

Como integrante del equipo de desarrollo del Trabajo Práctico Integrador (Actividad 02), asumí la responsabilidad principal del diseño, desarrollo, pruebas y documentación del **Módulo S04 (Informes de Resolución)**. 

Adicionalmente, ante la dependencia directa de este módulo respecto del dominio de incidentes, **avancé sobre la construcción de la estructura base del Módulo S03 (Gestión de Incidentes)**. Esta contribución desbloqueó el desarrollo al proveer las entidades, enumeraciones, repositorios y datos iniciales necesarios para procesar las resoluciones de manera 100% funcional e integrada.

---

## 2. Detalle de Implementación Técnica por Módulo

### 🔹 Módulo S04: Informes de Resolución (Responsabilidad Principal)

* **Entidad de Dominio y DTOs:**
  * Implementación de la entidad JPA `InformeResolucion` para el registro persistente de comprobantes de cierre.
  * Creación de objetos de transferencia de datos: `InformeResolucionRequestDTO` e `InformeResolucionResponseDTO`.
  * Incorporación de anotaciones de validación (`@NotNull`, `@PositiveOrZero`, etc.) y enriquecimiento de respuestas mediante **HATEOAS** (enlaces `self` al informe y `incidente` al recurso asociado).

* **Servicio y Lógica de Negocio Transaccional:**
  * Desarrollado en `InformeResolucionService`, utilizando la anotación `@Transactional` para garantizar la atomicidad de las operaciones.
  * Al registrar un informe de resolución:
    1. Se crea y guarda el informe con fecha y costo de resolución.
    2. Se actualiza el estado del incidente a `RESUELTO`.
    3. Se registra de forma transparente la transición de estado en el `HistorialEstadoIncidente`.
  * Manejo de concurrencia mediante bloqueo pesimista/optimista para evitar resoluciones dobles sobre un mismo incidente.

* **Controlador REST y Documentación OpenAPI:**
  * Exposición de endpoints en `InformeResolucionController`:
    * `POST /informes-resolucion`: Registro de nuevo informe (retorna HTTP `201 Created`).
    * `GET /informes-resolucion/{id}`: Consulta individual por ID de informe (retorna HTTP `200 OK`).
    * `GET /informes-resolucion?incidenteId={id}`: Listado de informes filtrados por incidente (retorna HTTP `200 OK`).
  * Integración de **Springdoc OpenAPI (Swagger UI)** para la generación automática de documentación interactiva de la API.

---

### 🔹 Módulo S03: Gestión de Incidentes (Avance y Soporte por Necesidad)

Para permitir que el módulo S04 pudiera consultar y actualizar el estado de los incidentes, desarrollé los componentes clave del dominio de S03:

* **Modelo de Datos y Enums:**
  * Entidad principal `Incidente` y entidad de trazabilidad `HistorialEstadoIncidente`.
  * Enumeraciones del negocio: `EstadoIncidente` (`EN_PROCESO`, `RESUELTO`, etc.), `CategoriaIncidente` y `PrioridadIncidente`.

* **Persistencia e Inicialización:**
  * Creación de repositorios Spring Data JPA: `IncidenteRepository`, `EstadoIncidenteRepository` e `HistorialEstadoIncidenteRepository`.
  * Componente `EstadoIncidenteInitializer` para la carga inicial de datos requerida durante el arranque de la aplicación y la ejecución de pruebas.

---

## 3. Arquitectura del Proyecto y Manejo Global de Excepciones

* **Estructura de Paquetes Modulares:**
  * Organización limpia bajo la raíz `com.tuti.daos`, desacoplando responsabilidades en paquetes: `controller`, `domain`, `dto`, `exception`, `repository` y `service`.

* **Tratamiento Centralizado de Errores:**
  * Implementación de `InformeResolucionExceptionHandler` utilizando `@RestControllerAdvice` y una estructura estandarizada de error (`ApiErrorResponse`):
    * `400 Bad Request`: Captura de errores de validación en los DTOs de entrada.
    * `404 Not Found`: Manejo de incidentes o informes no encontrados.
    * `409 Conflict`: Reglas de negocio no cumplidas (ej. intentar resolver un incidente que no se encuentra en estado `EN_PROCESO`).

---

## 4. Pruebas Automatizadas (JUnit 5)

En la clase `InformeResolucionControllerTest`, desarrollé la suite de pruebas unitarias e integradas cubriendo los **6 casos de prueba obligatorios** de la consigna:

1. **Creación Exitosa:** Registro de informe, verificación de cambio de estado a `RESUELTO` e historial.
2. **Consulta por ID:** Obtención de datos e hipermedios HATEOAS.
3. **Filtrado por Incidente:** Consulta de historial de resoluciones vinculadas a un incidente.
4. **Validación de Solicitud (HTTP 400):** Rechazo de peticiones con datos incompletos o inválidos.
5. **Recurso Inexistente (HTTP 404):** Manejo de IDs no encontrados en la base de datos.
6. **Conflicto de Negocio (HTTP 409):** Rechazo de resoluciones sobre incidentes no válidos o en estado incorrecto.

---

## 5. Control de Versiones en Git y GitHub

* **Estrategia de Ramas:** Desarrollo aislado en la rama `feature/s04-hogarya` para evitar interferencias en `main`.
* **Configuración del Entorno:** Adaptación y unificación del archivo `pom.xml` integrando dependencias de Spring Boot, Spring Data JPA, HATEOAS, OpenAPI/Swagger, Lombok, MySQL y H2.
* **Historial de Commits:** Commits informativos siguiendo la convención (*conventional commits*), facilitando la integración limpia mediante Pull Requests.
