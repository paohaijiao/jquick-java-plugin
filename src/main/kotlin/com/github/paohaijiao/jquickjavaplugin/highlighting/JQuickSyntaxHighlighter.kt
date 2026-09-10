package com.github.paohaijiao.jquickjavaplugin.highlighting

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.IElementType
import com.github.paohaijiao.jquickjavaplugin.lang.JQuickAntlrLexer
import com.github.paohaijiao.jquickjavaplugin.lang.JQuickTokenTypes

/**
 * JQuick 语法高亮器：把 Lexer 返回的分组 token 映射为 IDE 预设的着色 key。
 *
 * 全部复用 [DefaultLanguageHighlighterColors]，因此用户可以像 Java/Kotlin
 * 一样在 Settings → Editor → Color Scheme 中直接调整各类颜色，无需额外配色页。
 */
class JQuickSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer(): Lexer = JQuickAntlrLexer()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> {
        val key = when (tokenType) {
            JQuickTokenTypes.KEYWORD -> DefaultLanguageHighlighterColors.KEYWORD
            JQuickTokenTypes.IDENTIFIER -> DefaultLanguageHighlighterColors.IDENTIFIER
            JQuickTokenTypes.STRING -> DefaultLanguageHighlighterColors.STRING
            JQuickTokenTypes.NUMBER -> DefaultLanguageHighlighterColors.NUMBER
            JQuickTokenTypes.LINE_COMMENT -> DefaultLanguageHighlighterColors.LINE_COMMENT
            JQuickTokenTypes.BLOCK_COMMENT -> DefaultLanguageHighlighterColors.BLOCK_COMMENT
            JQuickTokenTypes.OPERATOR -> DefaultLanguageHighlighterColors.OPERATION_SIGN
            JQuickTokenTypes.DOT -> DefaultLanguageHighlighterColors.DOT
            JQuickTokenTypes.SEMICOLON -> DefaultLanguageHighlighterColors.SEMICOLON
            JQuickTokenTypes.COMMA -> DefaultLanguageHighlighterColors.COMMA
            JQuickTokenTypes.PARENS -> DefaultLanguageHighlighterColors.PARENTHESES
            JQuickTokenTypes.BRACES -> DefaultLanguageHighlighterColors.BRACES
            JQuickTokenTypes.BRACKETS -> DefaultLanguageHighlighterColors.BRACKETS
            else -> null
        }
        return if (key == null) emptyArray() else arrayOf(key)
    }
}

/**
 * JQuick 高亮器工厂（plugin.xml 的 lang.syntaxHighlighterFactory 入口）。
 *
 * <pre>
 * &lt;lang.syntaxHighlighterFactory language="JQuick"
 *     implementationClass="...highlighting.JQuickSyntaxHighlighterFactory"/&gt;
 * </pre>
 */
class JQuickSyntaxHighlighterFactory : SyntaxHighlighterFactory() {

    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
        JQuickSyntaxHighlighter()
}
