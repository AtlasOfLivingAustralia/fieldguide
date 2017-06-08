package au.org.ala

import grails.converters.JSON
import grails.util.Environment
import groovy.json.JsonSlurper
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.io.FileUtils
import org.apache.commons.lang.time.DateFormatUtils
import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

class GenerateService {

    def grailsApplication
    def imageService
    def collectionsService
    def pdfRenderingService

    def generate(JSONObject json, String origFileRef) {
        long id = System.currentTimeMillis()
        String fileRef = DateFormatUtils.format(new Date(id), "ddMMyyyy") + File.separator + "fieldguide" + id + ".pdf"

        //queued downloads already have a fileRef
        if (origFileRef) {
            id = Long.parseLong(origFileRef.substring(origFileRef.lastIndexOf('e') + 1).replace(".pdf",""))
            fileRef = origFileRef
        }

        String outputDir = grailsApplication.config.fieldguide.store + File.separator
        String pdfPath = outputDir + fileRef


        File dir = new File(outputDir + fileRef)
        if (!dir.getParentFile().exists()) {
            FileUtils.forceMkdir(dir.getParentFile())
        }

        //add taxon info from bie
        json.sortedTaxonInfo = getSortedTaxonInfo(json)

        //local image cache
        cacheImages(json)

        //write json to dir
        String pthJson = outputDir + id + ".json"
        FileUtils.writeStringToFile(new File(pthJson), (json as JSON).toString())

        String fieldGuideUrl = grailsApplication.config.fieldguide.url + "/generate/fieldguide?id=" + id

        String currentDay = DateFormatUtils.format(new Date(id), "ddMMyyyy")
        String pdfParam = currentDay + File.separator + "fieldguide" + id + ".pdf"

        Map map = new HashMap()
        map.put("title", json.title ? json.title : "Generated field guide")
        map.put("link", json.link ? json.link : grailsApplication.config.fieldguide.url + "/guide/" + pdfParam )
        map.put("families", json.sortedTaxonInfo)

        def outputStream = FileUtils.openOutputStream(new File(pdfPath))

        pdfRenderingService.render([template: "/generate/fieldguide", model: [data: map], stream: true,
                filename: fileRef, controller: "generate"], outputStream)

        outputStream.flush()
        outputStream.close()

        if (!new File(pdfPath).exists()) {
            log.error "failed to generate pdf from html\nrequest JSON: " + pthJson + "\nHTML version: " + fieldGuideUrl

            null
        } else {
            //was successful, no longer need json
            if (Environment.current == Environment.PRODUCTION) {
                FileUtils.deleteQuietly(new File(pthJson))
            }

            //file reference
            fileRef
        }
    }

    def cacheImages(json) {
        def cacheDir = "${grailsApplication.config.fieldguide.store}/cache/"
        def cacheDirFile = new File(cacheDir)
        if (!cacheDirFile.exists()) cacheDirFile.mkdirs()

        //default 1 day cache age
        def maxAgeMs = System.currentTimeMillis() - (grailsApplication.config.images.cache.age.minutes ?: 24 * 20) * 60 * 1000
        for (Object o : json.sortedTaxonInfo ) {
            println(o)
        }
        json.sortedTaxonInfo.each { familyKey, family ->
            family.each { commonNameKey, commonName ->
                commonName.each { taxon ->
                    if (taxon?.guid) {
                        //density map
                        def cachedFile = new File(cacheDir + taxon.guid.replaceAll("[^a-zA-Z0-9\\-\\_\\.]", "") + ".png")
                        def cachedFileHeaderFile = new File(cacheDir + taxon.guid.replaceAll("[^a-zA-Z0-9\\-\\_\\.]", "") + ".legend.png")
                        if (!cachedFile.exists() || cachedFile.lastModified() < maxAgeMs) {
                            FileUtils.copyURLToFile(
                                    new URL("${grailsApplication.config.service.biocache.ws.url}/density/map?q=lsid:%22${taxon.guid}%22&fq=geospatial_kosher:true"),
                                    cachedFile)
                            FileUtils.copyURLToFile(
                                    new URL("${grailsApplication.config.service.biocache.ws.url}/density/legend?q=lsid:%22${taxon.guid}%22&fq=geospatial_kosher:true"),
                                    cachedFileHeaderFile)
                        }
                        taxon.densitymap = "cache?id=" + cachedFile.getName()
                        taxon.densitylegend = "cache?id=" + cachedFileHeaderFile.getName()

                        if (taxon.largeImageUrl) {
                            //species image do not expire. when the image changes the url changes
                            cachedFile = new File(cacheDir + taxon.largeImageUrl.replaceAll("[^a-zA-Z0-9\\-\\_\\.]", "") + ".jpg")
                            if (!cachedFile.exists()) {
                                FileUtils.copyURLToFile(new URL("${taxon.largeImageUrl.replace('raw', 'smallRaw')}"), cachedFile)
                            }
                            taxon.thumbnail = "cache?id=" + cachedFile.getName()
                        }
                    }
                }
            }
        }
    }

    def getSortedTaxonInfo(json) {
        if (json?.sortedTaxonInfo) {
            return json.sortedTaxonInfo
        }

        def url = grailsApplication.config.service.bie.ws.url + "/species/guids/bulklookup"
        def list = (json.getAt("guids") as JSONArray)
        list.remove("")
        String guidsAsString = list.toString()

        log.debug "get fieldGuide info from bie\nURL: " + url + "\nPOST body: " + guidsAsString

        def http = new HttpClient()
        def post = new PostMethod(url)
        post.setRequestBody(guidsAsString)
        def status = http.executeMethod(post)

        if (status != 200) {
            log.error "failed to get fieldGuide info from bie"
            return
        }

        String text = new String(post.getResponseBody(), "UTF-8")

        //UTF-8 encoding errors removal
        text = text.replaceAll( "([\\ufffd])", "")

        def taxonProfilesAll = new JsonSlurper().parseText(text).searchDTOList
        def taxonProfiles = []

        //add image metadata
        taxonProfilesAll.each { taxon ->
            if (taxon) {
                if (taxon.largeImageUrl) {
                    def imageMetadata = imageService.getInfo(taxon.largeImageUrl)
                    taxon.imageCreator = imageMetadata?.creator
                    taxon.imageDataResourceUid = imageMetadata?.dataResourceUid
                    taxon.imageRights = imageMetadata?.rights

                    if (taxon?.imageDataResourceUid) {
                        def imageDataResourceMetadata = collectionsService.getInfo(taxon.imageDataResourceUid)
                        taxon.imageDataResourceUrl = imageDataResourceMetadata.websiteUrl
                        taxon.imageDataResourceName = imageDataResourceMetadata.name
                    }
                }
                taxonProfiles.add(taxon)
            }
        }

        //group sort bie output
        def taxonGroupedSorted = taxonProfiles.groupBy (
                [{ it.family ? it.family : "" }, { it.commonNameSingle ? it.commonNameSingle : "" }]
        ).sort { a, b ->
            a.key ? b.key ? a.key <=> b.key : 1 : b.key ? -1 : 0
        }
        for (tg in taxonGroupedSorted) {
            taxonGroupedSorted.put(tg.key, tg.value.sort { a, b ->
                a.key ? b.key ? a.key <=> b.key : 1 : b.key ? -1 : 0
            })
        }

        taxonGroupedSorted
    }
}
