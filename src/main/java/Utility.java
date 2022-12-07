import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;

import java.io.File;

public class Utility {
    protected static String trimStr(String str, String start, String end) {
        if (str.startsWith(start) && str.endsWith(end)) {
            return str.substring(start.length(), str.length() - end.length());
        }
        return null;
    }

    protected static String getClassNameOfPsiJavaFile(PsiFile psiFile) {
        String javaFileName = trimStr(psiFile.getName(), "", ".java");
        if (javaFileName == null)
            return null;
        PsiDirectory directory = psiFile.getParent();
        if (directory == null)
            return null;
        String packageName = ProjectRootManager.getInstance(psiFile.getProject()).getFileIndex().getPackageNameByDirectory(directory.getVirtualFile());
        if (packageName == null)
            return null;
        if (packageName.equals(""))
            return javaFileName;
        return packageName + "." + javaFileName;
    }

    protected static VirtualFile decompile(VirtualFile bytecode, VirtualFile intoDirectory) {
        String classname = trimStr(bytecode.getName(), "", ".class");
        if (classname == null)
            throw new UnableToDecompileException();
        ProcessBuilder pb = new ProcessBuilder();
        pb.directory(new File("C:\\Program Files\\JetBrains\\IntelliJ IDEA Community Edition 2021.2.3\\plugins\\java-decompiler\\lib\\"));
        pb.command("java", "-cp", "java-decompiler.jar", "org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler", bytecode.getPath(), intoDirectory.getPath());

        try {
            Process p = pb.start();
            p.waitFor();
        } catch (Exception e) {
            throw new UnableToDecompileException();
        }
        intoDirectory.refresh(false, false);
        VirtualFile result = intoDirectory.findChild(classname + ".java");
        if (result == null)
            throw new UnableToDecompileException();
        return result;
    }

    public static int countOccurences(String string, String within) {

        int occurences = 0;
        for (int i = 0; i < string.length(); i++)
            if (within.equals(string.substring(i, i + within.length())))
                occurences += 1;
        return occurences;
    }

}
