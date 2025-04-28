/** *****************************************************************************
 * Embedding Python-based AI Agents into Java Applications version 1.0.
 *
 * Copyright (c)  2024,  Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
 ***************************************************************************** */
package com.oracle.ateam.examples.langgraph;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.io.IOAccess;
import org.graalvm.python.embedding.utils.VirtualFileSystem;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * The PyContext class provide the method to create GraalPy Context in application scoped
 *
 */
@ApplicationScoped
public class PyContext {

    private final static Logger LOGGER = Logger.getLogger(PyContext.class.getName());
    private static Context context;

    public Context getPyContext() {
        return context;
    }

    /**
     * This method create a python context
     * @return Polygot Context
     */
    public Context createPyContext() {
        LOGGER.log(Level.INFO, "Creating GraalPy Context");
        VirtualFileSystem vfs = VirtualFileSystem.newBuilder()
                .extractFilter(p -> {
                    String s = p.toString();
                    // Specify what files in the virtual filesystem need to be accessed outside the
                    // Truffle sandbox.
                    // e.g. if they need to be accessed by the operating system loader.
                    return s.endsWith(".ttf");
                })
                .build();
        context = Context.newBuilder("python")
                // set true to allow experimental options
                .allowExperimentalOptions(true)
                .option("python.DisableFrozenModules", "true")
                // setting false will deny all privileges unless configured below
                .allowAllAccess(true)
                // allows python to access the java language
                .allowHostAccess(HostAccess.ALL)
                // allow access to the virtual and the host filesystem, as well as sockets
                .allowIO(IOAccess.newBuilder()
                        .allowHostSocketAccess(true)
                        .fileSystem(vfs)
                        .build())
                // allow creating python threads
                .allowCreateThread(true)
                // allow running Python native extensions
                .allowNativeAccess(true)
                // allow exporting Python values to polyglot bindings and accessing Java from
                // Python
                .allowPolyglotAccess(PolyglotAccess.ALL)
                // choose the backend for the POSIX module
                .option("python.PosixModuleBackend", "java")
                // equivalent to the Python -B flag
                .option("python.DontWriteBytecodeFlag", "true")
                // print Python exceptions directly
                .option("python.AlwaysRunExcepthook", "true")
                // Force to automatically import site.py module, to make Python packages
                // available
                .option("python.ForceImportSite", "true")
                // The sys.executable path, a virtual path that is used by the interpreter to
                // discover packages
                .option("python.Executable",
                        vfs.getMountPoint()
                        + "/venv/bin/python")
                // Set the python home to be read from the embedded resources
                .option("python.PythonHome", vfs.getMountPoint() + "/home")
                // Set python path to point to sources stored in src/main/resources/vfs/proj
                .option("python.PythonPath", vfs.getMountPoint() + "/src")
                .option("log.python.capi.CExtContext.level", "CONFIG")
                .build();

        context.initialize("python");
        LOGGER.log(Level.INFO, "GraalPy Context created");
        return context;
    }
}
