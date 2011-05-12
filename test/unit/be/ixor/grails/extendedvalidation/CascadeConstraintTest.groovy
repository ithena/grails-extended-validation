package be.ixor.grails.extendedvalidation

class CascadeConstraintTest extends GroovyTestCase {

    CascadeConstraint cascadeConstraint

    @Override
    protected void setUp() {
        super.setUp()
        cascadeConstraint = new CascadeConstraint()
    }

    void testSetParameter_null(){
        try{
            cascadeConstraint.setParameter(null)
            fail('Expected an IllegalArgumentException')
        } catch(IllegalArgumentException){
            // As expected
        }
    }

    void testSetParameter_boolean(){
        cascadeConstraint.setParameter(true)
        assert cascadeConstraint.cascade

        cascadeConstraint.setParameter(false)
        assert !cascadeConstraint.cascade

        cascadeConstraint.setParameter(Boolean.TRUE)
        assert cascadeConstraint.cascade
    }

    void testSetParameter_map(){
        cascadeConstraint.setParameter([excludes:null])
        assert cascadeConstraint.cascade
        assert !cascadeConstraint.excludes

        cascadeConstraint.setParameter([excludes: ['exclude1']])
        assert cascadeConstraint.cascade
        assert cascadeConstraint.excludes == ['exclude1']
    }

    void testSetParameter_illegal_type(){
        try{
            cascadeConstraint.setParameter(['exclude1','exclude2'])
            fail('Expected an IllegalArgumentException')
        } catch(IllegalArgumentException){
            // As expected
        }
    }

    void testSetExcludes_one_exclude(){
        cascadeConstraint.setExcludes('exclude')
        assert cascadeConstraint.excludes == ['exclude']
    }

    void testSetExcludes_list_of_excludes(){
        cascadeConstraint.setExcludes(['exclude1','exclude2'])
        assert cascadeConstraint.excludes == ['exclude1','exclude2']
    }

    void testSetExcludes_illegal_excludes_definition(){
        try{
            cascadeConstraint.setExcludes([new Class()])
            fail('Expected an IllegalArgumentException')
        } catch(IllegalArgumentException){
            // As expected
        }
    }

}
