package be.ixor.grails.extendedvalidation

import be.ixor.grails.extendedvalidation.test.MyDomain
import be.ixor.grails.extendedvalidation.test.SubDomain

class PluginConfigTest extends GroovyTestCase {

    MyDomain myDomain

    void testHasValidationMethods() {
        MyDomain myDomain = new MyDomain()
        myDomain.subDomain = [first: new SubDomain()]
        myDomain.subDomain['first'].subSubDomain = new SubDomain()
        assert !myDomain.validate()
        assert myDomain.serializableIdentityHashCode
        assert myDomain.errors
        assert myDomain.subDomain['first'].errors
        assert myDomain.subDomain['first'].subSubDomain.errors
        assert myDomain.subDomain['first'].serializableIdentityHashCode
        assert !myDomain.subDomain['first'].validate()
    }
}
