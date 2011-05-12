package be.ixor.grails.extendedvalidation

import be.ixor.grails.extendedvalidation.test.MyDomain
import be.ixor.grails.extendedvalidation.test.SubDomain

class ValidationHelperTest extends GroovyTestCase {

    void setUp() throws Exception {
        ValidationHelper.addValidationMethods(MyDomain, null)
        ValidationHelper.addValidationMethods(SubDomain, null)
    }

    void testValidate() {
        MyDomain myDomain = new MyDomain(
                sized: '123',
                minInt: 3,
                ranged: 3
        )
        myDomain.subDomain = [first: new SubDomain(
                subSized: '123',
                subMinInt: 3,
                subRanged: 3
        )]
        myDomain.subDomain['first'].subSubDomain = new SubDomain(
                subSized: '123',
                subMinInt: 3,
                subRanged: 3
        )
        assert myDomain.validate()
        assert myDomain.serializableIdentityHashCode

        //introduce 1 error
        myDomain.subDomain['first'].subSubDomain.subMinInt = 0
        assert !myDomain.validate()
        assert myDomain.errors.allErrors.size() == 1
        assert myDomain.errors.hasFieldErrors('subDomain')
        assert myDomain.subDomain['first'].errors.allErrors.size() == 1
        assert myDomain.subDomain['first'].errors.hasFieldErrors('subSubDomain')
        assert myDomain.subDomain['first'].subSubDomain.errors.allErrors.size() == 1
        assert myDomain.subDomain['first'].subSubDomain.errors.hasFieldErrors('subMinInt')
    }

    void testValidateWithIncludes() {
        MyDomain myDomain = new MyDomain(sized: '1234')
        assert !myDomain.validate()
        assert myDomain.errors.allErrors.size() == 4

        myDomain.clearErrors()
        assert !myDomain.validate(includes: ['minInt'])
        assert myDomain.errors.allErrors.size() == 1
        assert myDomain.errors.hasFieldErrors('minInt')

        myDomain.clearErrors()
        myDomain.subDomain = [first: new SubDomain(subSized: '1234')]
        assert !myDomain.validate()
        assert myDomain.errors.allErrors.size() == 4
        assert myDomain.errors.hasFieldErrors('subDomain')
        assert myDomain.subDomain['first'].errors.allErrors.size() == 3
    }

    void testValidateWithExcludes() {
        MyDomain myDomain = new MyDomain(sized: '1234')
        myDomain.subDomain = [first: new SubDomain(subSized: '1234')]
        assert !myDomain.validate(includes: ['subDomain.*'], excludes: ['subDomain.subRanged'])
        assert myDomain.errors.allErrors.size() == 1
        assert myDomain.errors.hasFieldErrors('subDomain')
        assert myDomain.subDomain['first'].errors.allErrors.size() == 2
        assert myDomain.subDomain['first'].errors.hasFieldErrors('subMinInt')
        assert !myDomain.subDomain['first'].errors.hasFieldErrors('subRanged')

    }

    void testValidationWithGroups() {
        MyDomain myDomain = new MyDomain(sized: '1234')
        myDomain.subDomain = [first: new SubDomain(subSized: '1234')]
        assert !myDomain.validate(groups: ['group1'])
        assert myDomain.errors.allErrors.size() == 3
        assert myDomain.errors.hasFieldErrors('minInt')
        assert myDomain.errors.hasFieldErrors('sized')
        assert myDomain.errors.hasFieldErrors('subDomain')
        assert myDomain.subDomain['first'].errors.allErrors.size() == 2
        assert myDomain.subDomain['first'].errors.hasFieldErrors('subMinInt')
        assert myDomain.subDomain['first'].errors.hasFieldErrors('subSized')

        myDomain.clearErrors()
        myDomain.subDomain['first'].clearErrors()
        myDomain.subDomain['first'].subSubDomain = new SubDomain(subSized: '1234')
        assert !myDomain.validate(groups: ['group1', 'group2'], excludes: ['subDomain.subSubDomain.subMinInt'])
        assert myDomain.errors.allErrors.size() == 3
        assert myDomain.errors.hasFieldErrors('minInt')
        assert myDomain.errors.hasFieldErrors('sized')
        assert myDomain.errors.hasFieldErrors('subDomain')
        assert myDomain.subDomain['first'].errors.allErrors.size() == 3
        assert myDomain.subDomain['first'].errors.hasFieldErrors('subMinInt')
        assert myDomain.subDomain['first'].errors.hasFieldErrors('subSized')
        assert myDomain.subDomain['first'].errors.hasFieldErrors('subSubDomain')
        assert myDomain.subDomain['first'].subSubDomain.errors.allErrors.size() == 1
        assert myDomain.subDomain['first'].subSubDomain.errors.hasFieldErrors('subSized')
    }

    void testInstanceConstraintValidation() {
        MyDomain myDomain = new MyDomain(sized: '123', minInt: 4, ranged: 2)
        myDomain.subDomain = [:]
        myDomain.subDomain['first'] = new SubDomain(subSized: '123', subMinInt: 4, subRanged: 2)

        myDomain.collection = [1, 1, 2, 3]

        assert !myDomain.validate()
        def errors = myDomain.errors.allErrors
        assert errors.size() == 1
        assert (errors.first().codes as List).contains('be.ixor.grails.extendedvalidation.test.MyDomain.uniqueInCollection.nonunique')
    }

    void testExcludeInstanceConstraintValidation() {
        MyDomain myDomain = new MyDomain(sized: '123', minInt: 4, ranged: 2)
        myDomain.subDomain = [:]
        myDomain.subDomain['first'] = new SubDomain(subSized: '123', subMinInt: 4, subRanged: 2)

        myDomain.collection = [1, 1, 2, 3]

        assert myDomain.validate(excludes: ['uniqueInCollection'])
    }

    void testIncludeInstanceConstraintValidation() {
        MyDomain myDomain = new MyDomain(sized: '123', minInt: 4, ranged: 2)
        myDomain.subDomain = [:]
        myDomain.subDomain['first'] = new SubDomain(subSized: '123', subMinInt: 4, subRanged: 2)

        myDomain.collection = [1, 1, 2, 3]

        assert !myDomain.validate(includes: ['uniqueInCollection'])
    }

    void testInstanceConstraintInGroupValidation() {
        MyDomain myDomain = new MyDomain(sized: '123', minInt: 4, ranged: 2)
        myDomain.subDomain = [:]
        myDomain.subDomain['first'] = new SubDomain(subSized: '123', subMinInt: 4, subRanged: 2)

        myDomain.collection = [1, 3, 2]

        assert !myDomain.validate()
        def errors = myDomain.errors.allErrors
        assert errors.size() == 1
        assert (errors.first().codes as List).contains('unsorted')
    }

    void testExcludeInstanceConstraintInGroupValidation() {
        MyDomain myDomain = new MyDomain(sized: '123', minInt: 4, ranged: 2)
        myDomain.subDomain = [:]
        myDomain.subDomain['first'] = new SubDomain(subSized: '123', subMinInt: 4, subRanged: 2)

        myDomain.collection = [1, 3, 2]

        assert myDomain.validate(excludes: ['sortedCollection'])
    }

    void testValidateWithCascadeExcludes(){
        MyDomain myDomain = new MyDomain(sized: '123', minInt: 4, ranged: 2)
        myDomain.subDomain = [:]
        assert myDomain.validate()

        // myOtherDomain subMinInt and subRanged values will not validate
        SubDomain myOtherSubDomain = new SubDomain(subSized: '123', subMinInt: 2, subRanged: 4)
        myDomain.myOtherSubDomain = myOtherSubDomain

        // myDomain should validate since it excludes subMinInt and subRanged in the myOtherSubDomain cascade
        assert myDomain.validate()

        // myOtherDomain validation fails since it does not exclude subMinInt and subRanged
        assert !myOtherSubDomain.validate()
    }
}
