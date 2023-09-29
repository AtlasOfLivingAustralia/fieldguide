package au.org.ala

import grails.plugin.cache.CacheEvict
import grails.plugin.cache.Cacheable
import groovy.json.JsonSlurper

class ImageService {

    def grailsApplication
    def uuidPattern = ~/^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;

    def parseId(imageUrl) {
        def list
        def id
        if(imageUrl.contains("=")) {
            list = imageUrl.split('=')
        }
        else{
            list= imageUrl.split("/")
        }

        list.each {
            if(uuidPattern.matcher(it).matches()){
                id = it
            }
        }
        return id
    }

    @Cacheable("imageMetadata")
    def getInfo(imageUrl) {
        def md = [:]

        if (imageUrl) {
            try {
                def imageId = parseId(imageUrl)

                def jsonSlurper = new JsonSlurper()
                md = jsonSlurper.parseText(new URL(grailsApplication.config.getProperty('image.ws.url') + '/image/' + imageId).text)
            } catch (err) {
                log.error("failed to get image metadata for: " + imageUrl, err)
            }
        }

        return md
    }

    @CacheEvict(value = "imageMetadata", allEntries = true)
    def clearCache() {}
}
