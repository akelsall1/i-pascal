package com.siberika.idea.pascal.lang.lexer;

import com.intellij.lexer.FlexAdapter;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.TokenType;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.text.CharArrayCharSequence;
import com.siberika.idea.pascal.PascalFileType;
import com.siberika.idea.pascal.sdk.DefinesParser;
import com.siberika.idea.pascal.sdk.FPCSdkType;
import com.siberika.idea.pascal.util.FileUtil;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Author: George Bakhtadze
 * Date: 05/04/2013
 */
public class PascalFlexLexerImpl extends _PascalLexer {

    private int curLevel = -1;
    private int inactiveLevel = -1;

    private Set<String> defines = new HashSet<String>();

    Project project;
    VirtualFile virtualFile;

    public PascalFlexLexerImpl(Reader in, Project project, VirtualFile virtualFile) {
        super(in);
        this.project = project;
        this.virtualFile = virtualFile;
    }

    @Override
    public CharSequence getIncludeContent(CharSequence text) {
        return new CharArrayCharSequence("{Some text}".toCharArray());
    }

    @Override
    public void define(CharSequence sequence) {
        String name = extractDefineName(sequence);
        defines.add(name);
    }

    @Override
    public void unDefine(CharSequence sequence) {
        String name = extractDefineName(sequence);
        defines.remove(name);
    }

    @Override
    public void clearDefines() {
        defines.clear();
        if ((project != null) && (virtualFile != null)) {
            Module module = ModuleUtil.findModuleForFile(virtualFile, project);
            if (module != null) {
                final Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
                if ((null != sdk) && (sdk.getSdkType() instanceof FPCSdkType)) {
                    defines.addAll(DefinesParser.getDefaultDefines(DefinesParser.COMPILER_FPC, sdk.getVersionString()));
                }
            }
        }
    }

    private IElementType doHandleIfDef(CharSequence sequence, boolean negate) {
        String name = extractDefineName(sequence);
        curLevel++;
        if ((!defines.contains(name) ^ negate) && (!isInactive())) {
            inactiveLevel = curLevel;
            yybegin(INACTIVE_BRANCH);
        }
        return CT_DEFINE;
    }

    @Override
    public IElementType handleIfDef(CharSequence sequence) {
        return doHandleIfDef(sequence, false);
    }

    @Override
    public IElementType handleIfNDef(CharSequence sequence) {
        return doHandleIfDef(sequence, true);
    }

    @Override
    public IElementType handleElse() {
        if (curLevel < 0) { return TokenType.BAD_CHARACTER; }
        if (isInactive()) {
            if (curLevel == inactiveLevel) {
                yybegin(YYINITIAL);
            }
        } else {
            inactiveLevel = curLevel;
            yybegin(INACTIVE_BRANCH);
        }
        return CT_DEFINE;
    }

    @Override
    public IElementType handleEndIf() {
        if (curLevel < 0) { return TokenType.BAD_CHARACTER; }
        if (curLevel == inactiveLevel) {
            yybegin(YYINITIAL);
        }
        curLevel--;
        return CT_DEFINE;
    }

    @Override
    public IElementType handleInclude(CharSequence sequence) {
        String name = extractIncludeName(sequence);

        if ((project != null) && (virtualFile != null)) {
            try {
                VirtualFile incFile = getIncludedFile(project, virtualFile, name);
                if (incFile != null) {
                    Reader reader = new FileReader(incFile.getCanonicalPath());
                    PascalFlexLexerImpl lexer = new PascalFlexLexerImpl(reader, project, incFile);
                    Document doc = FileDocumentManager.getInstance().getDocument(incFile);
                    if (doc != null) {
                        lexer.reset(doc.getCharsSequence(), 0);
                        FlexAdapter flexAdapter = new FlexAdapter(lexer);
                        while (flexAdapter.getTokenType() != null) {
                            flexAdapter.advance();
                        }
                        defines.addAll(lexer.defines);
                    }
                } else {
                    System.out.println("*** include " + name + " referenced from " + virtualFile.getName() + " not found");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return CT_DEFINE;
    }

    /**
     * Locates file specified in include directive and its full file name trying the following:
     *   1. if name specifies an absolute path return it
     *   2. search in the directory where the current source file is located
     *   3. search in all paths in search path
     *   if name doesn't include file extension and file doesn't exists ".pas" and ".pp" are tried sequentially
     * @param project - used to retrieve list of search paths project
     * @param referencing - file which references to the include
     * @param name - name found in include directive
     * @return file name or null if not found
     */
    private VirtualFile getIncludedFile(Project project, VirtualFile referencing, String name) throws IOException {
        File file = new File(name);
        if (file.isAbsolute()) {
            return tryExtensions(file);
        }

        if (referencing != null) {
            String path = referencing.getParent().getPath();
            VirtualFile result = tryExtensions(new File(path, name));
            if (result != null) {
                return result;
            }

            Module module = ModuleUtil.findModuleForFile(referencing, project);

            return trySearchPath(module, name);
        } else {
            return null;
        }
    }

    private  List<String> includeExtensions = Arrays.asList(null, "pas", "pp");

    private VirtualFile trySearchPath(Module module, String name) {
        Collection<VirtualFile> virtualFiles = FileBasedIndex.getInstance().getContainingFiles(FileTypeIndex.NAME, PascalFileType.INSTANCE,
                GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module, false));
        for (VirtualFile virtualFile : virtualFiles) {
            String ext = FileUtil.getExtension(name);
            if (ext != null) {
                if (name.equalsIgnoreCase(virtualFile.getName())) {
                    return virtualFile;
                }
            } else if (name.equalsIgnoreCase(virtualFile.getNameWithoutExtension())) {
                if (includeExtensions.contains(virtualFile.getExtension())) {
                    return virtualFile;
                }
            }
        }
        return null;
    }

    private VirtualFile tryExtensions(File file) throws IOException {
        if (!file.isFile() && (FileUtil.getExtension(file.getName()) == null)) {
            String filename = file.getCanonicalPath();
            file = new File(filename + "." + includeExtensions.get(1));
            if (!file.isFile()) {
                file = new File(filename + "." + includeExtensions.get(2));
            }
        }
        return FileUtil.getVirtualFile(file.getCanonicalPath());
    }

    @Override
    public IElementType getElement(IElementType elementType) {
        return elementType;
    }

    private boolean isInactive() {
        return yystate() == INACTIVE_BRANCH;
    }

    private static Pattern patternDefine = Pattern.compile("\\{\\$\\w+\\s+(\\w+)\\}");
    private static String extractDefineName(CharSequence sequence) {
        Matcher m = patternDefine.matcher(sequence);
        return m.matches() ? m.group(1).toUpperCase() : null;
    }

    private static Pattern patternInclude = Pattern.compile("\\{\\$\\w+\\s+([\\w.]+)\\}");
    private static String extractIncludeName(CharSequence sequence) {
        Matcher m = patternInclude.matcher(sequence);
        return m.matches() ? m.group(1) : null;
    }

}