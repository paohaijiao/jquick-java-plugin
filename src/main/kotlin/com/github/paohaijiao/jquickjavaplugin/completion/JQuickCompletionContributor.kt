package com.github.paohaijiao.jquickjavaplugin.completion

import com.github.paohaijiao.jquickjavaplugin.engine.JQuickEngineAccess
import com.github.paohaijiao.jquickjavaplugin.lang.JQuickLanguage
import com.github.paohaijiao.jquickjavaplugin.lang.JQuickTokenTypes
import com.github.paohaijiao.lsp.JQuickSymbolCollector
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PlatformPatterns.psiFile
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.ProcessingContext
import javax.swing.Icon

/**
 * JQuick 代码补全（Code Completion / 提示与补全）。
 *
 * 提示内容与 jquick-java 的 LSP 补全保持一致，分三类：
 * <ol>
 *   <li><b>文档语义符号</b>：复用引擎 [JQuickSymbolCollector] 收集本文件中的
 *       函数（def）、变量、参数、import 别名；</li>
 *   <li><b>内置类型</b>：int/float/.../List/Set/Map/Builtin；</li>
 *   <li><b>语言关键字</b>：if/else/for/while/def/import/var/.../console.log。</li>
 * </ol>
 *
 * <b>函数调用自动带括号与参数：</b>函数符号按“调用”补全，补全文本直接是
 * `name(...)` —— 无参为 `name()`，有参则从引擎签名（detail）解析出参数名
 * 拼入，例如 `add(a, b)`，选中的提示项与插入文本都带括号与参数。唯一例外是
 * 函数名正处在自己的定义处（前一个词是 `def`），此时只给名称本身。
 *
 * 因为本插件只建轻量扁平 PSI（token 直接挂到文件根下），补全不做 AST 作用域
 * 推断，直接提供“全量提示”交给 IDE 按输入前缀过滤；同时在字符串、注释内部
 * 以及成员访问（点号）之后不做提示，避免无意义干扰。
 *
 * <pre>
 * // 注册（plugin.xml）
 * &lt;completion.contributor language="JQuick"
 *     implementationClass="...completion.JQuickCompletionContributor"/&gt;
 * </pre>
 */
class JQuickCompletionContributor : CompletionContributor() {

    init {
        extend(
            CompletionType.BASIC,
            psiElement().inFile(psiFile().withLanguage(JQuickLanguage)),
            JQuickCompletionProvider()
        )
    }
}

/** 补全计算主体。 */
private class JQuickCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
        val element = parameters.position ?: return
        val file = element.containingFile ?: return
        if (file.language != JQuickLanguage) return
        val type = PsiUtilCore.getElementType(element)
        if (type == JQuickTokenTypes.STRING ||
            type == JQuickTokenTypes.NUMBER ||
            type == JQuickTokenTypes.LINE_COMMENT ||
            type == JQuickTokenTypes.BLOCK_COMMENT
        ) {
            return
        }

        val text = file.text
        val caret = parameters.offset
        if (caret > 0) {
            val prev = text.getOrNull(caret - 1)
            if (prev == '.' || prev == '@') return
        }
        val definingFunction = prevWordBefore(text, caret) == DEF_KEYWORD
        val table = JQuickEngineAccess.symbolTable(text)
        if (table != null) {
            addFunctions(result, table.functions, definingFunction)
            addItems(result, table.variables, AllIcons.Nodes.Variable, SYMBOL_PRIORITY)
            addItems(result, table.params, AllIcons.Nodes.Parameter, SYMBOL_PRIORITY)
            addItems(result, table.imports, AllIcons.Nodes.Package, SYMBOL_PRIORITY)
        }
        for (t in TYPE_KEYWORDS) {
            addItem(result, t, AllIcons.Nodes.Class, TYPE_PRIORITY)
        }
        for (kw in KEYWORDS) {
            addItem(result, kw, null, KEYWORD_PRIORITY)
        }
    }

    /**
     * 追加函数补全项：默认拼上调用括号与参数名（如 `add(a, b)`）；
     * [definingFunction] 为 true（正处于 `def` 定义名）时只给名称。
     */
    private fun addFunctions(result: CompletionResultSet, symbols: MutableList<JQuickSymbolCollector.Symbol>, definingFunction: Boolean) {
        for (symbol in symbols) {
            val name = symbol.name
            val detail = symbol.detail
            val params = paramNames(detail)
            val insertText = if (definingFunction) name
            else name + (if (params.isEmpty()) "()" else "(" + params.joinToString(", ") + ")")
            val builder = LookupElementBuilder.create(insertText)
                .withIcon(AllIcons.Nodes.Method)
                .withLookupString(name)
                .withTailText(detail, true)
            result.addElement(PrioritizedLookupElement.withPriority(builder, SYMBOL_PRIORITY))
        }
    }

    /**
     * 从引擎签名 detail（形如 `int def add(int: a, int: b)`）中解析参数名。
     * 类型里的泛型可能含逗号（如 `Map<String, int>`），这里用“取冒号后片段”的方式
     * 只关心形如 `类型: 名字` 的段落，冒号前属于类型文本（可能被逗号切碎）则忽略。
     */
    private fun paramNames(detail: String?): List<String> {
        if (detail == null) return emptyList()
        val open = detail.indexOf('(')
        val close = detail.lastIndexOf(')')
        if (open < 0 || close <= open + 1) return emptyList() // 无参数
        return detail.substring(open + 1, close)
            .split(',')
            .mapNotNull { piece ->
                val colon = piece.lastIndexOf(':')
                if (colon < 0) return@mapNotNull null
                piece.substring(colon + 1).trim().ifEmpty { null }
            }
    }

    /**
     * 取 [caret] 位置之前“当前输入前缀再往前”的最后一个词（跳过空白），
     * 例如文本 `int def ad`、caret 在末尾时返回 `def`。
     */
    private fun prevWordBefore(text: CharSequence, caret: Int): String? {
        var p = caret
        while (p > 0 && (text[p - 1].isLetterOrDigit() || text[p - 1] == '_')) p--
        while (p > 0 && text[p - 1].isWhitespace()) p--
        val end = p
        while (p > 0 && (text[p - 1].isLetterOrDigit() || text[p - 1] == '_')) p--
        return if (p < end) text.substring(p, end) else null
    }

    /** 追加一批引擎符号为补全项（detail 作为灰色尾部说明）。 */
    private fun addItems(
        result: CompletionResultSet,
        symbols: MutableList<JQuickSymbolCollector.Symbol>,
        icon: Icon?,
        priority: Double
    ) {
        for (symbol in symbols) {
            val builder = LookupElementBuilder.create(symbol.name).withIcon(icon)
            val element = symbol.detail?.let { builder.withTailText(it, true) } ?: builder
            result.addElement(PrioritizedLookupElement.withPriority(element, priority))
        }
    }

    /** 追加单个（类型/关键字）补全项。 */
    private fun addItem(result: CompletionResultSet, name: String, icon: Icon?, priority: Double) {
        val element: LookupElement = LookupElementBuilder.create(name).withIcon(icon)
        result.addElement(PrioritizedLookupElement.withPriority(element, priority))
    }

    private companion object {
        /** 函数定义关键字（识别“定义处”）。 */
        const val DEF_KEYWORD: String = "def"

        /** 关键字列表（与 jquick-java JQuickLspProvider.KEYWORDS 保持一致）。 */
        val KEYWORDS: Array<String> = arrayOf(
            "if", "else if", "else", "for", "while", "return", DEF_KEYWORD,
            "import", "as", "new", "var", "break", "continue",
            "true", "false", "null", "this", "console.log"
        )

        /** 内置类型列表（与 jquick-java JQuickLspProvider.TYPES 保持一致）。 */
        val TYPE_KEYWORDS: Array<String> = arrayOf(
            "int", "float", "double", "long", "boolean", "byte", "short",
            "List", "Set", "Map", "Builtin"
        )

        /** 优先级：用户定义的符号排最前，类型其次，关键字最后。 */
        const val SYMBOL_PRIORITY: Double = 200.0
        const val TYPE_PRIORITY: Double = 120.0
        const val KEYWORD_PRIORITY: Double = 0.0
    }
}
