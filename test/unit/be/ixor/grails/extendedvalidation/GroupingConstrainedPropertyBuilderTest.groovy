package be.ixor.grails.extendedvalidation

import be.ixor.grails.extendedvalidation.test.MyDomain

class GroupingConstrainedPropertyBuilderTest extends GroovyTestCase {

    MyDomain myDomain

    void setUp() throws Exception {
        ValidationHelper.addValidationMethods(MyDomain, null)
        myDomain = new MyDomain()
    }

    void testPropertyConstraintsCreated() {
        def propertyConstraints = myDomain.constraints.findAll { it.value?.hasProperty('propertyName') }

        assert 5 == propertyConstraints.size()
        assert propertyConstraints['sized']
        assert propertyConstraints['minInt']
        assert propertyConstraints['ranged']
        assert propertyConstraints['subDomain']
        assert propertyConstraints['myOtherSubDomain']
    }

    void testGroupConstraintsCreated() {
        def constraintGroups = myDomain.constraintGroups
        assert 1 == constraintGroups.size()
        assert constraintGroups['group1']
    }

    void testCustomConstraintsCreated() {
        def customConstraints = myDomain.constraints.findAll { !it.value?.hasProperty('propertyName') }
        assert 2 == customConstraints.size()
        assert customConstraints['uniqueInCollection']
        assert customConstraints['sortedCollection']
    }
}
