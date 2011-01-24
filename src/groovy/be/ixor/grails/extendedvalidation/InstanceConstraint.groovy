package be.ixor.grails.extendedvalidation

import org.springframework.context.MessageSource
import org.springframework.validation.BindingResult
import org.springframework.validation.Errors
import org.springframework.validation.ObjectError

class InstanceConstraint {

    protected String constraintName;
    protected Class<?> owningClass;

    MessageSource messageSource;

    protected Closure validator
    protected Integer numValidatorParams

    /**
     * Returns whether the constraint supports being applied against the specified type;
     *
     * @param type The type to support
     * @return True if the constraint can be applied against the specified type
     */
    boolean supports(Class type) {
        return type == owningClass
    }

    void validate(Object target, Errors errors) {
        if (!validator) return

        Object[] validatorParams = new Object[numValidatorParams]
        validatorParams[0] = target
        if (numValidatorParams == 2) {
            validatorParams[1] = errors
        }

        validator.delegate = this
        final Object validateResult = validator.call(validatorParams)

        if (!validatorHandlesErrors()) {
            handleErrors(validateResult, target, errors)
        }
    }

    private Boolean validatorHandlesErrors() {
        numValidatorParams == 2
    }

    private void handleErrors(result, target, Errors errors) {
        boolean bad = false;
        String errorMessage = null;
        Object[] args = [];

        if (result != null) {
            (bad, errorMessage, args) = interpretValidateResult(result)
        }
        if (bad) {
            if (!args) {
                args = [owningClass] as Object[]
            }
            rejectInstance(target, errors, errorMessage, args);
        }
    }

    private def interpretValidateResult(Boolean result) {
        [!result]
    }

    private def interpretValidateResult(CharSequence result) {
        [true, result.toString()]
    }

    private def interpretValidateResult(Collection result) {
        interpretValidateResult(result as Object[])
    }

    private def interpretValidateResult(Object[] result) {
        Object[] values = result as Object[]
        if (!(values[0] instanceof String)) {
            throw new IllegalArgumentException("Return value from validation closure of property [${constraintName}] of class [${owningClass}]"
                    + " is returning a list but the first element must be a string containing the error message code");
        }
        String errorMessage = (String) values[0];
        Object[] args = new Object[values.length - 1 + 1];
        int i = 0;
        args[i++] = owningClass;
        System.arraycopy(values, 1, args, i, values.length - 1);

        [true, errorMessage, args]
    }

    private void rejectInstance(target, errors, errorMessage, args) {
        BindingResult result = (BindingResult) errors

        Class owningClass = args[0]

        def newCodes = [] as Set
        newCodes.addAll(Arrays.asList(result.resolveMessageCodes("${owningClass.name}.${getName()}", errorMessage)));

        ObjectError error = new ObjectError(
                errors.objectName,
                newCodes as String[],
                args as Object[],
                "defaultMessage")

        errors.addError(error)
    }

    void setValidator(Closure validator) {
        if (!validator) return

        Class<?>[] params = validator.getParameterTypes();

        if (params.length == 0) {
            throw new IllegalArgumentException("Parameter for constraint [${constraintName}] of class [${owningClass}] must be a Closure taking at least 1 parameter (value, [object])");
        }

        if (params.length > 2) {
            throw new IllegalArgumentException("Parameter for constraint [${constraintName}] of class [${owningClass}] must be a Closure taking no more than 2 parameters (value, [object, [errors]])");
        }

        this.validator = validator
        numValidatorParams = params.length
    }

    void setParameter(Object constraintParameter) {
        if (!(constraintParameter instanceof Closure)) {
            throw new IllegalArgumentException("Parameter for instance constraint [${constraintName}] of class [${owningClass}] must be a Closure")
        }

        setValidator(constraintParameter)
    }

    String getName() {
        constraintName
    }


    def addInstanceError(Errors errors, Object target, String errorMessage, Object[] args) {
        BindingResult result = (BindingResult) errors

        Class owningClass = target?.class

        Set newCodes = [] as Set
        newCodes.addAll(Arrays.asList(result.resolveMessageCodes("${owningClass.name}", errorMessage)));

        ObjectError error = new ObjectError(
                errors.objectName,
                newCodes as String[],
                args as Object[],
                !newCodes?.isEmpty() ? newCodes.iterator().next() : "default error message for ${owningClass?.name}")

        result.addError(error)
    }
}
