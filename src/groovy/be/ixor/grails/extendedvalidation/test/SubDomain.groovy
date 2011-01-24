package be.ixor.grails.extendedvalidation.test

class SubDomain {
    String subSized
    int subMinInt
    int subRanged
    SubDomain subSubDomain

    static constraints = {
        group1 {
            subSized(maxSize: 3)
            subMinInt(min: 3)
        }
        group2 {
            subSubDomain(nullable: true, cascade: true)
        }
        subRanged(range: 1..3)
    }

}
