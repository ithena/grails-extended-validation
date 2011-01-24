package be.ixor.grails.extendedvalidation.test

import be.ixor.grails.extendedvalidation.Validateable

@Validateable
class MyDomain {
    String sized
    int minInt
    int ranged
    Map<String, SubDomain> subDomain

    def collection = []

    static constraints = {
        group1 {
            sized(maxSize: 3)
            minInt(min: 3)
            subDomain(nullable: false, cascade: true)
            sortedCollection(validator: { myDomain ->
                return myDomain?.collection == myDomain?.collection?.clone()?.sort() ? null : 'unsorted'
            })
        }
        ranged(range: 1..3)
        uniqueInCollection(validator: { myDomain ->
            def collection = myDomain?.collection
            for (element in collection) {
                if (collection.findAll { other -> other == element }.size() > 1) {
                    return ['nonunique', null]
                }
            }
        })
    }
}
