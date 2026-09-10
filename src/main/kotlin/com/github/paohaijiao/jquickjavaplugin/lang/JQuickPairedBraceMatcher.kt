package com.github.paohaijiao.jquickjavaplugin.lang

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

/**
 * JQuick 括号配对（Paired Brace Matcher）。
 *
 * 向 IDE 声明本语言的成对括号 token 后，平台会自动提供两项能力：
 * <ul>
 *   <li><b>自动补右括号</b>：输入 `(`、`{`、`[` 时自动插入对应的 `)`、`}`、`]`；</li>
 *   <li><b>括号匹配</b>：光标停在括号上时高亮与之配对的另一端，并支持 Ctrl+M 跳转。</li>
 * </ul>
 *
 * 注意：配对识别依赖“左/右”为不同的 [IElementType]（见 [JQuickTokenTypes.LPAREN]
 * 与 [JQuickTokenTypes.RPAREN]），因此词法器不能把左右括号归并为同一个类型。
 *
 * <pre>
 * // 注册（plugin.xml）
 * &lt;lang.braceMatcher language="JQuick"
 *     implementationClass="...lang.JQuickPairedBraceMatcher"/&gt;
 * </pre>
 */
class JQuickPairedBraceMatcher : PairedBraceMatcher {

    override fun getPairs(): Array<BracePair> = PAIRS

    /** 允许在任意上下文成对插入（与 Java 一致：紧跟字符、空白、换行等情况都补右括号）。 */
    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true

    /** 折叠/结构起点即左括号本身。 */
    override fun getCodeConstructStart(file: PsiFile, openingBraceOffset: Int): Int = openingBraceOffset

    private companion object {
        /** 花括号用于代码块折叠（structural=true），圆/方括号仅参与配对与自动补全。 */
        val PAIRS: Array<BracePair> = arrayOf(
            BracePair(JQuickTokenTypes.LBRACE, JQuickTokenTypes.RBRACE, true),
            BracePair(JQuickTokenTypes.LPAREN, JQuickTokenTypes.RPAREN, false),
            BracePair(JQuickTokenTypes.LBRACKET, JQuickTokenTypes.RBRACKET, false)
        )
    }
}
