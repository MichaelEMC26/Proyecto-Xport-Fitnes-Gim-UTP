# Registrar Asistencia: bloqueo de controles tras mostrar el resultado

## Qué hace

| Momento | Casilla **Código** | Botón **Ingresar [Enter]** | Tecla Enter / lector QR |
|---|---|---|---|
| Al abrir la plantilla | Activa (con el cursor) | Activo | Registra |
| Después de mostrar el resultado de un registro (ingreso correcto, duplicado, sin membresía, vencido, etc.) | **Desactivada** (gris) | **Desactivado** | **No hace nada** |
| Después de presionar **Nuevo [F2]** | Activa (con el cursor) | Activo | Registra |

Mientras está bloqueado, debajo de la casilla aparece el aviso
*"Registro mostrado. Presione Nuevo [F2] para registrar a otro socio."*

No se bloquea si el código está vacío o si ocurre un error de conexión con la base de datos,
para que se pueda volver a intentar.

**Salir [Esc]** funciona siempre.

## Cómo está programado

El estado lo guarda el servidor, en una sola variable. La página y el JavaScript solo la reflejan.

### 1. Controlador: `controller/AsistenciaBean.java`

```java
private boolean controlesBloqueados;          // false al iniciar -> controles activos

public boolean isControlesBloqueados() { return controlesBloqueados; }
```

| Método | Qué hace con la variable |
|---|---|
| `abrir()` (al abrir la página, vía `f:viewAction`) | Llama a `nuevo()`, que pone la variable en `false`: se inicia con los controles activos. |
| `registrarIngreso(codcli)` | Después de llamar a `RegistrarAsistenciaF` y cargar en pantalla el socio, el periodo y los avisos, pone `controlesBloqueados = true`. Pasa tanto si el ingreso fue correcto como si no. |
| `registrar()` | Si `controlesBloqueados` es `true`, sale sin registrar nada. Así se protege aunque alguien reactive la casilla desde las herramientas del navegador. |
| `nuevo()` | Pone `controlesBloqueados = false`. Es el **único** lugar que desbloquea. |

La variable `ingresoRegistrado` se mantiene, pero ahora solo indica si el último intento fue correcto.

### 2. Página: `RegistrarAsistencia.xhtml`

```xml
<h:inputText id="raCodigo" value="#{asistenciaBean.codigo}"
             disabled="#{asistenciaBean.controlesBloqueados}" ... />

<button id="raIngresar" data-bloqueado="#{asistenciaBean.controlesBloqueados}"> ... </button>

<p class="ra-ayuda" id="raAyuda" data-bloqueado="#{asistenciaBean.controlesBloqueados}">
    <span class="ra-ayuda-activo">Escanee su QR ... y presione Enter.</span>
    <span class="ra-ayuda-bloqueado">Registro mostrado. Presione Nuevo [F2] ...</span>
</p>
```

- `disabled` de JSF desactiva la casilla en el HTML. Además, JSF no acepta su valor mientras
  esté bloqueada.
- `data-bloqueado` le pasa el estado al JavaScript y al CSS.

### 3. JavaScript: `js/registrar-asistencia.js`

```js
function bloquearControles(bloquear) {
    codigo.disabled = bloquear;
    btnIngresar.disabled = bloquear;
    ayuda.setAttribute("data-bloqueado", bloquear ? "true" : "false");
    if (bloquear) codigo.blur(); else codigo.focus();
}
function bloqueado() { return codigo.disabled; }

bloquearControles(btnIngresar.getAttribute("data-bloqueado") === "true");  // estado inicial
```

- **`ingresar()`:** empieza con `if (bloqueado()) return;`, así que Enter y el botón Ingresar
  no hacen nada.
- **Teclado global:** cuando está bloqueado, se ignora cualquier tecla, salvo F2 y Esc, que se
  revisan antes. Eso incluye el Enter y el texto que envía el lector QR.
  - Antes, escribir con la pantalla bloqueada limpiaba la pantalla sola. Se quitó, porque ahora
    solo Nuevo debe reactivar los controles.
- **`limpiarPantalla()`:** se usa con Nuevo [F2]. Limpia los datos, llama a
  `bloquearControles(false)` para activar la casilla y el botón y poner el cursor en Código, y
  luego hace clic en `lnkNuevo`, que ejecuta `asistenciaBean.nuevo()` en el servidor.

### 4. Estilos: `css/registrar-asistencia.css`

```css
.ra-codigo-fila input:disabled{background:#EEF1F6;color:#8A96AA;cursor:not-allowed}
.ra-ayuda .ra-ayuda-bloqueado{display:none;color:#B45309}
.ra-ayuda[data-bloqueado="true"] .ra-ayuda-activo{display:none}
.ra-ayuda[data-bloqueado="true"] .ra-ayuda-bloqueado{display:inline}
```

El botón Ingresar ya tenía su estilo `:disabled`.

Se cambió la versión de los enlaces a `?v=4` (`.css` y `.js`) para que el navegador no use la copia anterior.

## Flujo completo

```
Abrir página -> abrir() -> nuevo() -> controlesBloqueados = false -> Código activo
   |
   | Enter / QR / Ingresar
   v
registrar() -> registrarIngreso() -> muestra resultado -> controlesBloqueados = true
   |                                         -> página: Código y Ingresar desactivados
   | Enter / QR -> ignorados (JS) y rechazados (servidor)
   |
   | Nuevo [F2]
   v
limpiarPantalla() + nuevo() -> controlesBloqueados = false -> Código activo con el cursor
```
