package cool.furry.mc.projectexpansion.wikiimages;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.Nullable;

final class WikiImageMethods {
    private static final String ANNOTATION = "cool.furry.mc.projectexpansion.wiki.WikiImage";

    private WikiImageMethods() {}

    // the image's path, when the method makes one
    static @Nullable String path(PsiMethod method) {
        PsiAnnotation annotation = method.getAnnotation(ANNOTATION);
        return annotation == null ? null : AnnotationUtil.getStringAttributeValue(annotation, "value");
    }

    // the Gradle project the method's in, the closest folder up with a build script, ex: versions/1.21.1
    static @Nullable String gradleProject(PsiMethod method) {
        PsiFile file = method.getContainingFile();
        VirtualFile dir = file == null || file.getVirtualFile() == null ? null : file.getVirtualFile().getParent();
        for (; dir != null; dir = dir.getParent()) {
            if (dir.findChild("build.gradle") != null || dir.findChild("build.gradle.kts") != null) return dir.getPath();
        }
        return null;
    }
}
