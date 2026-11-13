# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

Xport Fitness Gym is a Java web app for running a gym (members/"socios", staff/"personal", products, memberships, expenses). It was migrated from servlets + JSP to **Jakarta Faces 4.0 (Mojarra) + Facelets with Weld CDI**, running on **Tomcat 10.1** (context path `/AplicacionWebGimnasio`, see `src/main/webapp/META-INF/context.xml`). Code, comments, identifiers and UI text are in Spanish; keep new code consistent with that. `README_MIGRACION_JSF.md` documents the migration.

## Build and run

- Maven project (`pom.xml`), Java 21 (`maven.compiler.release=21`). Usually opened and deployed from NetBeans.
- `mvn clean package` → `target/AplicacionWebGimnasio.war`; deploy to Tomcat 10.1 and open `/AplicacionWebGimnasio/` (redirects to `login.xhtml`).
- No tests, no linter (`src/test` does not exist).
- Use `jakarta.*` imports only — never `javax.*`. Servlet API is `provided` by Tomcat; Faces, Weld, the SQL Server JDBC driver and JasperReports are bundled in the WAR.

## Database

- SQL Server database `BDGimnasio` on a remote host. `src/main/java/com/registro/config/ConexionBD.java` opens a new JDBC connection per call (no pooling). Note: the README says credentials come from `GYM_DB_URL`/`GYM_DB_USER`/`GYM_DB_PASSWORD` env vars, but the current `ConexionBD` hardcodes them.
- All data access goes through **stored procedures** via `CallableStatement` (`{call sp_...}`). Procedure sources are mostly not in the repo (only `sql/personal_crud.sql` for the Personal module, plus a backup of the originals), so column names read from `ResultSet`s must match what the procedures return. Some procedures report status via `RETURN` codes read with `{? = call ...}` + `registerOutParameter`.

## Architecture

Layered under `src/main/java/com/registro/`:

```
Facelets view (*.xhtml)  ->  CDI bean (*Bean)  ->  *FACADE  ->  *DAO  ->  stored procedure
                                                   (thin pass-through)    (JDBC + DTO mapping)
```

- `dto/` — plain beans whose field names mirror DB columns (e.g. `Cliente_ID`, `RazonSocial`).
- `dao/` — opens a connection, calls a stored procedure, maps rows to DTOs, closes resources, and swallows `SQLException` with `printStackTrace()` (returns empty list / null / false on failure).
- `facade/` — one-line delegations to the DAO; beans/servlets only talk to facades. `facade/ficherovacio.java` is an empty placeholder.
- `controller/` — despite the package name, holds both:
  - **JSF/CDI beans** (`@Named`): `loginBean` (request; login stores `usuarioAutenticado`, `usuario`, `rol` in the session map; `cerrarSesion`), `clienteBean` and `personalBean` (session-scoped CRUD state; actions return `/<view>.xhtml?faces-redirect=true`), `dashboardBean`, `empresaBean`, `placeholderBean`. These are what the views actually use.
  - **Legacy servlets** kept from the JSP version (`ClienteController`, `PersonalController`, `Controlador` mapped to `/UsuarioController`) — they now just redirect to `.xhtml` views or handle old `accion=` links; prefer the beans for new work.
  - `AutenticacionFilter` — protects every `*.xhtml` (except `login.xhtml`/`index.xhtml`) and the legacy servlet URLs; redirects to `login.xhtml` when the session has no `usuarioAutenticado`.
  - `CerrarSesionServlet` (`/cerrarSesion`) for logout from plain links.
- `report/ReporteClientesServlet` (`/reportes/clientes.pdf`) compiles `src/main/resources/reportes/report.jrxml` at request time with JasperReports and streams a PDF (the JRXML holds its own SQL query).

### Views

- Pages in `src/main/webapp/*.xhtml` are `ui:composition`s of `WEB-INF/templates/principal.xhtml` (except `login.xhtml`/`index.xhtml`). The template loads shared CSS/JS (`css/menu.css`, `js/menu.js`, Font Awesome from CDN) and inserts `title`, `styles`, `content`, `scripts`.
- The sidebar is the composite component `resources/components/menuLateral.xhtml` (namespace `jakarta.faces.composite/components`), driven by `<ui:param name="menuOpcion">` (active item) and `<ui:param name="menuSubmenu">` (open submenu) set on each page. Admin-only menu sections (`menuPersonal`, `menuCatalogo`) render when `sessionScope.rol` is `Administrador` or `ADMIN`.
- Per-page styling/scripts live in `css/<page>.css` and `js/<page>.js`, referenced with `#{request.contextPath}`.
- Only login, socios (Clientes) and personal are wired to beans/DB. Catálogo (Productos, Categorías, Marcas), Punto de Venta, Almacén, membresías and gastos are still front-end demos in HTML/JS inside Facelets; asistencia, ventas and configurar are informational placeholders. Product/category/brand DTOs and `ProductosDAO` are in progress.

## Repo notes

- `respaldo-jsp/` holds the original JSP views for reference only; they are not deployed.
- `ObjetosClienteDTO.txt`, `ScriptClienteDAO.txt`, `ScriptClienteFACADE.txt` in the root are scratch copies of class code, not compiled.
