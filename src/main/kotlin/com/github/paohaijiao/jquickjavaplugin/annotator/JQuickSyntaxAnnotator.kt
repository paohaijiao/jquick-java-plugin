package com.github.paohaijiao.jquickjavaplugin.annotator

import com.github.paohaijiao.jquickjavaplugin.engine.JQuickEngineAccess
import com.github.paohaijiao.jquickjavaplugin.lang.JQuickLanguage
import com.github.paohaijiao.lsp.JQuickDocumentManager
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile

/**
 * JQuick 语法查错器（外部 Annotator）。
 *
 * 直接复用 jquick-java 的 LSP 引擎 [JQuickDocumentManager.parseText]：
 * 引擎基于 ANTLR4 的错误监听器返回 LSP 风格的词法/语法诊断
 * （line/character 均为 0-based、UTF-16 码元，与 IntelliJ 偏移约定一致），
 * 这里把每条诊断换算成编辑器的 TextRange 并以红色波浪线/提示呈现。
 *
 * 因为走 [ExternalAnnotator]，真正解析发生在后台线程，不会阻塞 UI。
 */
class JQuickSyntaxAnnotator : ExternalAnnotator<JQuickSyntaxAnnotator.FileSnapshot, List<JQuickDocumentManager.Diagnostic>>() {

    class FileSnapshot(val text: String)

    override fun collectInformation(file: PsiFile, editor: Editor, hasErrors: Boolean): FileSnapshot? {
        if (file.language != JQuickLanguage) return null
        return FileSnapshot(file.text)
    }

    override fun doAnnotate(collectedInfo: FileSnapshot?): List<JQuickDocumentManager.Diagnostic>? {
        if (collectedInfo == null) return null
        return JQuickEngineAccess.diagnostics(collectedInfo.text)
    }

    override fun apply(file: PsiFile, annotationResult: List<JQuickDocumentManager.Diagnostic>?, holder: AnnotationHolder) {
        if (annotationResult.isNullOrEmpty()) return
        val document = file.viewProvider.document ?: return
        for (diagnostic in annotationResult) {
            val start = offsetOf(document, diagnostic.startLine, diagnostic.startChar)
            val end = offsetOf(document, diagnostic.endLine, diagnostic.endChar)
            if (start >= document.textLength) continue
            val rangeEnd = if (end > start) end else minOf(start + 1, document.textLength)
            val range = TextRange.create(start, rangeEnd)
            val severity = when (diagnostic.severity) {
                2 -> HighlightSeverity.WARNING
                3 -> HighlightSeverity.INFORMATION
                else -> HighlightSeverity.ERROR
            }
            holder.newAnnotation(severity, diagnostic.message).range(range).create()
        }
    }
    private fun offsetOf(document: Document, line: Int, column: Int): Int {
        if (line < 0) return 0
        if (line >= document.lineCount) return document.textLength
        val lineStart = document.getLineStartOffset(line)
        val lineEnd = document.getLineEndOffset(line)
        return minOf(lineStart + column, lineEnd)
    }
}
