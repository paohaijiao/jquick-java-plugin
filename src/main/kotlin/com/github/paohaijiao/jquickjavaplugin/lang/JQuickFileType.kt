package com.github.paohaijiao.jquickjavaplugin.lang

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

/**
 * JQuick 文件类型定义（File Type Definition）。
 *
 * 通过 plugin.xml 中的 fileType 扩展点注册，扩展名为 .jquick。
 *
 * <pre>
 * // 使用示例
 * val fileType = JQuickFileType
 * fileType.defaultExtension        // -> "jquick"
 * </pre>
 */
object JQuickFileType : LanguageFileType(JQuickLanguage) {

    /** 文件类型名称（在“文件类型”设置中显示）。 */
    override fun getName(): String = "JQuick"

    /** 文件类型描述。 */
    override fun getDescription(): String = "JQuick Java script file"

    /** 默认扩展名。 */
    override fun getDefaultExtension(): String = JQuickLanguage.FILE_EXTENSION

    /** 项目树/编辑器标签中显示的图标。 */
    override fun getIcon(): Icon = AllIcons.FileTypes.Any_type
}
