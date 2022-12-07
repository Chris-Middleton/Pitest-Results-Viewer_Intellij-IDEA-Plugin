import com.intellij.openapi.vfs.VirtualFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MutationGroup {
    public final String classpath;
    private final HashMap<Integer, Mutation[]> map = new HashMap<>();
    private final VirtualFile bytecode, exports;
    private VirtualFile sourcecode;

    public MutationGroup(String classpath, VirtualFile bytecode, VirtualFile exports, HashMap<Integer, ArrayList<ExportedMutation>> allExportedMutations, HashMap<Integer, ArrayList<XMLMutation>> allXmlMutations) throws CantFindMutationsException {
        this.classpath = classpath;
        this.bytecode = bytecode;
        this.exports = exports;

        for (Map.Entry<Integer, ArrayList<XMLMutation>> entry : allXmlMutations.entrySet()) {
            int line = entry.getKey();
            ArrayList<XMLMutation> xmlMutations = entry.getValue();
            ArrayList<ExportedMutation> exportedMutations = allExportedMutations.get(line);
            if (exportedMutations == null)
                throw new CantFindMutationsException();
            int i = 0;
            Mutation[] mutations = new Mutation[xmlMutations.size()];
            for (XMLMutation xmlMutation : xmlMutations) {
                Mutation mutation = null;
                for (ExportedMutation exportedMutation : exportedMutations) {
                    try {
                        mutation = new Mutation(xmlMutation, exportedMutation, this);
                        break;
                    } catch (Exception ignored) {
                    }
                }
                if (mutation == null)
                    throw new CantFindMutationsException();
                mutations[i] = mutation;
                i += 1;
            }
            map.put(line, mutations);
        }
    }

    public void debug() {
        for (Map.Entry<Integer, Mutation[]> entry : this.map.entrySet()) {
            System.out.println("  Mutations on line #" + entry.getKey() + ":");
            for (Mutation mutation : entry.getValue()) {
                System.out.println("    " + mutation.description);
            }
        }
    }

    //Assumes 0-based indexing for line numbers
    public Mutation[] get(int line) {
        return map.getOrDefault(line + 1, new Mutation[0]);
    }

    public VirtualFile getSourcecode() {
        if (sourcecode == null)
            sourcecode = Utility.decompile(bytecode, exports);
        return sourcecode;
    }
}
