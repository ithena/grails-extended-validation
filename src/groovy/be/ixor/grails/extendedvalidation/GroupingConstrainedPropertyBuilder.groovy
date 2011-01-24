package be.ixor.grails.extendedvalidation

import grails.util.GrailsUtil
import org.codehaus.groovy.grails.commons.ClassPropertyFetcher
import org.codehaus.groovy.grails.validation.ConstrainedProperty

class GroupingConstrainedPropertyBuilder extends BuilderSupport {

    Map<String, ConstrainedProperty> constrainedProperties = [:]
    Map<String, Map<String, ConstrainedProperty>> constrainedPropertiesGroups = new HashMap<String, Map<String, ConstrainedProperty>>();
    Map<String, InstanceConstraint> instanceConstraints = [:]

    private Map<String, String> sharedConstraints = new HashMap<String, String>();
    private int order = 1;
    private Class targetClass;
    private ClassPropertyFetcher classPropertyFetcher;
    private static final String SHARED_CONSTRAINT = "shared";

    public GroupingConstrainedPropertyBuilder(Object target) {
        this(target.getClass());
    }

    public GroupingConstrainedPropertyBuilder(Class targetClass) {
        this.targetClass = targetClass;
        this.classPropertyFetcher = ClassPropertyFetcher.forClass(targetClass);
    }

    public Map getConstraints() {
        return constrainedProperties + instanceConstraints
    }

    public String getSharedConstraint(String propertyName) {
        return sharedConstraints.get(propertyName);
    }

    protected Object createNode(Object name, Map attributes) {
        String nodeName = (String) name;
        if (classPropertyFetcher.isReadableProperty(nodeName)) {
            return createConstrainedProperty(nodeName, attributes);
        } else if (attributes && attributes['validator']) {
            return createInstanceConstraint(nodeName, attributes['validator']);
        } else {
            return createGroupConstraint(nodeName);
        }
    }

    private Object createInstanceConstraint(String constraintName, Closure validator) {
        if (instanceConstraints[constraintName]) return instanceConstraints[constraintName]

        InstanceConstraint constraint = InstanceConstraintFactory.create()
        instanceConstraints[constraintName] = constraint
        constraint.constraintName = constraintName
        constraint.validator = validator
        constraint.owningClass = targetClass

        return constraint
    }

    private Object createConstrainedProperty(String propertyName, Map attributes) {
        ConstrainedProperty cp;
        if (constrainedProperties.containsKey(propertyName)) {
            cp = constrainedProperties.get(propertyName);
        } else {
            cp = new ConstrainedProperty(this.targetClass, propertyName, classPropertyFetcher.getPropertyType(propertyName));
            cp.setOrder(order++);
            constrainedProperties.put(propertyName, cp);
        }
        for (Object o: attributes.keySet()) {
            String constraintName = (String) o;
            final Object value = attributes.get(constraintName);
            if (SHARED_CONSTRAINT.equals(constraintName)) {
                if (value != null)
                    sharedConstraints.put(propertyName, value.toString());
                continue;
            }
            if (cp.supportsContraint(constraintName)) {
                cp.applyConstraint(constraintName, value);
            } else {
                if (ConstrainedProperty.hasRegisteredConstraint(constraintName)) {
                    // constraint is registered but doesn't support this property's type
                    GrailsUtil.warn("Property [" + cp.getPropertyName() + "] of domain class " + targetClass.getName() + " has type [" + cp.getPropertyType().getName() + "] and doesn't support constraint [" + constraintName + "]. This constraint will not be checked during validation.");
                } else {
                    // in the case where the constraint is not supported we still retain meta data
                    // about the constraint in case its needed for other things
                    cp.addMetaConstraint(constraintName, value);
                }
            }
        }
        return cp;
    }

    private Object createGroupConstraint(String groupName) {
        if (!constrainedPropertiesGroups.containsKey(groupName)) {
            constrainedPropertiesGroups.put(groupName, new HashMap<String, ConstrainedProperty>());
        }
        return constrainedPropertiesGroups.get(groupName);
    }

    protected void setParent(Object parent, Object child) {
        setParent((Map) parent, child)
    }

    protected void setParent(Map parent, ConstrainedProperty child) {
        parent[child.getPropertyName()] = child;
    }

    protected void setParent(Map parent, InstanceConstraint child) {
        parent[child.name] = child
    }

    protected Object createNode(Object name) {
        return createNode(name, Collections.EMPTY_MAP);
    }

    protected Object createNode(Object name, Object value) {
        return createNode(name, Collections.EMPTY_MAP, value);
    }

    protected Object createNode(Object name, Map attributes, Object value) {
        throw new MissingMethodException((String) name, targetClass, [attributes, value] as Object[]);
    }

}
