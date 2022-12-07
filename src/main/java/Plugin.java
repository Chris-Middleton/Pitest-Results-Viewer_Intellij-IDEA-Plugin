import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.hints.InlayInfo;
import com.intellij.codeInsight.hints.InlayParameterHintsProvider;
import com.intellij.execution.ExecutionListener;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;


public class Plugin implements StartupActivity, ExecutionListener, LineMarkerProvider, InlayParameterHintsProvider {
    static HashMap<Project, MutationTestState> STATES = new HashMap<>();
    static HashMap<Project, Mutation> DISPLAYED_MUTATIONS = new HashMap<>();
    /* TODO PRIORITY
     * Figure out how to add different icons
     * Replace print output with viewable-in-editor output
     * Testing
     * TODO LATER
     * Change the way I deal with files so the plugin will work in most environments
     * Better Error Handling
     * More Testing
     */

    /**
     * Strikeout the mutated away operator
     * Explore InlayHintProvider to display hint in different color
     * Add to github with liscense.
     *
     * @param project
     */

    @Override
    public void runActivity(@NotNull Project project) {
        updateState(project);
    }

    @Override
    public void processTerminated(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler, int exitCode) {
        if (env.getRunProfile().toString().startsWith("PIT Runner: ")) {
            updateState(env.getProject());
        }
    }

    private void updateState(Project project) {
        try {
            STATES.put(project, new MutationTestState(project));
        } catch (CantFindMutationsException e) {
            e.printStackTrace();
        }
    }


    @Override
    public LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> elements, @NotNull Collection<? super LineMarkerInfo<?>> result) {
        for (PsiElement element : elements) {
            if (element instanceof PsiFile) {
                Project project = element.getProject();
                PsiFile psiFile = element.getContainingFile();
                Document document;
                MutationTestState state;
                String classpath;

                if ((document = PsiDocumentManager.getInstance(project).getDocument(psiFile)) == null)
                    return;

                if ((state = STATES.get(project)) == null)
                    return;
                if ((classpath = Utility.getClassNameOfPsiJavaFile(psiFile)) == null)
                    return;

                for (int line = 0; line < document.getLineCount(); line++) {
                    TextRange range = new TextRange(document.getLineStartOffset(line), document.getLineEndOffset(line));
                    markLine(state.get(classpath, line), range, element, result);
                }
            }
        }
    }


    public void markLine(Mutation[] mutations, TextRange range, PsiElement element, @NotNull Collection<? super LineMarkerInfo<?>> result) {
        for (Mutation mutation : mutations) {
            if (!mutation.detected) {
                result.add(new LineMarkerInfo<>(element, range, AllIcons.Hierarchy.ShouldDefineMethod, (t) -> mutation.toString(), (event, psiElement) -> {
                    String comparison = mutation.compareSources();
                    System.out.println(comparison);
                    mutation.checkOperatorDifference();
                    DISPLAYED_MUTATIONS.put(element.getProject(), mutation);
                }, GutterIconRenderer.Alignment.RIGHT, () -> null));
            }
        }
    }

    @Override
    public @NotNull Set<String> getDefaultBlackList() {
        return Set.of();
    }

    @NotNull
    public List<InlayInfo> getParameterHints(@NotNull PsiElement element) {
        Mutation mutation = DISPLAYED_MUTATIONS.get(element.getProject());
        if (mutation == null || mutation.getSourceOperator() == null)
            return List.of();
        PsiFile psiFile = element.getContainingFile();
        if (psiFile == null)
            return List.of();
        Document document = PsiDocumentManager.getInstance(element.getProject()).getDocument(psiFile);
        if (document == null)
            return List.of();

        String classpath = Utility.getClassNameOfPsiJavaFile(psiFile);
        if (!mutation.group.classpath.equals(classpath))
            return List.of();

        int textOffset = element.getTextOffset();
        int lineNumber = document.getLineNumber(textOffset);
        if (mutation.lineNumber - 1 != lineNumber)
            return List.of();

        if (!element.textMatches(mutation.getSourceOperator()))
            return List.of();

        String line = document.getText(new TextRange(document.getLineStartOffset(lineNumber), document.getLineEndOffset(lineNumber)));
        int offsetInLine = textOffset - document.getLineStartOffset(lineNumber);
        line = line.substring(0, offsetInLine);
        if (mutation.getPriorOccurences() != Utility.countOccurences(line, mutation.getSourceOperator()))
            return List.of();
        return List.of(new InlayInfo(mutation.getMutantOperator(), textOffset, false, false, false, null));
    }

    @NotNull
    public String getInlayPresentation(@NotNull String inlayText) {
        return inlayText;
    }

}

//    @Override
//    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
//        if (element instanceof PsiFile) {
//            Project project = element.getProject();
//            PsiFile psiFile = element.getContainingFile();
//            Document document;
//            MutationTestState state;
//            String classpath;
//
//            if ((document = PsiDocumentManager.getInstance(project).getDocument(psiFile)) == null)
//                return;
//
//            if ((state = STATES.get(project)) == null)
//                return;
//            if((classpath = Utility.getClassNameOfPsiJavaFile(psiFile)) == null)
//                return;
//
//            for (int line = 0; line < document.getLineCount(); line++) {
//                TextRange range = new TextRange(document.getLineStartOffset(line), document.getLineEndOffset(line));
//                annotateLine(state.get(classpath, line), range, holder);
//            }
//        }
//    }

//    public void annotateLine(Mutation[] mutations, TextRange range, AnnotationHolder holder){
//        if(mutations.length == 0)
//            return;
//
//        boolean detected = true;
//        for(Mutation mutation: mutations){
//            detected = detected && mutation.detected;
//        }
//
//        HighlightSeverity severity =
//                detected ? HighlightSeverity.INFORMATION : HighlightSeverity.WARNING;
//        TextAttributesKey textAttributes =
//                CodeInsightColors.WARNINGS_ATTRIBUTES;
//        ProblemHighlightType highlightType =
//                detected ? ProblemHighlightType.INFORMATION : ProblemHighlightType.POSSIBLE_PROBLEM;
//
//        holder.newAnnotation(severity, "")
//                .range(range)
//                .highlightType(highlightType)
//                .textAttributes(textAttributes)
//                .create();
//    }
