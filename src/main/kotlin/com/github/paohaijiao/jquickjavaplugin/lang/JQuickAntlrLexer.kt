package com.github.paohaijiao.jquickjavaplugin.lang

import com.intellij.lexer.Lexer
import com.intellij.lexer.LexerPosition
import com.intellij.lexer.LexerPositionImpl
import com.intellij.psi.tree.IElementType
import com.github.paohaijiao.parser.JQuickJavaLexer
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.Vocabulary

/**
 * ANTLR4 词法器桥接：把 jquick-java 的 [JQuickJavaLexer] 适配为 IntelliJ [Lexer]。
 *
 * 背景：jquick 语法把注释通过 `-> skip` 丢弃、把空白送入 HIDDEN 通道，
 * 因此不能直接把 ANTLR token 流交给 IDE（会出现“文本空洞”）。本适配器一次性
 * 词法分析整段文本，把跳过的注释/空白区域补回为对应分组 token，从而保证
 * 返回的 token 序列能够**连续、无空洞**地覆盖 [startOffset, endOffset)，
 * 供高亮器与轻量 PSI 使用。
 *
 * <pre>
 * // 使用示例
 * val lexer = JQuickAntlrLexer()
 * lexer.start(editor.document.charsSequence, 0, text.length, 0)
 * while (lexer.tokenType != null) {
 *     val type = lexer.tokenType       // JQuickTokenTypes.* 之一
 *     val text  = lexer.tokenStart until lexer.tokenEnd
 *     lexer.advance()
 * }
 * </pre>
 */
class JQuickAntlrLexer : Lexer() {

    private var cachedSequence: CharSequence? = null

    private val tokenTypes = ArrayList<IElementType>()

    private val tokenStarts = ArrayList<Int>()

    private val tokenEnds = ArrayList<Int>()

    private var tokenCount = 0

    private var myBuffer: CharSequence = ""

    private var myStartOffset = 0

    private var myEndOffset = 0

    private var currentIndex = 0

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        myBuffer = buffer
        myStartOffset = startOffset
        myEndOffset = endOffset
        if (cachedSequence !== buffer) {
            tokenize(buffer.toString())
            cachedSequence = buffer
        }
        currentIndex = firstTokenEndingAfter(startOffset)
    }

    override fun advance() {
        currentIndex++
    }

    override fun getTokenType(): IElementType? {
        if (currentIndex >= tokenCount) return null
        val start = getTokenStart()
        if (start >= myEndOffset) return null
        return tokenTypes[currentIndex]
    }

    override fun getTokenStart(): Int {
        if (currentIndex >= tokenCount) return myEndOffset
        return maxOf(tokenStarts[currentIndex], myStartOffset)
    }

    override fun getTokenEnd(): Int {
        if (currentIndex >= tokenCount) return myEndOffset
        return minOf(tokenEnds[currentIndex], myEndOffset)
    }

    override fun getState(): Int = 0

    override fun getCurrentPosition(): LexerPosition {
        val offset = if (currentIndex < tokenCount) getTokenStart() else myEndOffset
        return LexerPositionImpl(offset, 0)
    }

    override fun restore(position: LexerPosition) {
        currentIndex = firstTokenEndingAfter(position.offset)
    }

    override fun getBufferSequence(): CharSequence = myBuffer

    override fun getBufferEnd(): Int = myEndOffset


    /**
     * 全量词法分析文本，产出“连续覆盖全文”的分组 token 列表。
     * 空白（HIDDEN 通道）与跳过的注释之间的空洞会被补全。
     */
    private fun tokenize(text: String) {
        tokenTypes.clear()
        tokenStarts.clear()
        tokenEnds.clear()
        tokenCount = 0

        val lexer = JQuickJavaLexer(CharStreams.fromString(text))
        lexer.removeErrorListeners()
        val vocabulary: Vocabulary = lexer.vocabulary

        var previousEnd = 0
        var token = lexer.nextToken()
        while (token.type != Token.EOF) {
            val start = token.startIndex
            val end = token.stopIndex + 1
            if (previousEnd < start) {
                fillGap(text, previousEnd, start)
            }
            tokenTypes.add(classify(token.type, text.substring(start, end), vocabulary))
            tokenStarts.add(start)
            tokenEnds.add(end)
            tokenCount++
            previousEnd = end
            token = lexer.nextToken()
        }
        if (previousEnd < text.length) {
            fillGap(text, previousEnd, text.length)
        }
    }

    /** 补全两个相邻 token 之间的空洞（只会是跳过的注释或极少数无法识别的字符）。 */
    private fun fillGap(text: String, from: Int, to: Int) {
        val gap = text.substring(from, to)
        val trimmed = gap.trimStart()
        val type: IElementType = when {
            trimmed.isBlank() -> JQuickTokenTypes.WHITE_SPACE
            trimmed.startsWith("//") -> JQuickTokenTypes.LINE_COMMENT
            trimmed.startsWith("/*") || trimmed.endsWith("*/") -> JQuickTokenTypes.BLOCK_COMMENT
            else -> JQuickTokenTypes.IDENTIFIER // 兜底，保证连续覆盖
        }
        tokenTypes.add(type)
        tokenStarts.add(from)
        tokenEnds.add(to)
        tokenCount++
    }

    /** 把 ANTLR token 归并为高亮分组。 */
    private fun classify(type: Int, text: String, vocabulary: Vocabulary): IElementType {
        if (text.isBlank()) return JQuickTokenTypes.WHITE_SPACE
        val symbolic = vocabulary.getSymbolicName(type)
        if (symbolic != null) {
            when (symbolic) {
                "IDENTIFIER" -> return JQuickTokenTypes.IDENTIFIER
                "STRING" -> return JQuickTokenTypes.STRING
                "NUMBERIC", "DATE", "DATETIME" -> return JQuickTokenTypes.NUMBER
                "WS", "NEWLINE" -> return JQuickTokenTypes.WHITE_SPACE
                "LPAREN", "RPAREN" -> return JQuickTokenTypes.PARENS
                "LBRACE", "RBRACE" -> return JQuickTokenTypes.BRACES
                "SEMICOLON" -> return JQuickTokenTypes.SEMICOLON
                "DOT" -> return JQuickTokenTypes.DOT
                "COLON", "ASSIGN", "GT", "GE", "LT", "LE", "EQ", "NE",
                "AND", "OR", "ADD", "MINUS", "MUL", "DIV", "DOLLAR" ->
                    return JQuickTokenTypes.OPERATOR
            }
            if (symbolic in KEYWORD_SYMBOLIC_NAMES) return JQuickTokenTypes.KEYWORD
        }
        val literal = vocabulary.getLiteralName(type)
        if (literal != null) {
            classifyLiteral(literal)?.let { return it }
        }
        return classifyByText(text)
    }

    /** 匿名内联字面量（形如 'if'、'['、',' 等）的分组。 */
    private fun classifyLiteral(literal: String): IElementType? = when (literal) {
        "','" -> JQuickTokenTypes.COMMA
        "'['", "']'" -> JQuickTokenTypes.BRACKETS
        "'@'" -> JQuickTokenTypes.OPERATOR
        "'.'" -> JQuickTokenTypes.DOT
        "';'" -> JQuickTokenTypes.SEMICOLON
        "'('", "')'" -> JQuickTokenTypes.PARENS
        "'{'", "'}'" -> JQuickTokenTypes.BRACES
        else -> if (literal in KEYWORD_LITERALS) JQuickTokenTypes.KEYWORD else null
    }

    /** 纯文本兜底分类（正常不会触发）。 */
    private fun classifyByText(text: String): IElementType = when (text) {
        ";", ",", ".", "(", ")", "[", "]", "{", "}", "@" -> JQuickTokenTypes.OPERATOR
        else -> JQuickTokenTypes.IDENTIFIER
    }

    /** 定位第一个结束位置 > startOffset 的 token 下标。 */
    private fun firstTokenEndingAfter(offset: Int): Int {
        var low = 0
        var high = tokenCount
        while (low < high) {
            val mid = (low + high) ushr 1
            if (tokenEnds[mid] <= offset) low = mid + 1 else high = mid
        }
        return low
    }

    private companion object {
        /** 关键字类命名 token（来自 JQuickJava.g4 词法规则）。 */
        val KEYWORD_SYMBOLIC_NAMES: Set<String> = setOf(
            "TYPEINT", "TYPEFLOAT", "TYPEDOUBLE", "TYPELONG", "TYPEBOOLEAN",
            "TYPEBYTE", "TYPESHORT", "TYPENULL",
            "THIS", "CONTINUE", "BREAK", "IMPORT", "NEW", "VAR", "AS",
            "RETURN", "DEF", "WHILE", "FOR", "TRUE", "FALSE",
            "IF", "THEN", "ELSEIF", "ELSE", "WITH",
            "CONTAIN", "NOTCONTAIN", "START", "NOTSTART", "END", "NOTEND",
            "BUILTIN"
        )

        /** 关键字类内联字面量（没有对应命名词法规则）。 */
        val KEYWORD_LITERALS: Set<String> = setOf(
            "'if'", "'else if'", "'else'", "'console.log'",
            "'List'", "'Set'", "'Map'"
        )
    }
}
