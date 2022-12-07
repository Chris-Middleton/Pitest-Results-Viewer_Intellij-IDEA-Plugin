import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MutationTestState {
    private final HashMap<String, MutationGroup> mutations = new HashMap<>();
    private final Project project;

    public MutationTestState(Project project) throws CantFindMutationsException {
        this.project = project;
        VirtualFile export = getExportDirectory();
        VirtualFile build = getBuildDirectory();
        VirtualFile report = findMostRecentReport();
        if (report == null || export == null || build == null)
            throw new CantFindMutationsException();
        HashMap<String, HashMap<Integer, ArrayList<XMLMutation>>> allXmlMutations;
        try {
            allXmlMutations = XMLMutation.readAll(report);
        } catch (IOException e) {
            throw new CantFindMutationsException();
        }

        for (Map.Entry<String, HashMap<Integer, ArrayList<XMLMutation>>> entry : allXmlMutations.entrySet()) {
            String classpath = entry.getKey();
            String classpathPath = "./" + classpath.replace('.', '/');
            VirtualFile exports = export.findFileByRelativePath(classpathPath);
            VirtualFile source = build.findFileByRelativePath(classpathPath + ".class");
            if (exports == null || source == null)
                throw new CantFindMutationsException();
            mutations.put(classpath, new MutationGroup(classpath, source, exports, ExportedMutation.readAll(exports, classpath), entry.getValue()));
        }
    }

    private void debug() {
        System.out.println("--Begin MutationTestState--");
        for (Map.Entry<String, MutationGroup> entry : this.mutations.entrySet()) {
            System.out.println(entry.getKey());
            entry.getValue().debug();
        }
        System.out.println("--End MutationTestState--");
    }

    private VirtualFile getReportDirectory() {
        VirtualFile report = project.getProjectFile();
        if (report == null)
            return null;
        return report.findFileByRelativePath("../../report");
    }

    private VirtualFile getBuildDirectory() {
        VirtualFile report = project.getProjectFile();
        if (report == null)
            return null;
        return report.findFileByRelativePath("../../out/production/" + project.getName());
    }

    private VirtualFile getExportDirectory() {
        VirtualFile report = getReportDirectory();
        if (report == null)
            return null;
        return report.findFileByRelativePath("./export");
    }

    private VirtualFile findMostRecentReport() {
        VirtualFile report = getReportDirectory();
        if (report == null)
            return null;
        VirtualFile[] files = report.getChildren();
        if (files == null)
            return null;
        long biggest = 0;
        for (VirtualFile file : files) {
            try {
                long num = Long.parseUnsignedLong(file.getName());
                if (num > biggest)
                    biggest = num;
            } catch (NumberFormatException ignored) {
            }
        }
        report = report.findChild(Long.toString(biggest));
        if (report == null)
            return null;
        return report.findChild("mutations.xml");
    }

    //Assumes 0-based indexing for line numbers
    public Mutation[] get(String classpath, int line) {
        MutationGroup group = mutations.get(classpath);
        if (group == null)
            return new Mutation[0];
        return group.get(line);
    }

}
