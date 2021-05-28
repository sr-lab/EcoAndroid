package leaks;

import soot.SootClass;
import soot.SootMethod;

import java.util.Objects;

public class FactId {
    private String name;
    private Resource resource;
    private SootClass declaringClass;
    private SootMethod declaringMethod;

    public FactId(String name, Resource resource, SootClass declaringClass, SootMethod declaringMethod) {
        this.name = name;
        this.resource = resource;
        this.declaringClass = declaringClass;
        this.declaringMethod = declaringMethod;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public void setDeclaringClass(SootClass declaringClass) {
        this.declaringClass = declaringClass;
    }

    public void setDeclaringMethod(SootMethod declaringMethod) {
        this.declaringMethod = declaringMethod;
    }

    public String getName() { return name;}

    public Resource getResource() {
        return resource;
    }

    public SootClass getDeclaringClass() {
        return declaringClass;
    }

    public SootMethod getDeclaringMethod() {
        return declaringMethod;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FactId factId = (FactId) o;
        return name.equals(factId.name) && resource == factId.resource
                && declaringClass.equals(factId.declaringClass) && declaringMethod.equals(factId.declaringMethod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, resource, declaringClass, declaringMethod);
    }

    @Override
    public String toString() {
        return "[" + name + "," + resource + "," + declaringMethod + "]";
    }
}
