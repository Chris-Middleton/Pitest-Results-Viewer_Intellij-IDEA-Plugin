import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

public class Mutation {
    public final int lineNumber, index, block, numberOfTestsRun;
    public final boolean detected;
    public final MutationStatus status;
    public final String killingTest;
    public final String sourceFile, mutatedClass, mutatedMethod, methodDescription, mutator, description, testsInOrder;
    public final MutationGroup group;
    public final VirtualFile bytecode;

    private VirtualFile sourcecode = null;
    private String sourceOperator = null;
    private String mutantOperator = null;
    private int priorOccurences;

    public Mutation(XMLMutation xml, ExportedMutation exp, MutationGroup group) throws Exception {
        if (xml.lineNumber == exp.lineNumber && xml.index == exp.index && xml.block == exp.block && xml.sourceFile.equals(exp.sourceFile) && xml.mutatedClass.equals(exp.mutatedClass) && xml.mutatedMethod.equals(exp.mutatedMethod) && xml.methodDescription.equals(exp.methodDescription) && xml.mutator.equals(exp.mutator)) {
            this.lineNumber = xml.lineNumber;
            this.index = xml.index;
            this.block = xml.block;
            this.numberOfTestsRun = xml.numberOfTestsRun;
            this.detected = xml.detected;
            this.status = xml.status;
            this.killingTest = xml.killingTest;
            this.sourceFile = xml.sourceFile;
            this.mutatedClass = xml.mutatedClass;
            this.mutatedMethod = xml.mutatedMethod;
            this.methodDescription = xml.methodDescription;
            this.mutator = xml.mutator;
            this.description = exp.description;
            this.testsInOrder = exp.testsInOrder;
            this.bytecode = exp.bytecode;
            this.group = group;
        } else {
            throw new Exception();
        }
    }

    public String toString() {
        return String.format("%s mutant - %s", status, description);
    }


    private VirtualFile getSourcecode() {
        if (sourcecode == null)
            sourcecode = Utility.decompile(bytecode, bytecode.getParent());
        return sourcecode;
    }

    public String compareSources() {
        Scanner mutant, source;
        try {
            mutant = new Scanner(new String(getSourcecode().contentsToByteArray()));
            source = new Scanner(new String(group.getSourcecode().contentsToByteArray()));
        } catch (IOException e) {
            throw new RuntimeException();
        }
        ArrayList<String> mutantLines = new ArrayList<>();
        ArrayList<String> sourceLines = new ArrayList<>();
        while (mutant.hasNextLine())
            mutantLines.add(mutant.nextLine());
        while (source.hasNextLine())
            sourceLines.add(source.nextLine());

        int m = mutantLines.size() - 1;
        int s = sourceLines.size() - 1;
        int maxI = Math.min(m, s);
        int d = 0;
        while (d < maxI && mutantLines.get(d).equals(sourceLines.get(d))) {
            d += 1;
        }
        while (m > 0 && s > 0 && mutantLines.get(m).equals(sourceLines.get(s))) {
            m -= 1;
            s -= 1;
        }
        StringBuilder comparison = new StringBuilder();
        comparison.append(String.format("Mutation on line #%d of %s\n", lineNumber, sourceFile));
        comparison.append(String.format("Status = %s, detected = %s\n", status, detected ? "true" : "false"));
        comparison.append(String.format("Mutated Method: %s\n", mutatedMethod));
        comparison.append(String.format("Description: %s\n", description));
        comparison.append("-------- Source Code --------\n...\n");
        for (int i = d; i <= s; i++) {
            comparison.append(sourceLines.get(i));
            comparison.append('\n');
        }
        comparison.append("...\n-------- Mutant Code --------\n...\n");
        for (int i = d; i <= m; i++) {
            comparison.append(mutantLines.get(i));
            comparison.append('\n');
        }
        comparison.append("...");
        return comparison.toString();

    }

    public void checkOperatorDifference() {
        Scanner mutant, source;
        try {
            mutant = new Scanner(new String(getSourcecode().contentsToByteArray()));
            source = new Scanner(new String(group.getSourcecode().contentsToByteArray()));
        } catch (IOException e) {
            throw new RuntimeException();
        }
        String mutantLine = "";
        String sourceLine = "";
        while (mutantLine.equals(sourceLine) && mutant.hasNextLine() && source.hasNextLine()) {
            mutantLine = mutant.nextLine();
            sourceLine = source.nextLine();
        }
        if (mutantLine.equals(sourceLine))
            return;
        while (mutant.hasNextLine() && source.hasNextLine())
            if (!mutant.nextLine().equals(source.nextLine()))
                return;
        if (mutant.hasNextLine() || source.hasNextLine())
            return;

        diffLines(mutantLine, sourceLine);

    }

    private void diffLines(String m, String s) {

        int minLen = Math.min(m.length(), s.length());
        int first = 0;
        int last = 1;
        while (first < minLen && m.charAt(first) == s.charAt(first))
            first += 1;

        while (last < minLen && m.charAt(m.length() - last) == s.charAt(s.length() - last))
            last += 1;
        last -= 1;
        if (first == last)
            return;

        mutantOperator = m.substring(first, m.length() - last);
        sourceOperator = s.substring(first, s.length() - last);

        this.priorOccurences = Utility.countOccurences(s.substring(0, first), sourceOperator);

    }

    public int getPriorOccurences() {
        return this.priorOccurences;
    }

    public String getSourceOperator() {
        return this.sourceOperator;
    }

    public String getMutantOperator() {
        return this.mutantOperator;
    }
}
