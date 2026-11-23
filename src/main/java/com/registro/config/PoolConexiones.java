package com.registro.config;

import com.microsoft.sqlserver.jdbc.SQLServerConnectionPoolDataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.PooledConnection;

/**
 * Pool de conexiones a SQL Server (sin librerías nuevas: usa SQLServerConnectionPoolDataSource del driver mssql-jdbc).
 *
 * Antes, cada consulta abría una conexión física nueva (TCP + inicio de sesión en SQL Server + búsqueda de la
 * instancia), y una pantalla hace varias consultas. Ahora las conexiones se reutilizan:
 *  - obtener(): entrega una conexión libre (o abre una nueva si no hay) envuelta en un proxy.
 *  - close() del proxy: cierra los Statement / ResultSet que quedaron abiertos y la devuelve al pool.
 *    El driver limpia la sesión (sp_reset_connection: deshace transacciones abiertas, tablas temporales,
 *    opciones SET) al volver a usarla, sin una ida y vuelta extra al servidor.
 *  - Una conexión que estuvo libre más de VALIDAR_MS se prueba (isValid) antes de entregarla;
 *    una que estuvo libre más de MAXIMO_LIBRE_MS se cierra.
 *  - Si todas están ocupadas más de ESPERA_S segundos (por ejemplo, porque algún código no cerró su
 *    conexión), se entrega una conexión directa sin pool para no bloquear el sistema.
 *
 * Los DAO no cambian: siguen llamando a ConexionBD.obtenerConexion() y a close().
 */
final class PoolConexiones {

    private static final long VALIDAR_MS = 30_000;          // libre más de 30 s: se prueba antes de usarla
    private static final long MAXIMO_LIBRE_MS = 600_000;    // libre más de 10 min: se cierra
    private static final int ESPERA_S = 10;

    private final String url;
    private final String usuario;
    private final String clave;
    private final javax.sql.ConnectionPoolDataSource origen;
    private final Semaphore permisos;
    private final int maximo;

    private final Deque<Libre> libres = new ArrayDeque<>();
    private final Set<PooledConnection> enUso = Collections.newSetFromMap(new IdentityHashMap<>());
    private volatile boolean cerrado;

    private static final class Libre {
        final PooledConnection pc;
        final long desde;
        Libre(PooledConnection pc, long desde) { this.pc = pc; this.desde = desde; }
    }

    PoolConexiones(String url, String usuario, String clave, int maximo) {
        this(origenSqlServer(url, usuario, clave), url, usuario, clave, maximo);
    }

    /** Constructor con el origen de conexiones (permite probar el pool sin base de datos). */
    PoolConexiones(javax.sql.ConnectionPoolDataSource origen, String url, String usuario, String clave, int maximo) {
        this.url = url;
        this.usuario = usuario;
        this.clave = clave;
        this.maximo = maximo;
        this.permisos = new Semaphore(maximo, true);
        this.origen = origen;
    }

    private static javax.sql.ConnectionPoolDataSource origenSqlServer(String url, String usuario, String clave) {
        SQLServerConnectionPoolDataSource ds = new SQLServerConnectionPoolDataSource();
        ds.setURL(url);
        ds.setUser(usuario);
        ds.setPassword(clave);
        return ds;
    }

    boolean mismosDatos(String u, String us, String c) {
        return url.equals(u) && usuario.equals(us) && clave.equals(c);
    }

    /** Conexión del pool (o directa si el pool está lleno o cerrado). */
    Connection obtener() throws SQLException {
        boolean conPermiso;
        try {
            conPermiso = !cerrado && permisos.tryAcquire(ESPERA_S, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new SQLException("Se interrumpió la espera de una conexión.", ex);
        }
        if (!conPermiso) {
            if (!cerrado) {
                System.err.println("[PoolConexiones] Las " + maximo + " conexiones están ocupadas desde hace "
                        + ESPERA_S + " s; se usa una conexión directa. Revise si algún código no cierra su conexión.");
            }
            return java.sql.DriverManager.getConnection(url, usuario, clave);
        }
        try {
            PooledConnection pc = tomarLibre();
            Connection logica = null;
            if (pc == null) {
                pc = origen.getPooledConnection();
                pc.addConnectionEventListener(oyente);
            } else if (pc instanceof Validada) {
                logica = ((Validada) pc).logica;
                pc = ((Validada) pc).pc;
            }
            synchronized (this) {
                enUso.add(pc);
            }
            if (logica == null) logica = pc.getConnection();
            if (!logica.getAutoCommit()) logica.setAutoCommit(true);
            return envolver(logica);
        } catch (SQLException | RuntimeException ex) {
            permisos.release();
            throw ex;
        }
    }

    /** Toma la conexión libre más reciente; descarta las vencidas y prueba las que estuvieron mucho tiempo sin uso. */
    private PooledConnection tomarLibre() {
        while (true) {
            Libre l;
            synchronized (this) {
                l = libres.pollFirst();
            }
            if (l == null) return null;
            long libreMs = System.currentTimeMillis() - l.desde;
            if (libreMs > MAXIMO_LIBRE_MS) {
                cerrarFisica(l.pc);
                continue;
            }
            if (libreMs > VALIDAR_MS) {
                try {
                    Connection prueba = l.pc.getConnection();
                    if (prueba.isValid(3)) return new Validada(l.pc, prueba);
                } catch (SQLException ex) {
                    // se descarta
                }
                cerrarFisica(l.pc);
                continue;
            }
            return l.pc;
        }
    }

    /** Conexión ya probada con isValid: se reutiliza la misma conexión lógica. */
    private static final class Validada implements PooledConnection {
        final PooledConnection pc;
        final Connection logica;
        Validada(PooledConnection pc, Connection logica) { this.pc = pc; this.logica = logica; }
        @Override public Connection getConnection() { return logica; }
        @Override public void close() throws SQLException { pc.close(); }
        @Override public void addConnectionEventListener(ConnectionEventListener l) { pc.addConnectionEventListener(l); }
        @Override public void removeConnectionEventListener(ConnectionEventListener l) { pc.removeConnectionEventListener(l); }
        @Override public void addStatementEventListener(javax.sql.StatementEventListener l) { pc.addStatementEventListener(l); }
        @Override public void removeStatementEventListener(javax.sql.StatementEventListener l) { pc.removeStatementEventListener(l); }
    }

    /** Eventos del driver: conexión lógica cerrada (vuelve al pool) o error grave (se descarta). */
    private final ConnectionEventListener oyente = new ConnectionEventListener() {
        @Override
        public void connectionClosed(ConnectionEvent e) {
            PooledConnection pc = (PooledConnection) e.getSource();
            boolean devolver;
            synchronized (PoolConexiones.this) {
                devolver = enUso.remove(pc);
                if (devolver && !cerrado) libres.addFirst(new Libre(pc, System.currentTimeMillis()));
            }
            if (devolver) {
                if (cerrado) cerrarFisica(pc);
                permisos.release();
            }
        }

        @Override
        public void connectionErrorOccurred(ConnectionEvent e) {
            PooledConnection pc = (PooledConnection) e.getSource();
            boolean estaba;
            synchronized (PoolConexiones.this) {
                estaba = enUso.remove(pc);
                libres.removeIf(l -> l.pc == pc);
            }
            cerrarFisica(pc);
            if (estaba) permisos.release();
        }
    };

    /**
     * Proxy de la conexión: registra los Statement que se crean y los cierra al cerrar la conexión.
     * Así no importa el orden del finally (hay DAO que cierran la conexión antes que el ResultSet):
     * nada queda abierto sobre una conexión que ya volvió al pool y que puede estar usando otra petición.
     */
    private static Connection envolver(Connection logica) {
        return (Connection) Proxy.newProxyInstance(PoolConexiones.class.getClassLoader(),
                new Class<?>[] { Connection.class }, new Manejador(logica));
    }

    private static final class Manejador implements InvocationHandler {
        private final Connection logica;
        private final List<Statement> sentencias = new ArrayList<>();
        private boolean cerrada;

        Manejador(Connection logica) { this.logica = logica; }

        @Override
        public synchronized Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
            String n = m.getName();
            switch (n) {
                case "close":
                    if (!cerrada) {
                        cerrada = true;
                        for (Statement s : sentencias) {
                            try { s.close(); } catch (SQLException ignorada) { /* ya estaba cerrada */ }
                        }
                        sentencias.clear();
                        logica.close();          // el driver avisa al pool (connectionClosed)
                    }
                    return null;
                case "isClosed":
                    return cerrada || logica.isClosed();
                case "equals":
                    return proxy == args[0];
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "toString":
                    return "ConexionPool[" + logica + "]";
                case "unwrap":
                case "isWrapperFor":
                    break;
                default:
                    if (cerrada) throw new SQLException("La conexión ya está cerrada.");
            }
            try {
                Object r = m.invoke(logica, args);
                if (r instanceof Statement) sentencias.add((Statement) r);
                return r;
            } catch (InvocationTargetException ex) {
                throw ex.getCause();
            }
        }
    }

    private static void cerrarFisica(PooledConnection pc) {
        try {
            pc.close();
        } catch (SQLException ignorada) {
            // ya estaba cerrada o la red se cayó
        }
    }

    /** Cierra las conexiones libres (al detener la aplicación o al cambiar la configuración). */
    void cerrar() {
        List<Libre> copia;
        synchronized (this) {
            cerrado = true;
            copia = new ArrayList<>(libres);
            libres.clear();
        }
        for (Libre l : copia) cerrarFisica(l.pc);
    }

    synchronized int libres() { return libres.size(); }
    synchronized int enUso() { return enUso.size(); }
}
