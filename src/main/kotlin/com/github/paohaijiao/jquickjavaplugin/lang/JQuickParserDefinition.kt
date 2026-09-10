package com.github.paohaijiao.jquickjavaplugin.lang

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

/**
 * JQuick 轻量 PSI 解析器：只把词法 token 平铺到文件根节点下，不做结构归并。
 *
 * 这样既能给 JQuick 文件提供真实的 PSI（后续 Annotator / 结构浏览 / 快速修复
 * 都以 PSI 为基础），又无需为本插件的目标（语法高亮 + 查错诊断）编写完整
 * 语法树。
 */
object JQuickFlatParser : PsiParser {

    override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
        val marker = builder.mark()
        while (!builder.eof()) {
            builder.advanceLexer()
        }
        marker.done(root)
        return builder.treeBuilt
    }
}

/** JQuick 文件的 PSI 根元素。 */
class JQuickPsiFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, JQuickLanguage) {

    override fun getFileType(): FileType = JQuickFileType

    override fun toString(): String = "JQuick File"
}

/**
 * JQuick 的 ParserDefinition：文件节点 + 词法器 + 平铺解析器 + 空白/注释声明。
 *
 * <pre>
 * // 注册（plugin.xml）
 * &lt;lang.parserDefinition language="JQuick"
 *     implementationClass="...lang.JQuickParserDefinition"/&gt;
 * </pre>
 */
class JQuickParserDefinition : ParserDefinition {

    override fun createLexer(project: Project): Lexer = JQuickAntlrLexer()

    override fun createParser(project: Project): PsiParser = JQuickFlatParser

    override fun getFileNodeType(): IFileElementType = FILE_NODE_TYPE

    override fun getWhitespaceTokens(): TokenSet = TokenSet.create(JQuickTokenTypes.WHITE_SPACE)

    override fun getCommentTokens(): TokenSet = TokenSet.create(JQuickTokenTypes.LINE_COMMENT, JQuickTokenTypes.BLOCK_COMMENT)

    override fun getStringLiteralElements(): TokenSet = TokenSet.create(JQuickTokenTypes.STRING)

    override fun createElement(node: ASTNode): PsiElement = ASTWrapperPsiElement(node)

    override fun createFile(viewProvider: FileViewProvider): PsiFile = JQuickPsiFile(viewProvider)

    companion object {
        /** 文件节点的 IElementType（根节点类型即语言自身）。 */
        val FILE_NODE_TYPE: IFileElementType = IFileElementType(JQuickLanguage)
    }
}
