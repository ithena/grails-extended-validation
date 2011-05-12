package be.ixor.grails.extendedvalidation.test

import be.ixor.grails.extendedvalidation.Validateable

@Validateable
class MyDomain {
    String sized
    int minInt
    int ranged
    Map<String, SubDomain> subDomain

    SubDomain myOtherSubDomain

    def collection = []

    static constraints = {
        // Constraint group
        group1 {
            sized(maxSize: 3)
            minInt(min: 3)

            // cascade:true causes the validation to be cascaded to all the SubDomain
            // entities in the sub domain map
            subDomain(nullable: false, cascade: true)
            
            sortedCollection(validator: { myDomain, errors ->
                if (myDomain?.collection != myDomain?.collection?.clone()?.sort()) {
                    errors.reject('unsorted')
                }
            })
        }

        ranged(range: 1..3)

        // Instance constraint
        uniqueInCollection(validator: {MyDomain myDomain ->
            def collection = myDomain?.collection
            for (element in collection) {
                if (collection.findAll { other -> other == element }.size() > 1) {
                    return ['nonunique', null]
                }
            }
        })

        // Cascade validation, but exclude the validation on 'subRanged' and 'subMinInt'
        myOtherSubDomain(cascade: [excludes: ['subRanged','subMinInt']])
    }
}
