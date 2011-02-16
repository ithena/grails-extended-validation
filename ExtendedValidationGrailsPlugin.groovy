import be.ixor.grails.extendedvalidation.Validateable
import be.ixor.grails.extendedvalidation.ValidationHelper
import org.codehaus.groovy.grails.beans.factory.GenericBeanFactoryAccessor
import org.springframework.context.ApplicationContext
import org.springframework.core.type.filter.AnnotationTypeFilter

class ExtendedValidationGrailsPlugin {
    // the plugin version
    def version = "1.0"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.3.50 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/conf"
    ]

    def author = "Ivo Houbrechts"
    def authorEmail = "ivo@houbrechts-it.be"
    def title = "Extended Validation Plugin"
    def description = 'This plugin provides partial validation of (groups) of fields, cascaded validation and instance validators for non-domain objects.'

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/extended-validation"

    def typeFilters = [new AnnotationTypeFilter(Validateable)]

    def doWithDynamicMethods = { ApplicationContext ctx ->
        // list of validateable classes
        def validateables = []

        // grab all of the classes specified in the application config
        application.config?.grails?.extendedvalidation?.classes?.each {
            validateables << it
        }

        def accessor = new GenericBeanFactoryAccessor(ctx)
        for (entry in accessor.getBeansWithAnnotation(Validateable)) {
            Class validateable = entry?.value?.class
            if (validateable)
                validateables << validateable
        }

        // make all of these classes 'validateable'
        for (validateableClass in validateables) {
            log.debug "Making Class Validateable: ${validateableClass.name}"
            ValidationHelper.addValidationMethods(validateableClass, ctx)
        }
    }
}
