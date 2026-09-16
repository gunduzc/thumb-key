@file:Suppress("ktlint:standard:no-wildcard-imports")

package com.dessalines.thumbkey.keyboards

import android.view.KeyEvent
import com.dessalines.thumbkey.IMEService
import com.dessalines.thumbkey.textprocessors.TextProcessor
import com.dessalines.thumbkey.utils.*
import com.dessalines.thumbkey.utils.ColorVariant.*
import com.dessalines.thumbkey.utils.FontSizeVariant.*
import com.dessalines.thumbkey.utils.KeyAction.*
import com.dessalines.thumbkey.utils.SwipeNWay.*

// "english türkçe deutsch thumb-key dual": a 3-wide, 4-high layout meant to be shown twice in
// the Dual keyboard position, one copy under each thumb.
//
// Rows 1 to 3 are the english thumb-key letter block, untouched, with only the emoji, numeric
// and clipboard-history toggles and the dead key added. Holding a letter types a digit in phone
// order (s r o = 1 2 3, n h a = 4 5 6, t i e = 7 8 9), and holding space types 0.
//
// Every symbol and every utility swipe sits on the letter keys' spare directions, drawn small
// and muted, so no mode switch is needed for code. Bracket pairs mirror across the board: ( on s
// down-left, ) on o down-right, and so on. Numeric mode is the same grid with big tappable digits
// in place of the letters, plus F1 to F12 and insert on the h and i keys.
//
// Turkish and German letters come from one dead key (⟳) that cycles the letter before the cursor
// through its variants: s ş ß, c ç, g ğ, i ı, I İ, a ä, o ö, u ü, and back. The dead key is
// implemented by the layout's own text processor below.
//
// Ctrl and alt each switch to a grid generated from the main one, where every plain character
// becomes the matching key event with the modifier flag, so ctrl+c, ctrl+r, alt+. and friends
// work in termux and editors.

/** Cycles the character before the cursor through its Turkish and German variants. */
private val DEAD_KEY_CYCLE: Map<String, String> =
    listOf(
        "sşß",
        "SŞẞ",
        "cç",
        "CÇ",
        "gğ",
        "GĞ",
        "iı",
        "Iİ",
        "aä",
        "AÄ",
        "oö",
        "OÖ",
        "uü",
        "UÜ",
    ).flatMap { cycle ->
        val chars = cycle.codePoints().toArray().map { String(Character.toChars(it)) }
        chars.indices.map { i -> chars[i] to chars[(i + 1) % chars.size] }
    }.toMap()

private class DeadKeyCycleProcessor : TextProcessor {
    override fun handleComposeStart(ime: IMEService) {
        val ic = ime.currentInputConnection ?: return
        val before = ic.getTextBeforeCursor(1, 0)?.toString() ?: return
        val next = DEAD_KEY_CYCLE[before] ?: return
        ic.beginBatchEdit()
        ic.deleteSurroundingText(1, 0)
        ic.commitText(next, 1)
        ic.endBatchEdit()
    }

    override fun handleCommitText(
        ime: IMEService,
        input: CharSequence,
    ) {
        ime.currentInputConnection?.commitText(input, 1)
    }

    override fun handleKeyEvent(
        ime: IMEService,
        ev: KeyEvent,
    ) {
        ime.currentInputConnection?.sendKeyEvent(ev)
    }

    override fun handleFinishInput(ime: IMEService) {}

    override fun handleCursorUpdate(
        ime: IMEService,
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
    ) {}

    override fun updateCursorPosition(ime: IMEService) {}
}

// Blanks the whole board, letters, symbols, digits and icons, and brings it back.
private val TOGGLE_HIDE_ALL_KEYC = TOGGLE_HIDE_LETTERS_KEYC.copy(action = ToggleHideAll)

private val DEAD_KEY_DUAL =
    KeyC(
        display = KeyDisplay.TextDisplay("⟳"),
        action = StartComposeCombo,
        color = MUTED,
    )

private fun sendKey(
    keyCode: Int,
    label: String,
    meta: Int = 0,
): KeyC =
    KeyC(
        display = KeyDisplay.TextDisplay(label),
        action = SendEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, keyCode, 0, meta)),
        color = MUTED,
    )

private fun sym(text: String) = KeyC(text, color = MUTED)

private fun fkey(n: Int) = sendKey(KeyEvent.KEYCODE_F1 + (n - 1), "F$n")

private fun digit(d: String) = KeyC(d, size = LARGE)

private val EMOJI_TOGGLE_SMALL = TOGGLE_EMOJI_MODE_TRUE_KEYC.copy(size = SMALL, color = MUTED)
private val NUMERIC_TOGGLE_SMALL = TOGGLE_NUMERIC_MODE_TRUE_KEYC.copy(size = SMALL, color = MUTED)
private val ABC_TOGGLE_SMALL = TOGGLE_NUMERIC_MODE_FALSE_KEYC.copy(size = SMALL)
private val CLIPBOARD_TOGGLE_SMALL = TOGGLE_CLIPBOARD_MODE_TRUE_KEYC.copy(size = SMALL, color = MUTED)

// Bottom row: return, space, backspace. Space in the middle so that holding it for 0 matches the
// phone dial, backspace in the corner under a resting right thumb.
private val SPACEBAR_DUAL_KEY_ITEM =
    KeyItemC(
        center = SPACEBAR_CENTER_KEYC,
        swipeType = EIGHT_WAY,
        slideType = SlideType.MOVE_CURSOR,
        topLeft = SPACEBAR_TOP_KEYC, // line start
        top = SPACEBAR_PROGRAMMING_TOP_KEYC, // arrow up
        topRight = SPACEBAR_BOTTOM_KEYC, // line end
        left = SPACEBAR_LEFT_KEYC,
        right = SPACEBAR_RIGHT_KEYC,
        bottomLeft = PREVIOUS_WORD_BEFORE_CURSOR_KEYC,
        bottom = SPACEBAR_PROGRAMMING_BOTTOM_KEYC, // arrow down
        bottomRight = NEXT_WORD_AFTER_CURSOR_KEYC,
        backgroundColor = SURFACE_VARIANT,
        longPress = CommitText("0"),
    )

// The same key in numeric mode: tap for 0, hold for space.
private val ZERO_DUAL_KEY_ITEM =
    SPACEBAR_DUAL_KEY_ITEM.copy(
        center = digit("0"),
        longPress = CommitText(" "),
    )

private val BACKSPACE_DUAL_KEY_ITEM =
    BACKSPACE_KEY_ITEM.copy(
        swipeType = EIGHT_WAY,
        top = sendKey(KeyEvent.KEYCODE_PAGE_UP, "⇞"),
        bottom = sendKey(KeyEvent.KEYCODE_PAGE_DOWN, "⇟"),
        topRight = sendKey(KeyEvent.KEYCODE_FORWARD_DEL, "⌦"),
    )

private val RETURN_DUAL_KEY_ITEM =
    KeyItemC(
        center = RETURN_KEYC,
        swipeType = EIGHT_WAY,
        topLeft = TOGGLE_ALT_TRUE_KEYC,
        top = sendKey(KeyEvent.KEYCODE_ESCAPE, "esc"),
        topRight = TOGGLE_CTRL_TRUE_KEYC,
        left = sendKey(KeyEvent.KEYCODE_MOVE_HOME, "⇱"),
        right = sendKey(KeyEvent.KEYCODE_MOVE_END, "⇲"),
        bottomLeft = sendKey(KeyEvent.KEYCODE_TAB, "⇤", KeyEvent.META_SHIFT_ON),
        bottom = sendKey(KeyEvent.KEYCODE_TAB, "⇥"),
        backgroundColor = SURFACE_VARIANT,
        longPress = CommitText("\n"),
    )

private val BOTTOM_ROW = listOf(RETURN_DUAL_KEY_ITEM, SPACEBAR_DUAL_KEY_ITEM, BACKSPACE_DUAL_KEY_ITEM)
private val BOTTOM_ROW_NUMERIC = listOf(RETURN_DUAL_KEY_ITEM, ZERO_DUAL_KEY_ITEM, BACKSPACE_DUAL_KEY_ITEM)

// Letter block: the english thumb-key letters, with symbols and utility swipes on the spare
// directions. Settings group on r and clipboard group on t, in the default's directions.
private fun letterRows(upper: Boolean): List<List<KeyItemC>> {
    fun l(c: String) = KeyC(if (upper) c.uppercase() else c)

    fun big(c: String) = KeyC(if (upper) c.uppercase() else c, size = LARGE)

    return listOf(
        listOf(
            KeyItemC(
                center = big("s"),
                topLeft = sym("`"),
                top = sym("\\"),
                topRight = sym("/"),
                left = sym("#"),
                right = EMOJI_TOGGLE_SMALL,
                bottomLeft = sym("("),
                bottom = sym("~"),
                bottomRight = l("w"),
                longPress = CommitText("1"),
            ),
            KeyItemC(
                center = big("r"),
                topLeft = TOGGLE_HIDE_ALL_KEYC,
                top = GOTO_SETTINGS_KEYC,
                topRight = SWITCH_IME_KEYC,
                left = SWITCH_LANGUAGE_KEYC,
                right = MOVE_KEYBOARD_CYCLE_RIGHT_KEYC,
                bottomLeft = SWITCH_IME_VOICE_KEYC,
                bottom = l("g"),
                longPress = CommitText("2"),
            ),
            KeyItemC(
                center = big("o"),
                topLeft = sym("="),
                top = sym("%"),
                topRight = sym("+"),
                left = NUMERIC_TOGGLE_SMALL,
                right = sym(";"),
                bottomLeft = l("u"),
                bottom = sym(":"),
                bottomRight = sym(")"),
                longPress = CommitText("3"),
            ),
        ),
        listOf(
            KeyItemC(
                center = big("n"),
                topLeft = sym("{"),
                top = sym("$"),
                topRight = sym("@"),
                left = CLIPBOARD_TOGGLE_SMALL,
                right = l("m"),
                bottomLeft = sym("["),
                bottom = sym("&"),
                bottomRight = sym("|"),
                longPress = CommitText("4"),
            ),
            KeyItemC(
                center = big("h"),
                topLeft = l("j"),
                top = l("q"),
                topRight = l("b"),
                right = l("p"),
                bottomRight = l("y"),
                bottom = l("x"),
                bottomLeft = l("v"),
                left = l("k"),
                longPress = CommitText("5"),
            ),
            KeyItemC(
                center = big("a"),
                topLeft = sym("?"),
                top = if (upper) TOGGLE_CAPS_KEYC else TOGGLE_SHIFT_TRUE_KEYC,
                topRight = sym("}"),
                left = l("l"),
                right = sym("!"),
                bottomLeft = sym("_"),
                bottom = TOGGLE_SHIFT_FALSE_KEYC,
                bottomRight = sym("]"),
                longPress = CommitText("6"),
            ),
        ),
        listOf(
            KeyItemC(
                center = big("t"),
                topLeft = SELECT_ALL_KEYC,
                top = COPY_KEYC,
                topRight = l("c"),
                left = UNDO_KEYC,
                right = CUT_KEYC,
                bottomLeft = sym(","),
                bottom = PASTE_KEYC,
                bottomRight = REDO_KEYC,
                longPress = CommitText("7"),
            ),
            KeyItemC(
                center = big("i"),
                top = l("f"),
                topRight = sym("'"),
                right = l("z"),
                bottomRight = sym("-"),
                bottom = sym("."),
                bottomLeft = sym("*"),
                longPress = CommitText("8"),
            ),
            KeyItemC(
                center = big("e"),
                topLeft = l("d"),
                top = DEAD_KEY_DUAL,
                topRight = sym("\""),
                left = sym("<"),
                right = sym(">"),
                bottomRight = sym("^"),
                longPress = CommitText("9"),
            ),
        ),
    )
}

val KB_EN_TR_DE_THUMBKEY_DUAL_MAIN = KeyboardC(letterRows(upper = false) + listOf(BOTTOM_ROW))
val KB_EN_TR_DE_THUMBKEY_DUAL_SHIFTED = KeyboardC(letterRows(upper = true) + listOf(BOTTOM_ROW))

// Numeric mode: the letter block with big digits in place of the letters, the same symbols and
// utility swipes, abc where the numeric toggle was, and F1 to F12 and insert on the h and i keys.
private val KB_EN_TR_DE_THUMBKEY_DUAL_NUMERIC =
    KeyboardC(
        letterRows(upper = false).mapIndexed { row, keys ->
            keys.mapIndexed { col, key ->
                val d = (row * 3 + col + 1).toString()
                when (row to col) {
                    0 to 2 -> {
                        key.copy(center = digit(d), left = ABC_TOGGLE_SMALL, longPress = null)
                    }

                    1 to 1 -> {
                        key.copy(
                            center = digit(d),
                            topLeft = fkey(1),
                            top = fkey(2),
                            topRight = fkey(3),
                            left = fkey(4),
                            right = fkey(5),
                            bottomLeft = fkey(6),
                            bottom = fkey(7),
                            bottomRight = fkey(8),
                            longPress = null,
                        )
                    }

                    2 to 1 -> {
                        key.copy(
                            center = digit(d),
                            top = fkey(9),
                            topRight = fkey(10),
                            right = fkey(11),
                            bottom = sendKey(KeyEvent.KEYCODE_INSERT, "ins"),
                            bottomRight = fkey(12),
                            longPress = null,
                        )
                    }

                    else -> {
                        key.copy(center = digit(d), longPress = null)
                    }
                }
            }
        } + listOf(BOTTOM_ROW_NUMERIC),
    )

// Characters typed with shift held on a US keyboard, mapped to their unshifted key.
private val SHIFTED_CHARS =
    mapOf(
        '!' to '1',
        '@' to '2',
        '#' to '3',
        '$' to '4',
        '%' to '5',
        '^' to '6',
        '&' to '7',
        '*' to '8',
        '(' to '9',
        ')' to '0',
        '_' to '-',
        '+' to '=',
        '{' to '[',
        '}' to ']',
        '|' to '\\',
        ':' to ';',
        '"' to '\'',
        '<' to ',',
        '>' to '.',
        '?' to '/',
        '~' to '`',
    )

private val PLAIN_KEYCODES =
    mapOf(
        ' ' to KeyEvent.KEYCODE_SPACE,
        '-' to KeyEvent.KEYCODE_MINUS,
        '=' to KeyEvent.KEYCODE_EQUALS,
        '[' to KeyEvent.KEYCODE_LEFT_BRACKET,
        ']' to KeyEvent.KEYCODE_RIGHT_BRACKET,
        '\\' to KeyEvent.KEYCODE_BACKSLASH,
        ';' to KeyEvent.KEYCODE_SEMICOLON,
        '\'' to KeyEvent.KEYCODE_APOSTROPHE,
        ',' to KeyEvent.KEYCODE_COMMA,
        '.' to KeyEvent.KEYCODE_PERIOD,
        '/' to KeyEvent.KEYCODE_SLASH,
        '`' to KeyEvent.KEYCODE_GRAVE,
    )

private fun keyCodeFor(c: Char): Pair<Int, Int>? {
    val base = SHIFTED_CHARS[c]
    val extraMeta = if (base != null) KeyEvent.META_SHIFT_ON else 0
    val ch = base ?: c
    val code =
        when {
            ch in 'a'..'z' -> KeyEvent.KEYCODE_A + (ch - 'a')
            ch in 'A'..'Z' -> KeyEvent.KEYCODE_A + (ch - 'A')
            ch in '0'..'9' -> KeyEvent.KEYCODE_0 + (ch - '0')
            else -> PLAIN_KEYCODES[ch] ?: return null
        }
    return code to extraMeta
}

// Characters become the matching key event with the modifier; existing key events (arrows, esc,
// tab, home, end, page keys, backspace) get the modifier added to what they already carry.
private fun KeyC.withModifier(meta: Int): KeyC =
    when (val action = this.action) {
        is CommitText -> {
            if (action.text.length != 1) return this
            val (code, extraMeta) = keyCodeFor(action.text[0]) ?: return this
            this.copy(action = SendEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, code, 0, meta or extraMeta)))
        }

        is SendEvent -> {
            val ev = action.event
            this.copy(action = SendEvent(KeyEvent(0, 0, ev.action, ev.keyCode, ev.repeatCount, ev.metaState or meta)))
        }

        else -> {
            this
        }
    }

private fun KeyItemC.withModifier(meta: Int): KeyItemC =
    this.copy(
        center = center.withModifier(meta),
        left = left?.withModifier(meta),
        topLeft = topLeft?.withModifier(meta),
        top = top?.withModifier(meta),
        topRight = topRight?.withModifier(meta),
        right = right?.withModifier(meta),
        bottomRight = bottomRight?.withModifier(meta),
        bottom = bottom?.withModifier(meta),
        bottomLeft = bottomLeft?.withModifier(meta),
        nextTapActions = null,
        longPress = null,
    )

private fun KeyboardC.withModifier(meta: Int): KeyboardC = KeyboardC(arr.map { row -> row.map { it.withModifier(meta) } })

// The return key is at row 3, column 0. In the modifier grids its center becomes a real enter
// key event so that ctrl+enter and alt+enter go through and the mode resets afterwards, and the
// corner that entered the mode turns into "cancel".
private fun modifiedReturn(meta: Int) =
    RETURN_KEYC.copy(action = SendEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER, 0, meta)))

val KB_EN_TR_DE_THUMBKEY_DUAL_CTRLED =
    KB_EN_TR_DE_THUMBKEY_DUAL_MAIN
        .withModifier(KeyEvent.META_CTRL_ON)
        .alterKey(3, 0) { it.copy(center = modifiedReturn(KeyEvent.META_CTRL_ON), topRight = TOGGLE_CTRL_FALSE_KEYC) }

val KB_EN_TR_DE_THUMBKEY_DUAL_ALTED =
    KB_EN_TR_DE_THUMBKEY_DUAL_MAIN
        .withModifier(KeyEvent.META_ALT_ON)
        .alterKey(3, 0) { it.copy(center = modifiedReturn(KeyEvent.META_ALT_ON), topLeft = TOGGLE_ALT_FALSE_KEYC) }

val KB_EN_TR_DE_THUMBKEY_DUAL: KeyboardDefinition =
    KeyboardDefinition(
        title = "english türkçe deutsch thumb-key dual",
        modes =
            KeyboardDefinitionModes(
                main = KB_EN_TR_DE_THUMBKEY_DUAL_MAIN,
                shifted = KB_EN_TR_DE_THUMBKEY_DUAL_SHIFTED,
                numeric = KB_EN_TR_DE_THUMBKEY_DUAL_NUMERIC,
                ctrled = KB_EN_TR_DE_THUMBKEY_DUAL_CTRLED,
                alted = KB_EN_TR_DE_THUMBKEY_DUAL_ALTED,
            ),
        settings =
            KeyboardDefinitionSettings(
                autoCapitalizers = arrayOf(::autoCapitalizeI, ::autoCapitalizeIApostrophe),
                textProcessor = DeadKeyCycleProcessor(),
            ),
    )
