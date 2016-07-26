package au.org.ala

import grails.plugin.cache.CacheEvict
import grails.plugin.cache.Cacheable
import groovy.json.JsonSlurper

class ImageService {

    def grailsApplication

    def parseId(imageUrl) {
        return imageUrl.split('=')[1]
    }

    @Cacheable("imageMetadata")
    def getInfo(imageUrl) {
        def md = [:]

        if (imageUrl) {
            try {
                def imageId = parseId(imageUrl)

                def jsonSlurper = new JsonSlurper()
                md = jsonSlurper.parseText(new URL(grailsApplication.config.image.ws.url + '/image/' + imageId).text)
            } catch (err) {
                log.error("failed to get image metadata for: " + imageUrl, err)
            }
        }

        return md
    }

    @CacheEvict(value = "imageMetadata", allEntries = true)
    def clearCache() {}
}
