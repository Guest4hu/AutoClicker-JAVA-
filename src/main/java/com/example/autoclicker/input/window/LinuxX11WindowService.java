package com.example.autoclicker.input.window;

import com.example.autoclicker.dto.WindowInfoDTO;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementação para servidores gráficos Linux X11 utilizando a Foreign Function & Memory API (Panama FFM).
 * Acessa diretamente a biblioteca nativa libX11.so.6 para consultar propriedades EWMH das janelas ativas.
 */
public class LinuxX11WindowService implements WindowService {

    private static final MethodHandle X_OPEN_DISPLAY;
    private static final MethodHandle X_CLOSE_DISPLAY;
    private static final MethodHandle X_DEFAULT_ROOT_WINDOWS;
    private static final MethodHandle X_INTERN_ATOM;
    private static final MethodHandle X_GET_WINDOW_PROPERTY;
    private static final MethodHandle X_FETCH_NAME;
    private static final MethodHandle X_FREE;
    private static final boolean INITIALIZED;

    static {
        boolean success = false;
        MethodHandle open = null;
        MethodHandle close = null;
        MethodHandle root = null;
        MethodHandle internAtom = null;
        MethodHandle getProp = null;
        MethodHandle fetchName = null;
        MethodHandle free = null;
        try {
            Linker linker = Linker.nativeLinker();
            SymbolLookup x11 = SymbolLookup.libraryLookup("libX11.so.6", Arena.global());

            open = linker.downcallHandle(
                    x11.find("XOpenDisplay").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            );

            close = linker.downcallHandle(
                    x11.find("XCloseDisplay").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
            );

            root = linker.downcallHandle(
                    x11.find("XDefaultRootWindow").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)
            );
            internAtom = linker.downcallHandle(
                    x11.find("XInternAtom").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
            );
            getProp = linker.downcallHandle(
                    x11.find("XGetWindowProperty").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
                            ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG,
                            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            );
            fetchName = linker.downcallHandle(
                    x11.find("XFetchName").orElseThrow(),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)
            );
            free = linker.downcallHandle(
                    x11.find("XFree").orElseThrow(),
                    FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
            );
            success = true;
        } catch (Throwable e) {
            System.err.println("LinuxX11WindowService: falha ao carregar libX11.so.6: " + e.getMessage());
        }
        X_OPEN_DISPLAY = open;
        X_CLOSE_DISPLAY = close;
        X_DEFAULT_ROOT_WINDOWS = root;
        X_INTERN_ATOM = internAtom;
        X_GET_WINDOW_PROPERTY = getProp;
        X_FETCH_NAME = fetchName;
        X_FREE = free;
        INITIALIZED = success;
    }

    @Override
    public List<WindowInfoDTO> getOpenWindows() {
        if (!INITIALIZED) {
            return List.of();
        }
        MemorySegment display;
        try {
            display = (MemorySegment) X_OPEN_DISPLAY.invokeExact(MemorySegment.NULL);
        } catch (Throwable e) {
            return List.of();
        }
        if (display.equals(MemorySegment.NULL)) {
            return List.of();
        }
        List<WindowInfoDTO> windows = new ArrayList<>();
        try (Arena arena = Arena.ofConfined()) {
            long rootWindow = (long) X_DEFAULT_ROOT_WINDOWS.invokeExact(display);

            // Atoms padrão EWMH
            MemorySegment clientListAtomName = arena.allocateFrom("_NET_CLIENT_LIST");
            long clientListAtom = (long) X_INTERN_ATOM.invokeExact(display, clientListAtomName, 0);

            MemorySegment netWmNameAtomName = arena.allocateFrom("_NET_WM_NAME");
            long netWmNameAtom = (long) X_INTERN_ATOM.invokeExact(display, netWmNameAtomName, 0);

            MemorySegment utf8AtomName = arena.allocateFrom("UTF8_STRING");
            long utf8Atom = (long) X_INTERN_ATOM.invokeExact(display, utf8AtomName, 0);

            MemorySegment actualType = arena.allocate(ValueLayout.JAVA_LONG);
            MemorySegment actualFormat = arena.allocate(ValueLayout.JAVA_INT);
            MemorySegment nitems = arena.allocate(ValueLayout.JAVA_LONG);
            MemorySegment bytesAfter = arena.allocate(ValueLayout.JAVA_LONG);
            MemorySegment propReturn = arena.allocate(ValueLayout.ADDRESS);

            int result = (int) X_GET_WINDOW_PROPERTY.invokeExact(
                    display, rootWindow, clientListAtom,
                    0L, 1024L, 0, 33L /* XA_WINDOW */,
                    actualType, actualFormat, nitems, bytesAfter, propReturn
            );
            long totalWindows = nitems.get(ValueLayout.JAVA_LONG, 0);
            MemorySegment windowListPtr = propReturn.get(ValueLayout.ADDRESS, 0);

            if (result == 0 && totalWindows > 0 && !windowListPtr.equals(MemorySegment.NULL)) {
                MemorySegment windowArray = windowListPtr.reinterpret(totalWindows * ValueLayout.JAVA_LONG.byteSize());

                for (int i = 0; i < totalWindows; i++) {
                    long windowId = windowArray.getAtIndex(ValueLayout.JAVA_LONG, i);
                    String title = "";

                    // 1. Tenta ler o título moderno em UTF-8 (_NET_WM_NAME usado por Chrome, Firefox, etc.)
                    MemorySegment utf8PropReturn = arena.allocate(ValueLayout.ADDRESS);
                    int utf8Result = (int) X_GET_WINDOW_PROPERTY.invokeExact(
                            display, windowId, netWmNameAtom,
                            0L, 1024L, 0, utf8Atom,
                            actualType, actualFormat, nitems, bytesAfter, utf8PropReturn
                    );

                    MemorySegment utf8Ptr = utf8PropReturn.get(ValueLayout.ADDRESS, 0);
                    long utf8Len = nitems.get(ValueLayout.JAVA_LONG, 0);

                    if (utf8Result == 0 && utf8Len > 0 && !utf8Ptr.equals(MemorySegment.NULL)) {
                        title = utf8Ptr.reinterpret(utf8Len + 1).getString(0);
                        X_FREE.invokeExact(utf8Ptr);
                    }

                    // 2. Fallback para XFetchName caso seja um programa clássico sem _NET_WM_NAME
                    if (title.isBlank()) {
                        MemorySegment nameReturn = arena.allocate(ValueLayout.ADDRESS);
                        int fetchResult = (int) X_FETCH_NAME.invokeExact(display, windowId, nameReturn);
                        MemorySegment namePtr = nameReturn.get(ValueLayout.ADDRESS, 0);
                        if (fetchResult != 0 && !namePtr.equals(MemorySegment.NULL)) {
                            title = namePtr.reinterpret(1024).getString(0);
                            X_FREE.invokeExact(namePtr);
                        }
                    }

                    if (!title.isBlank()) {
                        windows.add(new WindowInfoDTO(windowId, title));
                    }
                }
                X_FREE.invokeExact(windowListPtr);
            }
        } catch (Throwable e) {
            System.err.println("LinuxX11WindowService: erro ao listar janelas X11: " + e.getMessage());
        } finally {
            try {
                X_CLOSE_DISPLAY.invokeExact(display);
            } catch (Throwable ignored) {
            }
        }
        return windows;
    }
}
