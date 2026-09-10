package com.github.paohaijiao.jquickjavaplugin.lang

import com.intellij.lang.Language

/**
 * JQuick 脚本语言定义（Language Definition）。
 *
 * 该对象通过唯一 id "JQuick" 在 IntelliJ 平台注册，后续 FileType、
 * SyntaxHighlighter、Annotator 等均以它作为语言关联键。
 *
 * <pre>
 * // 使用示例
 * if (element.language == JQuickLanguage) { ... }
 * </pre>
 */
object JQuickLanguage : Language("JQuick") {

    /** 展示名（用户界面/设置中显示）。 */
    const val DISPLAY_NAME: String = "JQuick"

    /** 默认文件扩展名。 */
    const val FILE_EXTENSION: String = "jquick"
}
