package com.alvaro;

import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.*;
import org.xmldb.api.modules.*;

public class Biblioteca {

    private static final String URI      = "xmldb:exist://localhost:8080/exist/xmlrpc";
    private static final String USER     = "admin";
    private static final String PASSWORD = "";
    private static final String COL_PATH = "/db/Biblioteca";

    private static final String XML_INICIAL = """
            <?xml version="1.0" encoding="UTF-8"?>
            <biblioteca>
                <libro id="1">
                    <titulo>Acceso a Datos</titulo>
                    <autor>María López</autor>
                    <precio>29.95</precio>
                </libro>
            </biblioteca>
            """;

    public static void main(String[] args) {
        try {
            registrarDriver();
            Collection coleccion = crearColeccion();
            if (coleccion != null) {
                insertarDocumento(coleccion);
                modificarDocumento(coleccion);
                consultarTitulos(coleccion);
                eliminarDocumento(coleccion);
                coleccion.close();
            }
        } catch (Exception e) {
            System.err.println("Error general: " + e.getMessage());
        }
    }

    private static void registrarDriver() throws Exception {
        Class<?> driver = Class.forName("org.exist.xmldb.DatabaseImpl");
        Database database = (Database) driver.getDeclaredConstructor().newInstance();
        database.setProperty("create-database", "true");
        DatabaseManager.registerDatabase(database);
        System.out.println("✓ Driver eXist-db registrado correctamente.");
    }

    private static Collection crearColeccion() throws Exception {
        Collection root = DatabaseManager.getCollection(URI + "/db", USER, PASSWORD);
        if (root == null) {
            System.err.println("Error: No se pudo conectar a eXist-db.");
            return null;
        }
        CollectionManagementService cms =
                (CollectionManagementService) root.getService("CollectionManagementService", "1.0");
        Collection coleccion = DatabaseManager.getCollection(URI + COL_PATH, USER, PASSWORD);
        if (coleccion == null) {
            coleccion = cms.createCollection("Biblioteca");
            System.out.println("✓ Colección '/db/Biblioteca' creada.");
        } else {
            System.out.println("i La colección '/db/Biblioteca' ya existe.");
        }
        root.close();
        return coleccion;
    }

    private static void insertarDocumento(Collection col) throws Exception {
        XMLResource documento = (XMLResource) col.createResource("libros.xml", "XMLResource");
        documento.setContent(XML_INICIAL);
        col.storeResource(documento);
        System.out.println("✓ Documento 'libros.xml' insertado correctamente.");
    }

    private static void modificarDocumento(Collection col) throws Exception {
        XMLResource recurso = (XMLResource) col.getResource("libros.xml");
        if (recurso != null) {
            String xmlActual = (String) recurso.getContent();
            String xmlModificado = xmlActual.replace(
                    "</libro>",
                    "    <editorial>Ediciones DAM</editorial>\n    </libro>"
            );
            recurso.setContent(xmlModificado);
            col.storeResource(recurso);
            System.out.println("✓ Documento modificado: se añadió <editorial>Ediciones DAM</editorial>.");
        }
    }

    private static void consultarTitulos(Collection col) throws Exception {
        XPathQueryService xqs = (XPathQueryService) col.getService("XPathQueryService", "1.0");
        xqs.setProperty("indent", "yes");
        String xquery = "for $t in doc('/db/Biblioteca/libros.xml')//titulo return $t";
        ResourceSet resultado = xqs.query(xquery);
        ResourceIterator it = resultado.getIterator();
        System.out.println("\n--- Títulos encontrados en la biblioteca ---");
        while (it.hasMoreResources()) {
            Resource r = it.nextResource();
            System.out.println("   → " + r.getContent());
        }
        System.out.println("--------------------------------------------\n");
    }

    private static void eliminarDocumento(Collection col) throws Exception {
        Resource recurso = col.getResource("libros.xml");
        if (recurso != null) {
            col.removeResource(recurso);
            System.out.println("✓ Documento 'libros.xml' eliminado.");
        } else {
            System.out.println("⚠ Documento 'libros.xml' no encontrado.");
        }
    }
}