import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class ExportedMutation {

    public final int index, block, lineNumber;
    public final String mutatedClass, mutatedMethod, methodDescription, mutator, sourceFile, description, testsInOrder;
    public final VirtualFile bytecode;

    private ExportedMutation(String line, VirtualFile bytecode) {
        this.bytecode = bytecode;
        String[] parts = line.trim().split(",");
        if (parts.length != 10)
            throw new IllegalArgumentException();
        this.mutatedClass = Utility.trimStr(parts[0], "MutationDetails [id=MutationIdentifier [location=Location [clazz=", "");
        this.mutatedMethod = Utility.trimStr(parts[1], " method=", "");
        this.methodDescription = Utility.trimStr(parts[2], " methodDesc=", "]");
        String index = Utility.trimStr(parts[3], " indexes=[", "]");
        this.mutator = Utility.trimStr(parts[4], " mutator=", "]");
        this.sourceFile = Utility.trimStr(parts[5], " filename=", "");
        String block = Utility.trimStr(parts[6], " block=[", "]");
        String lineNumber = Utility.trimStr(parts[7], " lineNumber=", "");
        this.description = Utility.trimStr(parts[8], " description=", "");
        this.testsInOrder = Utility.trimStr(parts[9], " testsInOrder=[", "]]");
        if (mutatedClass == null || mutatedMethod == null || methodDescription == null || index == null || mutator == null || sourceFile == null || block == null || lineNumber == null || description == null || testsInOrder == null)
            throw new IllegalArgumentException();
        try {
            this.index = Integer.parseInt(index);
            this.block = Integer.parseInt(block);
            this.lineNumber = Integer.parseInt(lineNumber);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException();
        }
    }

    public static HashMap<Integer, ArrayList<ExportedMutation>> readAll(VirtualFile directory, String classpath) {
        HashMap<Integer, ArrayList<ExportedMutation>> result = new HashMap<>();
        String bytecodeName = classpath + ".class";
        String detailsName = "details.txt";
        directory = directory.findChild("mutants");
        if (directory == null)
            return result;
        VirtualFile[] children = directory.getChildren();
        if (children == null)
            return result;
        for (VirtualFile child : children) {
            VirtualFile bytecode = child.findChild(bytecodeName);
            VirtualFile detailsFile = child.findChild(detailsName);
            if (bytecode == null || detailsFile == null)
                continue;
            try {
                String details = new String(detailsFile.contentsToByteArray());
                ExportedMutation mutation = new ExportedMutation(details, bytecode);
                result.computeIfAbsent(mutation.lineNumber, line -> new ArrayList<>()).add(mutation);
            } catch (IOException | IllegalArgumentException ignored) {
            }
        }
        return result;
    }
}
