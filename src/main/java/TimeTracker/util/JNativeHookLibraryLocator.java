/*
 * Copyright (C) 2026 Matthias Grimm
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package TimeTracker.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Iterator;

import com.github.kwhat.jnativehook.NativeLibraryLocator;

/**
 * Locates JNativeHook's native library in a way that works inside a jlink
 * runtime image.<p>
 *
 * JNativeHook's stock {@code DefaultLibraryLocator} derives the extraction
 * directory from its own code-source location and calls {@code new File(uri)}
 * on it. In a modular runtime image the classes are served from the {@code jrt:}
 * filesystem, so that call throws {@code IllegalArgumentException: URI scheme is
 * not "file"} before the library can be loaded.<p>
 *
 * Reading the library from JNativeHook's own module is not an option either: its
 * {@code lib/<os>/<arch>} directories are encapsulated packages, so no other
 * module can reach them through the resource API. Instead the build copies the
 * native libraries into this module's own resources under {@code /jni/...} (see
 * {@code processResources} in build.gradle); a module can always read its own
 * resources, including from a {@code jrt:} image. This locator is selected via
 * the {@code jnativehook.lib.locator} system property, set in
 * {@link GlobalHotkey}'s static initializer before {@code GlobalScreen} loads.
 *
 * @author Matthias Grimm
 */
public class JNativeHookLibraryLocator
implements NativeLibraryLocator
{
    @Override
    public Iterator<File> getLibraries()
    {
        String resource = resourcePath();

        // Same-module resource read: unaffected by JNativeHook's encapsulation
        // and by the file-URI assumption of its default locator.
        try (InputStream in = JNativeHookLibraryLocator.class.getResourceAsStream(resource)) {
            if (in == null)
                throw new IOException("Bundled native library not found: " + resource);

            String name = resource.substring(resource.lastIndexOf('/') + 1);
            int dot = name.lastIndexOf('.');
            File lib = File.createTempFile(name.substring(0, dot), name.substring(dot));
            lib.deleteOnExit();

            try (OutputStream out = new FileOutputStream(lib)) {
                in.transferTo(out);
            }
            return Collections.singletonList(lib).iterator();

        } catch (IOException e) {
            throw new RuntimeException("Unable to extract the JNativeHook native library", e);
        }
    }

    /**
     * Builds the resource path of the bundled native library for the current
     * platform, mirroring JNativeHook's own {@code lib/<os>/<arch>/} layout.
     */
    private static String resourcePath()
    {
        String os   = System.getProperty("os.name", "").toLowerCase();
        String arch = System.getProperty("os.arch", "").toLowerCase();

        String osDir;
        String libName;
        if (os.contains("win")) {
            osDir = "windows";  libName = "JNativeHook.dll";
        } else if (os.contains("mac") || os.contains("darwin")) {
            osDir = "darwin";   libName = "libJNativeHook.dylib";
        } else {
            osDir = "linux";    libName = "libJNativeHook.so";
        }

        String archDir = switch (arch) {
            case "amd64", "x86_64"                         -> "x86_64";
            case "x86", "i386", "i486", "i586", "i686"     -> "x86";
            case "aarch64", "arm64"                        -> "arm64";
            case "arm"                                     -> "arm";
            default                                        -> arch;
        };

        return "/jni/" + osDir + "/" + archDir + "/" + libName;
    }
}
