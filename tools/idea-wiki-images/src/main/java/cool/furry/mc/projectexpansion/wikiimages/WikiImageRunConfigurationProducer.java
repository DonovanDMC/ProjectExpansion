package cool.furry.mc.projectexpansion.wikiimages;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType;
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration;

import java.util.List;
import java.util.Objects;

// a Gradle run of the method's project's runWikiImages, limited to just its image
public final class WikiImageRunConfigurationProducer extends LazyRunConfigurationProducer<GradleRunConfiguration> {
    private static final String TASK = "runWikiImages";

    @Override
    public ConfigurationFactory getConfigurationFactory() {
        return GradleExternalTaskConfigurationType.getInstance().getConfigurationFactories()[0];
    }

    @Override
    protected boolean setupConfigurationFromContext(GradleRunConfiguration configuration, ConfigurationContext context, Ref<PsiElement> sourceElement) {
        Target target = target(context);
        if (target == null) return false;
        ExternalSystemTaskExecutionSettings settings = configuration.getSettings();
        settings.setExternalProjectPath(target.project());
        settings.setTaskNames(List.of(TASK));
        settings.setScriptParameters(target.parameters());
        configuration.setName("Wiki Image " + target.path());
        sourceElement.set(target.method());
        return true;
    }

    // so running the same image again reuses its configuration
    @Override
    public boolean isConfigurationFromContext(GradleRunConfiguration configuration, ConfigurationContext context) {
        Target target = target(context);
        if (target == null) return false;
        ExternalSystemTaskExecutionSettings settings = configuration.getSettings();
        return Objects.equals(settings.getExternalProjectPath(), target.project()) && settings.getTaskNames().equals(List.of(TASK)) && Objects.equals(settings.getScriptParameters(), target.parameters());
    }

    private record Target(PsiMethod method, String path, String project) {
        String parameters() {
            return ParametersListUtil.escape("-PwikiOnly=" + path);
        }
    }

    // anywhere in a @WikiImage method
    private static @Nullable Target target(ConfigurationContext context) {
        PsiMethod method = PsiTreeUtil.getParentOfType(context.getPsiLocation(), PsiMethod.class, false);
        if (method == null) return null;
        String path = WikiImageMethods.path(method), project = WikiImageMethods.gradleProject(method);
        return path == null || project == null ? null : new Target(method, path, project);
    }
}
