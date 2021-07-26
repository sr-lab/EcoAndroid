package leaks.results;

import leaks.Resource;
import soot.SootMethod;

import java.util.Objects;

public class Leak {
    private final Resource resource;
    private final SootMethod leakedMethod;
    private final SootMethod declaredMethod;
    private final int lineNumber;

    public Leak(Resource resource, SootMethod leakedMethod, SootMethod declaredMethod, int lineNumber) {
        this.resource = resource;
        this.leakedMethod = leakedMethod;
        this.declaredMethod = declaredMethod;
        this.lineNumber = lineNumber;
    }

    public SootMethod getLeakedMethod() {
        return leakedMethod;
    }

    public SootMethod getDeclaredMethod() {
        return declaredMethod;
    }

    public String getDeclaredMethodName() {
        return declaredMethod.getName();
    }

    public String getLeakedMethodName() {
        return leakedMethod.getName();
    }

    public String getDeclaredClassName() {
        return declaredMethod.getDeclaringClass().getName();
    }

    public String getLeakedClassName() {
        return leakedMethod.getDeclaringClass().getName();
    }

    public Resource getResource() {
        return resource;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Leak leak = (Leak) o;
        return lineNumber == leak.lineNumber && resource == leak.resource && leakedMethod.equals(leak.leakedMethod) && declaredMethod.equals(leak.declaredMethod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resource, leakedMethod, declaredMethod, lineNumber);
    }
}