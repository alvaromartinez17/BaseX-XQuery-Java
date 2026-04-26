

//  la librería BaseX para conectarse a servidores de bases de datos XML
import org.basex.api.client.ClientSession;

// import de java.io para manejar flujos de entrada/salida y excepciones
import java.io.ByteArrayInputStream;
import java.io.IOException;
// Import para codificación de caracteres en UTF-8
import java.nio.charset.StandardCharsets;



public class AutorLibroDbApp {

    // Datos de conexioon al servidor BaseX (host, puerto, usuario y contraseña)
    // ── Configuración de conexión ──────────────────────────────────────────────
    private static final String HOST     = "localhost";
    private static final int    PORT     = 1984;
    private static final String USER     = "admin";
    private static final String PASSWORD = "admin";

    // ── Nombre de la base de datos ─────────────────────────────────────────────
    private static final String DB_NAME  = "AutoresDB";

    // ── Documento XML inicial ──────────────────────────────────────────────────
    private static final String XML_CONTENT =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<autores>\n"
                    + "  <autor id=\"1\">\n"
                    + "    <nombre>Ana pepita</nombre>\n"
                    + "    <pais>españa</pais>\n"
                    + "  </autor>\n"
                    + "  <autor id=\"2\">\n"
                    + "    <nombre>Jonass</nombre>\n"
                    + "    <pais>españa</pais>\n"
                    + "  </autor>\n"
                    + "</autores>";

    // ── Consultas XQuery ───────────────────────────────────────────────────────

    /** Devuelve los nombres de los autores, España */
    private static final String QUERY_ESPANOLES =
            "for $a in doc('AutoresDB/autores.xml')/autores/autor\n"
                    + "where $a/pais = 'España'\n"
                    + "return $a/nombre/text()";

    /** XQuery Update: añade <libros><libro id="2"/></libros> al autor id=1 */
    private static final String QUERY_UPDATE =
            "let $autor := doc('AutoresDB/autores.xml')/autores/autor[@id='1']\n"
                    + "return insert node\n"
                    + "  <libros><libro id=\"2\"/></libros>\n"
                    + "into $autor";

    /** Devuelve el XML completo del autor id=1 para comparar antes/después */
    private static final String QUERY_AUTOR_1 =
            "doc('AutoresDB/autores.xml')/autores/autor[@id='1']";

    // ──────────────────────────────────────────────────────────────────────────
    //  MAIN
    // ──────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) {

        titulo("EJERCICIO  2 – XQuery con BaseX desde Java");

        ClientSession session = null;

        try {
            // ── 1. Conectar al servidor BaseX ──────────────────────────────────
            seccion("1. Conectando al servidor BaseX");
            System.out.printf("     Host   : %s%n     Puerto : %d%n     Usuario: %s%n%n",
                    HOST, PORT, USER);
            session = new ClientSession(HOST, PORT, USER, PASSWORD);
            System.out.println("    Conexión establecida correctamente.");

            // ── 2. Crear la base de datos ──────────────────────────────────────
            seccion("2. Creando base de datos \"" + DB_NAME + "\"");
            crearBaseDeDatos(session);

            // ── 3. Consulta: autores españoles ─────────────────────────────────
            seccion("3. Consulta XQuery – Autores españoles");
            System.out.println("     XQuery ejecutada:");
            System.out.println("     ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄");
            for (String l : QUERY_ESPANOLES.split("\n")) System.out.println("     " + l);
            System.out.println("     ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄");
            String espanoles = session.query(QUERY_ESPANOLES).execute();
            System.out.println("\n     ► Resultado: " + espanoles);

            // ── 4. Estado antes de la actualizacion ───────────────────────────
            seccion("4. Autor id=\"1\" ANTES de la actualización");
            String antes = session.query(QUERY_AUTOR_1).execute();
            System.out.println(antes);

            // ── 5. Ejecutar XQuery Update ──────────────────────────────────────
            seccion("5. XQuery Update – Insertando nodo <libros>");
            System.out.println("     ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄");
            for (String l : QUERY_UPDATE.split("\n")) System.out.println("     " + l);
            System.out.println("     ┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄");
            session.query(QUERY_UPDATE).execute();
            System.out.println("\n      Actualización realizada correctamente.");

            // ── 6. Estado DESPUÉS de la actualización ─────────────────────────
            seccion("6. Autor id=\"1\" Despues de la actualización");
            String despues = session.query(QUERY_AUTOR_1).execute();
            System.out.println(despues);

        } catch (IOException e) {
            System.err.println("\n  ERROR: " + e.getMessage());
            System.err.println("  Asegúrate de que el servidor BaseX está activo (basexserver)");
            System.err.println("  y de que las credenciales son correctas (admin/admin).");
        } finally {
            // ── 7. Cerrar la sesión ────────────────────────────────────────────
            seccion("7. Cerrando sesión con el servidor");
            if (session != null) {
                try {
                    session.close();
                    System.out.println("      Sesión cerrada correctamente.");
                } catch (IOException e) {
                    System.err.println("      Error al cerrar sesión: " + e.getMessage());
                }
            }
        }

        titulo("FIN DEL EJERCICIO");
    }





    /**
     * Creo la base de datos "AutoresDB" y carga el XML embebido como "autores.xml".
     * Si la BD ya existía, la elimina primero para garantizar estado inicial limpio.
     */
    private static void crearBaseDeDatos(ClientSession session) throws IOException {
        // elimino s ya  si ya existe
        try {
            session.execute("DROP DB " + DB_NAME);
            System.out.println("     (BD anterior eliminada para partir de cero)");
        } catch (IOException ignorada) { /* no existía, es normal */ }

        // Crear BD completamente vacia
        session.execute("CREATE DB " + DB_NAME);

        // añado el documento XML con el nombre "autores.xml"
        byte[] bytes = XML_CONTENT.getBytes(StandardCharsets.UTF_8);
        session.add("autores.xml", new ByteArrayInputStream(bytes));

        System.out.println("     ✔ BD \"" + DB_NAME + "\" creada con el documento 'autores.xml':");
        System.out.println();
        for (String l : XML_CONTENT.split("\n")) System.out.println("     " + l);
        System.out.println();
    }

    private static void titulo(String texto) {
        String borde = "═".repeat(64);
        System.out.println("\n" + borde);
        System.out.println("  " + texto);
        System.out.println(borde + "\n");
    }

    private static void seccion(String texto) {
        System.out.println("\n  ┌─ " + texto);
        System.out.println("  │");
    }
}