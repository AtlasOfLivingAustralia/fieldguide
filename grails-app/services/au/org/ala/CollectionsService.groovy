package au.org.ala

import grails.plugin.cache.CacheEvict
import grails.plugin.cache.Cacheable
import groovy.json.JsonSlurper

class CollectionsService {

    def grailsApplication

    @Cacheable("collectionMetadata")
    def getInfo(dataResourceUid) {
        def md = [:]

        try {
            def jsonSlurper = new JsonSlurper()
            // updating config retrieval to allow fo backwards compatibility and still work with api endpoint from apigateway
            md = jsonSlurper.parseText(new URL((grailsApplication.config.getProperty('collections.ws.url', String, null) ?: grailsApplication.config.getProperty('collections.url')) + '/ws/dataResource/' + dataResourceUid).text)
        } catch (err) {
            log.error("failed to get collection metadata for: " + dataResourceUid, err)
        }

        return md
    }

    @CacheEvict(value = "collectionMetadata", allEntries = true)
    def clearCache() {}
}
