package com.github.paohaijiao.jquickjavaplugin.engine

import com.github.paohaijiao.lsp.JQuickDocumentManager
import com.github.paohaijiao.lsp.JQuickSymbolCollector

/**
 * jquick-java 引擎的进程内统一访问入口（供语法查错、代码补全等功能复用）。
 *
 * 背景：JQuickJava.g4 在“解析动作”中会读写 parser 内静态的 console/context/scope
 * 状态（例如解析到未定义变量会直接向 console 打印错误），因此多线程并发调用
 * [JQuickDocumentManager.parseText] 会互相污染。本入口用一个全局锁把所有解析
 * 调用串行化；Annotator 与 Completion 都必须经过这里，保证互斥。
 *
 * <pre>
 * // 使用示例
 * JQuickEngineAccess.diagnostics(text)   // -> 语法诊断（Annotator）
 * JQuickEngineAccess.symbolTable(text)   // -> 文档符号表（Completion）
 * </pre>
 */
object JQuickEngineAccess {

    /** 解析锁：所有经过引擎的解析都在该锁内串行执行。 */
    private val parseLock = Any()

    /** 解析文本并对结果做映射；解析抛出异常时返回 null。 */
    private fun <T> locked(text: String, mapper: (JQuickDocumentManager.ParseResult) -> T): T? =
        synchronized(parseLock) {
            runCatching { JQuickDocumentManager.parseText(text) }
                .getOrNull()
                ?.let(mapper)
        }

    /** 解析文本并返回 LSP 风格诊断列表；失败时返回 null。 */
    fun diagnostics(text: String): List<JQuickDocumentManager.Diagnostic>? =
        locked(text) { it.diagnostics }

    /**
     * 解析文本并收集文档符号表（函数/变量/参数/import 别名）。
     * 解析失败或没有语法树时返回 null。
     */
    fun symbolTable(text: String): JQuickSymbolCollector.SymbolTable? {
        val tree = locked(text) { it.tree } ?: return null
        return runCatching { JQuickSymbolCollector.collect(tree) }.getOrNull()
    }
}
