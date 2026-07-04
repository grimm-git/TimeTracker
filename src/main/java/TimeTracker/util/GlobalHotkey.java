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

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import javafx.application.Platform;

/**
 * Installs a system-wide keyboard hook (via JNativeHook) so a configurable
 * hotkey can bring the hidden application window back to the front, even while
 * the application has no focus.<p>
 *
 * The default combination is {@code CTRL+SHIFT+F10}.<p>
 *
 * JNativeHook delivers key events on its own native dispatch thread, so the
 * trigger action is forwarded to the JavaFX Application Thread via
 * {@link Platform#runLater(Runnable)}.
 *
 * @author Matthias Grimm
 */
public class GlobalHotkey
implements NativeKeyListener
{
    static {
        // Select our jlink-friendly library locator before GlobalScreen's static
        // initializer runs (triggered later by registerNativeHook). The stock
        // DefaultLibraryLocator assumes a file: code source and fails inside a
        // jlink image; see JNativeHookLibraryLocator. Respect an explicit
        // override if the user set one on the command line.
        if (System.getProperty("jnativehook.lib.locator") == null)
            System.setProperty("jnativehook.lib.locator",
                    JNativeHookLibraryLocator.class.getName());
    }

    /** Modifier bits this hotkey distinguishes (Ctrl/Shift/Alt/Meta). */
    private static final int MOD_MASK = NativeKeyEvent.CTRL_MASK
                                      | NativeKeyEvent.SHIFT_MASK
                                      | NativeKeyEvent.ALT_MASK
                                      | NativeKeyEvent.META_MASK;

    private final Runnable onTrigger;
    private boolean registered = false;

    // Customizable combination, defaulting to CTRL+SHIFT+F10. Volatile because the
    // combination may be changed on the JavaFX thread while the native hook
    // thread reads it in nativeKeyPressed().
    private volatile int keyCode   = NativeKeyEvent.VC_F10;
    private volatile int modifiers = NativeKeyEvent.CTRL_MASK | NativeKeyEvent.SHIFT_MASK;

    /**
     * @param onTrigger action run on the JavaFX thread when the hotkey is
     *        pressed. May be {@code null} for a configuration-only instance
     *        that merely carries a combination and is never registered.
     */
    public GlobalHotkey(Runnable onTrigger)
    {
        this.onTrigger = onTrigger;
    }

    /**
     * Changes the hotkey combination.
     *
     * @param keyCode   a {@code NativeKeyEvent.VC_*} key code
     * @param modifiers a bitwise OR of {@code NativeKeyEvent.*_MASK} modifiers
     */
    public void setHotkey(int keyCode, int modifiers)
    {
        this.keyCode = keyCode;
        this.modifiers = modifiers & MOD_MASK;
    }

    /** The default combination ({@code CTRL+SHIFT+F10}) in packed form. */
    public static final int DEFAULT_HOTKEY =
            packHotkey(NativeKeyEvent.VC_F10, NativeKeyEvent.CTRL_MASK | NativeKeyEvent.SHIFT_MASK);

    /**
     * Packs a key code and modifier mask into a single integer suitable for
     * storage (e.g. the {@code hotkey} column of the config table). The modifier
     * bits occupy the high 16 bits and the key code the low 16 bits.
     *
     * @param keyCode   a {@code NativeKeyEvent.VC_*} key code
     * @param modifiers a bitwise OR of {@code NativeKeyEvent.*_MASK} modifiers
     * @return the packed combination
     */
    public static int packHotkey(int keyCode, int modifiers)
    {
        return ((modifiers & MOD_MASK) << 16) | (keyCode & 0xFFFF);
    }

    /**
     * Restores a combination previously produced by {@link #packHotkey(int,int)}.
     *
     * @param packedCombo the packed key code and modifier mask
     */
    public void setHotkey(int packed)
    {
        setHotkey(packed & 0xFFFF, packed >>> 16);
    }

    /**
     * @return this combination in packed form, ready to be stored
     *         (see {@link #packHotkey(int,int)})
     */
    public int getHotkey()
    {
        return packHotkey(keyCode, modifiers);
    }

    /**
     * Returns a human readable description of the stored combination, with the
     * modifiers in a fixed order followed by the key, e.g. {@code CTRL + SHIFT + T}.
     *
     * @return the combination as a display string
     */
    @Override
    public String toString()
    {
        return format(getHotkey());
    }

    /**
     * Returns a human readable description of a packed combination, with the
     * modifiers in a fixed order followed by the key, e.g. {@code CTRL + SHIFT + T}.
     * A combination whose key part is {@link NativeKeyEvent#VC_UNDEFINED} renders
     * the modifiers only (e.g. {@code CTRL + }), which is handy as a live preview
     * while the final key has not been pressed yet.
     *
     * @param packedCombo the packed key code and modifier mask
     *                    (see {@link #packHotkey(int, int)})
     * @return the combination as a display string
     */
    public static String format(int packedCombo)
    {
        int keyCode   = packedCombo & 0xFFFF;
        int modifiers = (packedCombo >>> 16) & MOD_MASK;

        StringBuilder sb = new StringBuilder();

        if ((modifiers & NativeKeyEvent.CTRL_MASK)  != 0) sb.append("CTRL + ");
        if ((modifiers & NativeKeyEvent.SHIFT_MASK) != 0) sb.append("SHIFT + ");
        if ((modifiers & NativeKeyEvent.ALT_MASK)   != 0) sb.append("ALT + ");
        if ((modifiers & NativeKeyEvent.META_MASK)  != 0) sb.append("META + ");

        if (keyCode != NativeKeyEvent.VC_UNDEFINED)
            sb.append(NativeKeyEvent.getKeyText(keyCode).toUpperCase());

        return sb.toString();
    }

    /** Native key codes of the function keys F1..F24 (values are not contiguous). */
    private static final Set<Integer> FUNCTION_KEYS = Set.of(
            NativeKeyEvent.VC_F1,  NativeKeyEvent.VC_F2,  NativeKeyEvent.VC_F3,  NativeKeyEvent.VC_F4,
            NativeKeyEvent.VC_F5,  NativeKeyEvent.VC_F6,  NativeKeyEvent.VC_F7,  NativeKeyEvent.VC_F8,
            NativeKeyEvent.VC_F9,  NativeKeyEvent.VC_F10, NativeKeyEvent.VC_F11, NativeKeyEvent.VC_F12,
            NativeKeyEvent.VC_F13, NativeKeyEvent.VC_F14, NativeKeyEvent.VC_F15, NativeKeyEvent.VC_F16,
            NativeKeyEvent.VC_F17, NativeKeyEvent.VC_F18, NativeKeyEvent.VC_F19, NativeKeyEvent.VC_F20,
            NativeKeyEvent.VC_F21, NativeKeyEvent.VC_F22, NativeKeyEvent.VC_F23, NativeKeyEvent.VC_F24);

    /**
     * @param nativeKeyCode a {@code NativeKeyEvent.VC_*} key code
     * @return true if the code denotes one of the function keys F1..F24
     */
    public static boolean isFunctionKey(int nativeKeyCode)
    {
        return FUNCTION_KEYS.contains(nativeKeyCode);
    }

    /**
     * @param nativeKeyCode a {@code NativeKeyEvent.VC_*} key code
     * @return true if the code denotes a modifier key (Ctrl/Shift/Alt/Meta)
     */
    private static boolean isModifierKey(int nativeKeyCode)
    {
        return nativeKeyCode == NativeKeyEvent.VC_SHIFT
            || nativeKeyCode == NativeKeyEvent.VC_CONTROL
            || nativeKeyCode == NativeKeyEvent.VC_ALT
            || nativeKeyCode == NativeKeyEvent.VC_META;
    }

    /**
     * Callback invoked while a {@link Learner} records a combination. It runs on
     * the JavaFX Application Thread.
     */
    @FunctionalInterface
    public interface KeyCapture
    {
        /**
         * @param packedCombo the pressed combination in packed form; for a press
         *        of modifier keys only the key part is {@code VC_UNDEFINED}
         * @param modifierOnly true while only modifier keys are held (no main key)
         */
        void onKey(int packedCombo, boolean modifierOnly);
    }

    /**
     * Records a new hotkey directly from the global native key stream (the same
     * layer that enforces the hotkey), so combinations a window manager grabs
     * before the application window sees them can still be captured. Each press is
     * forwarded to the given {@link KeyCapture} on the JavaFX thread.
     */
    public static final class Learner
    implements NativeKeyListener
    {
        private final KeyCapture capture;

        public Learner(KeyCapture capture)
        {
            this.capture = capture;
        }

        /** Starts listening on the (already installed) global hook. */
        public void start()
        {
            GlobalScreen.addNativeKeyListener(this);
        }

        /** Stops listening. Safe to call more than once. */
        public void stop()
        {
            GlobalScreen.removeNativeKeyListener(this);
        }

        @Override
        public void nativeKeyPressed(NativeKeyEvent ev)
        {
            int code = ev.getKeyCode();
            int mods = normalize(ev.getModifiers());
            boolean modifierOnly = isModifierKey(code);
            int packed = packHotkey(modifierOnly ? NativeKeyEvent.VC_UNDEFINED : code, mods);

            Platform.runLater(() -> capture.onKey(packed, modifierOnly));
        }
    }

    /**
     * Installs the native keyboard hook and starts listening. Logging produced
     * by JNativeHook is disabled before the hook is registered.
     *
     * @throws NativeHookException if the platform hook can not be installed
     *         (e.g. on a Wayland session, where global hooks are blocked)
     */
    public void register() throws NativeHookException
    {
        // Disable JNativeHook logging. Must happen before registerNativeHook().
        Logger log = Logger.getLogger("com.github.kwhat.jnativehook");
        log.setLevel(Level.OFF);
        log.setUseParentHandlers(false);

        GlobalScreen.registerNativeHook();
        GlobalScreen.addNativeKeyListener(this);
        registered = true;
    }

    /**
     * Removes the listener and uninstalls the native hook. Safe to call more
     * than once and safe to call when registration never succeeded.
     */
    public void unregister()
    {
        if (!registered)
            return;

        try {
            GlobalScreen.removeNativeKeyListener(this);
            GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException e) {
            // Nothing useful to do during teardown.
        } finally {
            registered = false;
        }
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent ev)
    {
        // Keep this callback cheap: only compare and hand off to the FX thread.
        // The hotkey is not swallowed, so it also reaches the focused application.
        if (onTrigger != null
                && ev.getKeyCode() == keyCode && normalize(ev.getModifiers()) == modifiers) {
            Platform.runLater(onTrigger);
        }
    }

    /**
     * Collapses the side-specific modifier bits JNativeHook reports (e.g.
     * {@code CTRL_L_MASK}) into their side-agnostic group masks ({@code
     * CTRL_MASK}), so a left-side press matches a combination stored with the
     * generic {@code CTRL_MASK}. Bits outside {@link #MOD_MASK} are dropped.
     *
     * @param mods the raw {@code NativeKeyEvent.getModifiers()} value
     * @return the modifiers normalised to {@code CTRL/SHIFT/ALT/META_MASK} bits
     */
    private static int normalize(int mods)
    {
        int norm = 0;
        if ((mods & NativeKeyEvent.CTRL_MASK)  != 0) norm |= NativeKeyEvent.CTRL_MASK;
        if ((mods & NativeKeyEvent.SHIFT_MASK) != 0) norm |= NativeKeyEvent.SHIFT_MASK;
        if ((mods & NativeKeyEvent.ALT_MASK)   != 0) norm |= NativeKeyEvent.ALT_MASK;
        if ((mods & NativeKeyEvent.META_MASK)  != 0) norm |= NativeKeyEvent.META_MASK;
        return norm;
    }
}
