package com.github.paohaijiao.jquickjavaplugin.lang

import com.intellij.psi.tree.IElementType

/**
 * JQuick 词法单元（Token）类型定义。
 *
 * 由于本插件采用“仅高亮 + 轻量 PSI”策略（不做完整语法树），
 * 这里不按 ANTLR 每个词法规则逐一创建 IElementType，而是归并为若干
 * 语义分组（关键字/字符串/数字/注释/括号/运算符等）。每个分组对应
 * 一种着色方式，既足够还原 JQuick 语法高亮，又避免产生海量类型。
 *
 * <pre>
 * // 使用示例（在 Lexer 与 SyntaxHighlighter 中）
 * JQuickTokenTypes.KEYWORD   // -> 关键字分组
 * JQuickTokenTypes.STRING    // -> 字符串字面量分组
 * </pre>
 */
object JQuickTokenTypes {

    /** 空白（仅用于 Lexer 衔接与 PSI 剔除，不参与着色）。 */
    val WHITE_SPACE: IElementType = IElementType("JQUICK_WHITE_SPACE", JQuickLanguage)

    /** 关键字（if/for/while/return/import/类型关键字/true/false 等）。 */
    val KEYWORD: IElementType = IElementType("JQUICK_KEYWORD", JQuickLanguage)

    /** 标识符（变量名、方法名、类型全限定名片段等）。 */
    val IDENTIFIER: IElementType = IElementType("JQUICK_IDENTIFIER", JQuickLanguage)

    /** 字符串字面量（单引号或双引号）。 */
    val STRING: IElementType = IElementType("JQUICK_STRING", JQuickLanguage)

    /** 数值字面量（整型/浮点/日期等）。 */
    val NUMBER: IElementType = IElementType("JQUICK_NUMBER", JQuickLanguage)

    /** 单行注释（// ...）。 */
    val LINE_COMMENT: IElementType = IElementType("JQUICK_LINE_COMMENT", JQuickLanguage)

    /** 块注释（斜杠星号风格）。 */
    val BLOCK_COMMENT: IElementType = IElementType("JQUICK_BLOCK_COMMENT", JQuickLanguage)

    /** 运算符（+ - * / == != > < >= <= && || = : @ $ 等）。 */
    val OPERATOR: IElementType = IElementType("JQUICK_OPERATOR", JQuickLanguage)

    /** 点号（成员访问 . 以及 ::）。 */
    val DOT: IElementType = IElementType("JQUICK_DOT", JQuickLanguage)

    /** 分号 ;。 */
    val SEMICOLON: IElementType = IElementType("JQUICK_SEMICOLON", JQuickLanguage)

    /** 逗号 ,。 */
    val COMMA: IElementType = IElementType("JQUICK_COMMA", JQuickLanguage)

    /** 圆括号 ( )。 */
    val PARENS: IElementType = IElementType("JQUICK_PARENS", JQuickLanguage)

    /** 花括号 { }。 */
    val BRACES: IElementType = IElementType("JQUICK_BRACES", JQuickLanguage)

    /** 方括号 [ ]。 */
    val BRACKETS: IElementType = IElementType("JQUICK_BRACKETS", JQuickLanguage)
}
