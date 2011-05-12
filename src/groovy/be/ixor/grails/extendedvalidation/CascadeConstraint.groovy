package be.ixor.grails.extendedvalidation

import org.codehaus.groovy.grails.validation.AbstractConstraint
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.springframework.validation.Errors

public class CascadeConstraint extends AbstractConstraint {
    static final String NAME = "cascade"
    static final String DEFAULT_MESSAGE_CODE = "default.cascade.message"
    static final String MESSAGE_CODE = NAME + ConstrainedProperty.INVALID_SUFFIX

    boolean cascade
    List<String> excludes = []

    @Override
    void setParameter(Object constraintParameter) {
        if(constraintParameter == null){
            throw new IllegalArgumentException(
                    "Parameter for constraint [${NAME}] of property [${constraintPropertyName}] of class [${constraintOwningClass}] must not be NULL.")
        }

        initConstraintParameter(constraintParameter)

        super.setParameter(constraintParameter);
    }

    void initConstraintParameter(Boolean constraintParameter){
        cascade = constraintParameter
    }

    void initConstraintParameter(Map constraintParameter){
        if(!constraintParameter){
            throw new IllegalArgumentException(
                    "Parameter for constraint [${NAME}] of property [${constraintPropertyName}] of class [${constraintOwningClass}] ")
        }

        setExcludes(constraintParameter.excludes ?: [])
        cascade = true
    }

    void initConstraintParameter(constraintParameter){
        throw new IllegalArgumentException(
                    "Parameter for constraint [${NAME}] of property [${constraintPropertyName}] of class [${constraintOwningClass}] " +
                    "should be of type boolean or type Map but is ${constraintParameter?.class?.name}.")
    }

    void setExcludes(String exclude){
        setExcludes([exclude])
    }

    void setExcludes(List excludes){
        this.excludes = excludes
    }

    void setExcludes(excludes){
        throw new IllegalArgumentException(
                    "Excludes definition for constraint [${NAME}] of property [${constraintPropertyName}] of class [${constraintOwningClass}] " +
                    "should be a String or a List of Strings but is ${excludes?.class?.name}.")
    }

    @Override
    protected void processValidate(Object target, Object propertyValue, Errors errors) {
        if (!cascade || propertyValue == null) {
            return
        }
        def nestedValues = (propertyValue instanceof Map) ? propertyValue.values() : propertyValue
        def propertiesToCheck = processConstraintsToCheck(errors.propertiesToCheck)
        def excludedChecks = processConstraintsToCheck(errors.excludedChecks) + excludes
        boolean valid = true
        for (value in nestedValues) {
            if (value != null) {
                valid &= ValidationHelper.validateInstance(errors.groupsToCheck, propertiesToCheck, excludedChecks, value, messageSource)
            }
        }
        if (!valid) {
            rejectValue(target, errors, DEFAULT_MESSAGE_CODE, NAME + ConstrainedProperty.INVALID_SUFFIX, [] as Object[])
        }
    }

    private List processConstraintsToCheck(List constraintsToCheck) {
        def result = []
        def prefix = propertyName + '.'
        for (constraint in constraintsToCheck) {
            if (constraint.startsWith(prefix)) {
                result << constraint.substring(prefix.size())
            }
        }
        return result
    }

    @Override
    boolean supports(Class type) {
        return !type.isPrimitive()
    }

    @Override
    String getName() {
        return NAME
    }
}
