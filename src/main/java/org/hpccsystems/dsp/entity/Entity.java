package org.hpccsystems.dsp.entity;

import java.io.Serializable;
import java.util.List;

public class Entity implements Serializable{
   
    private static final long serialVersionUID = 1L;

    private String name;
    private String value;
    private List<Entity> children;

    // TODO: remove this constructor, after implementing child dataset for file
    // import
    public Entity(String value) {
        this.value = value;
    }

    public Entity(String name, List<Entity> children) {
        this.name = name;
        this.children = children;
    }

    public Entity(List<Entity> children) {
        this.children = children;
    }

    public Entity(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public List<Entity> getChildren() {
        return children;
    }

    public boolean hasChild() {
        return children != null && !children.isEmpty();
    }

    @Override
    public String toString() {
        return name + ":" + value + "[" + children + "]";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}