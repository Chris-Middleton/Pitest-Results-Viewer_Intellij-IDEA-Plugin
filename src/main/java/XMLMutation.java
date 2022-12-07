import com.intellij.openapi.vfs.VirtualFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class XMLMutation {
    public final int lineNumber, index, block, numberOfTestsRun;
    public final boolean detected;
    public final MutationStatus status;
    public final String sourceFile, mutatedClass, mutatedMethod, methodDescription, mutator, description;
    public final String killingTest;

    public XMLMutation(String line) {
        String[] parts = line.split("><");
        if (parts.length != 16)
            throw new IllegalArgumentException();
        String[] mutationParts = parts[0].split(" ");
        if (mutationParts.length != 4 || !"<mutation".equals(mutationParts[0]))
            throw new IllegalArgumentException();
        String detected = Utility.trimStr(mutationParts[1], "detected='", "'");
        String status = Utility.trimStr(mutationParts[2], "status='", "'");
        String numberOfTestsRun = Utility.trimStr(mutationParts[3], "numberOfTestsRun='", "'");
        if (detected == null || status == null || numberOfTestsRun == null)
            throw new IllegalArgumentException();
        this.detected = "true".equals(detected);
        if (!this.detected && !"false".equals(detected))
            throw new IllegalArgumentException();
        try {
            this.numberOfTestsRun = Integer.parseInt(numberOfTestsRun);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException();
        }
        switch (status) {
            case "KILLED":
                this.status = MutationStatus.Killed;
                break;
            case "SURVIVED":
                this.status = MutationStatus.Survived;
                break;
            case "TIMED_OUT":
                this.status = MutationStatus.TimedOut;
                break;
            case "NO_COVERAGE":
                this.status = MutationStatus.NoCoverage;
                break;
            default:
                throw new IllegalArgumentException();
        }
        if (!"indexes".equals(parts[7]) || !"/indexes".equals(parts[9]) || !"blocks".equals(parts[10]) || !"/blocks".equals(parts[12]) || !"/mutation>".equals(parts[15]))
            throw new IllegalArgumentException();
        this.sourceFile = Utility.trimStr(parts[1], "sourceFile>", "</sourceFile");
        this.mutatedClass = Utility.trimStr(parts[2], "mutatedClass>", "</mutatedClass");
        this.mutatedMethod = Utility.trimStr(parts[3], "mutatedMethod>", "</mutatedMethod");
        this.methodDescription = Utility.trimStr(parts[4], "methodDescription>", "</methodDescription");
        String lineNumber = Utility.trimStr(parts[5], "lineNumber>", "</lineNumber");
        this.mutator = Utility.trimStr(parts[6], "mutator>", "</mutator");
        String index = Utility.trimStr(parts[8], "index>", "</index");
        String block = Utility.trimStr(parts[11], "block>", "</block");
        this.description = Utility.trimStr(parts[14], "description>", "</description");
        if (this.detected) {
            this.killingTest = Utility.trimStr(parts[13], "killingTest>", "</killingTest");
            if (killingTest == null)
                throw new IllegalArgumentException();
        } else if (!"killingTest/".equals(parts[13])) {
            throw new IllegalArgumentException();
        } else {
            this.killingTest = null;
        }
        if (sourceFile == null || mutatedClass == null || mutatedMethod == null || methodDescription == null || lineNumber == null || mutator == null || index == null || block == null || description == null)
            throw new IllegalArgumentException();
        try {
            this.lineNumber = Integer.parseInt(lineNumber);
            this.index = Integer.parseInt(index);
            this.block = Integer.parseInt(block);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException();
        }


    }

    public static HashMap<String, HashMap<Integer, ArrayList<XMLMutation>>> readAll(VirtualFile file) throws IOException {
        Scanner xml = new Scanner(new String(file.contentsToByteArray()));

        HashMap<String, HashMap<Integer, ArrayList<XMLMutation>>> result = new HashMap<>();
        while (xml.hasNextLine()) {
            try {
                XMLMutation mutation = new XMLMutation(xml.nextLine());
                result.computeIfAbsent(mutation.mutatedClass, classpath -> new HashMap<>()).computeIfAbsent(mutation.lineNumber, line -> new ArrayList<>()).add(mutation);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return result;
    }
}
