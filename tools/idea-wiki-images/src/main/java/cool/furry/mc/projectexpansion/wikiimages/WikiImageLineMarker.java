package cool.furry.mc.projectexpansion.wikiimages;

import com.intellij.execution.lineMarker.ExecutorAction;
import com.intellij.execution.lineMarker.RunLineMarkerContributor;
import com.intellij.icons.AllIcons;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.Nullable;

// a run button beside every @WikiImage method's name, like a test's
public final class WikiImageLineMarker extends RunLineMarkerContributor {
    @Override
    public @Nullable Info getInfo(PsiElement element) {
        if (!(element instanceof PsiIdentifier) || !(element.getParent() instanceof PsiMethod method) || method.getNameIdentifier() != element) return null;
        String path = WikiImageMethods.path(method);
        if (path == null) return null;
        // run & debug, made by WikiImageRunConfigurationProducer
        return new Info(AllIcons.RunConfigurations.TestState.Run, ExecutorAction.getActions(0), e -> "Save " + path);
    }
}
