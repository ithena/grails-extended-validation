package be.ixor.grails.extendedvalidation

import org.springframework.web.context.request.RequestContextHolder as SpringRCH

import java.lang.reflect.Method
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.support.SoftThreadLocalMap
import org.codehaus.groovy.grails.validation.ConstrainedProperty
import org.springframework.beans.BeanUtils
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError

class ValidationHelper {
    private static final PROPERTY_INSTANCE_MAP = new SoftThreadLocalMap()
    private static Method webflowRCHGetter

    static {
        try {
            Class webflowRCH = Class.forName('org.springframework.webflow.execution.RequestContextHolder')
            webflowRCHGetter = BeanUtils.findDeclaredMethod(webflowRCH, 'getRequestContext', null)
        } catch (ClassNotFoundException) {
            webflowRCHGetter = null
        }
    }


    static addValidationMethods(validateableClass, messageSource) {
        ConstrainedProperty.registerNewConstraint(CascadeConstraint.NAME, CascadeConstraint)

        def metaClass = validateableClass.metaClass
        metaClass.hasErrors = {->
            delegate.errors?.hasErrors()
        }

        def get
        def put
        try {
            get = {
                def requestContext = webflowRCHGetter?.invoke(null)
                if (requestContext?.flashScope) {
                    return requestContext.flashScope.get(it)
                } else {
                    def attributes = SpringRCH.getRequestAttributes()
                    if (attributes) {
                        return attributes.request.getAttribute(it)
                    }
                    else {
                        return PROPERTY_INSTANCE_MAP.get().get(it)
                    }
                }
            }
            put = {key, val ->
                def requestContext = webflowRCHGetter?.invoke(null)
                if (requestContext?.flashScope) {
                    requestContext.flashScope.put(key.toString(), val)
                } else {
                    def attributes = SpringRCH.getRequestAttributes()
                    if (attributes) {
                        attributes.request.setAttribute(key, val)
                    }
                    else {
                        PROPERTY_INSTANCE_MAP.get().put(key, val)
                    }
                }
            }
        }
        catch (Throwable e) {
            get = { PROPERTY_INSTANCE_MAP.get().get(it) }
            put = {key, val -> PROPERTY_INSTANCE_MAP.get().put(key, val) }
        }

        metaClass.getErrors = {->
            def errors
            def key = createKey(delegate)
            errors = get(key)
            if (!errors) {
                errors = new BeanPropertyBindingResult(delegate, delegate.getClass().getName())
                put key, errors
            }
            errors
        }
        metaClass.setErrors = {Errors errors ->
            def key = createKey(delegate)
            put key, errors
        }
        metaClass.clearErrors = {->
            delegate.setErrors(new BeanPropertyBindingResult(delegate, delegate.getClass().getName()))
        }

        def validationClosure = GrailsClassUtils.getStaticPropertyValue(validateableClass, 'constraints')
        def validateable = validateableClass.newInstance()
        if (validationClosure) {
            def groupingConstrainedPropertyBuilder = new GroupingConstrainedPropertyBuilder(validateable)
            validationClosure.setDelegate(groupingConstrainedPropertyBuilder)
            validationClosure()
            metaClass.constraints = groupingConstrainedPropertyBuilder.constraints
            def constraintGroups = groupingConstrainedPropertyBuilder.constrainedPropertiesGroups
            metaClass.getConstraintGroups = {-> constraintGroups}
        }
        else {
            metaClass.constraints = [:]
        }

        if (!metaClass.respondsTo(validateable, "validate")) {
            metaClass.validate = {
                ValidationHelper.validateInstance(null, null, null, delegate, messageSource)
            }
            metaClass.validate = {Map args ->
                ValidationHelper.validateInstance(args.groups, args.includes, args.excludes, delegate, messageSource)
            }
        }
        if (!metaClass.hasProperty(validateable, "serializableIdentityHashCode")) {
            metaClass.getSerializableIdentityHashCode = {
                return System.identityHashCode(delegate)
            }
        }
    }

    static String createKey(object) {
        "org.codehaus.groovy.grails.ERRORS_${object.class.name}_${object.serializableIdentityHashCode}"
    }

    static boolean validateInstance(List<String> groups, List<String> includes, List<String> excludes, object, messageSource) {
        def localErrors = new GroupAwareBeanPropertyBindingResult(object, object.getClass().name, groups, includes, excludes)
        if (object.hasProperty('constraints')) {
            def constraints = filterConstraints(groups, includes, excludes, object)
            if (constraints) {
                for (constraint in constraints) {
                    constraint.messageSource = messageSource
                    validateConstraint(constraint, object, localErrors);
                }

                if (localErrors.hasErrors()) {
                    def objectErrors = object.errors;
                    localErrors.allErrors.each { localError ->
                        addLocalError(localError, objectErrors)
                    }
                }
            }

            return !object.errors.hasErrors()
        }
        return true
    }

    private static void addLocalError(FieldError localError, objectErrors) {
        def fieldName = localError.getField()
        def fieldError = objectErrors.getFieldError(fieldName)

        // if we didn't find an error OR if it is a bindingFailure...
        if (!fieldError || fieldError.bindingFailure) {
            objectErrors.addError(localError)
        }
    }

    private static void addLocalError(ObjectError localError, objectErrors) {
        objectErrors.addError(localError)
    }

    private static Set filterConstraints(groups, includes, excludes, object) {
        def constraints = [] as Set

        if (!groups && (!includes || includes.contains('*'))) {
            constraints = object.constraints.values() as Set
        } else {
            filterConstraintIncludes(includes, object, constraints)
            filterConstraintGroupIncludes(groups, object, constraints)
        }

        filterConstraintExcludes(excludes, constraints, object)

        return constraints
    }

    private static def filterConstraintIncludes(List<String> includes, object, Set constraints) {
        for (include in includes) {
            int dotIndex = include.indexOf('.')
            def firstPart = (dotIndex > 0) ? include.substring(0, dotIndex) : include
            if (object.constraints[firstPart]) {
                constraints.add(object.constraints[firstPart])
            }
        }
    }

    private static def filterConstraintGroupIncludes(List<String> groups, object, Set constraints) {
        for (group in groups) {
            if (object.constraintGroups[group]) {
                constraints.addAll(object.constraintGroups[group].values())
            }
        }
    }

    private static def filterConstraintExcludes(List<String> excludes, Set constraints, object) {
        if (excludes) {
            if (excludes.contains('*')) {
                constraints.clear()
            } else {
                for (exclude in excludes) {
                    if (object.constraints[exclude]) {
                        constraints.remove(object.constraints[exclude])
                    }
                }
            }
        }
    }

    private static def validateConstraint(ConstrainedProperty constraint, object, localErrors) {
        constraint.validate(object, object.getProperty(constraint.getPropertyName()), localErrors)
    }

    private static def validateConstraint(InstanceConstraint constraint, object, localErrors) {
        constraint.validate(object, localErrors)
    }

}
