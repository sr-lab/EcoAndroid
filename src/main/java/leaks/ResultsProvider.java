package leaks;

import com.intellij.psi.PsiMethod;

import java.util.HashSet;
import java.util.Set;

public class ResultsProvider {

    private Set<ResourceLeak> results = new HashSet<>();

    public void addResult(ResourceLeak leak) {
        results.add(leak);
    }

    public void clearResults() {
        results.clear();
    }

    public boolean hasResourceLeaked(PsiMethod method) {
        for (ResourceLeak leak : results) {
            if (leak.getPsiMethod().isEquivalentTo(method)) {
                return true;
            }
        }
        return false;
    }

    public String toCSV() {
        StringBuilder stringResults = new StringBuilder();
        stringResults.append("methodName").append(",className\n");
        for (ResourceLeak leak : results) {
            String methodName = leak.getMethodName();
            String className = leak.getClassName();
            stringResults.append(methodName).append(",").append(className).append("\n");
        }

        return stringResults.toString();
    }
}
