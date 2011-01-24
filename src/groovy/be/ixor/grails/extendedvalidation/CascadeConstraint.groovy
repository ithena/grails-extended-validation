package be.ixor.grails.extendedvalidation

import org.codehaus.groovy.grails.validation.AbstractConstraint
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.springframework.validation.Errors

public class CascadeConstraint extends AbstractConstraint {
    static final String NAME = "cascade"
    static final String DEFAULT_MESSAGE_CODE = "default.cascade.message"

    boolean cascade

    @Override
    void setParameter(Object constraintParameter) {
        if (!(constraintParameter instanceof Boolean)) {
            throw new IllegalArgumentException("Parameter for constraint [" +
                    NAME + "] of property [" +
                    constraintPropertyName + "] of class [" + constraintOwningClass +
                    "] must be a boolean value");
        }

        cascade = ((Boolean) constraintParameter).booleanValue();
        super.setParameter(constraintParameter);
    }

    @Override
    protected void processValidate(Object target, Object propertyValue, Errors errors) {
        if (!cascade || propertyValue == null) {
            return
        }
        def nestedValues = (propertyValue instanceof Map) ? propertyValue.values() : propertyValue
        def propertiesToCheck = processConstraintsToCheck(errors.propertiesToCheck)
        def excludedChecks = processConstraintsToCheck(errors.excludedChecks)
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
