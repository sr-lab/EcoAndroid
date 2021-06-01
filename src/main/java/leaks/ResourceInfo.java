package leaks;

import soot.SootClass;
import soot.SootMethod;

import java.util.Objects;

public class ResourceInfo {
    private final String name;
    private final Resource resource;
    private final SootClass declaringClass;
    private final SootMethod declaringMethod;
    private boolean isClassMember;

    public ResourceInfo(String name, Resource resource, SootClass declaringClass, SootMethod declaringMethod) {
        this.name = name;
        this.resource = resource;
        this.declaringClass = declaringClass;
        this.declaringMethod = declaringMethod;
        this.isClassMember = false;
    }

    public ResourceInfo(String name, Resource resource, SootClass declaringClass, SootMethod declaringMethod, boolean isClassMember) {
        this.name = name;
        this.resource = resource;
        this.declaringClass = declaringClass;
        this.declaringMethod = declaringMethod;
        this.isClassMember = isClassMember;
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

    public boolean isClassMember() {
        return isClassMember;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceInfo resourceInfo = (ResourceInfo) o;
        return name.equals(resourceInfo.name) && resource == resourceInfo.resource
                && declaringClass.equals(resourceInfo.declaringClass) && declaringMethod.equals(resourceInfo.declaringMethod);
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
