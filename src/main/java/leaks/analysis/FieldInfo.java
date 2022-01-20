package leaks.analysis;

import leaks.Resource;

import java.util.Objects;

public class FieldInfo {
    private String name;
    private String declaringClass;
    private String type;
    private Resource resource;

    public FieldInfo(String name, String declaringClass, String type, Resource resource) {
        this.name = name;
        this.declaringClass = declaringClass;
        this.type = type;
        this.resource = resource;
    }

    public String getName() {
        return name;
    }

    public String getDeclaringClass() {
        return declaringClass;
    }

    public String getType() {
        return type;
    }

    public Resource getResource() { return resource; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldInfo fieldInfo = (FieldInfo) o;
        return name.equals(fieldInfo.name) && declaringClass.equals(fieldInfo.declaringClass) && type.equals(fieldInfo.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, declaringClass, type);
    }
}
