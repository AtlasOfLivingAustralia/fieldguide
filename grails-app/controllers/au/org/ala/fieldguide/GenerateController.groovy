package au.org.ala.fieldguide

import grails.converters.JSON
import groovy.json.JsonSlurper
import org.apache.commons.io.FileUtils
import org.apache.commons.lang.time.DateFormatUtils
import org.codehaus.groovy.grails.web.json.JSONArray
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.PostMethod
import org.codehaus.groovy.grails.web.json.JSONObject

class GenerateController {

    def imageService
    def collectionsService
    def generateService
    def queueService

    def index() {
        JSON json = request.JSON

        def fileRef = generateService.generate(new JSONObject(json.toString()), null)

        //test output file exists
        if (fileRef != null) {
            response.addHeader("fileId", fileRef)

            render(status: 201, text: '')
        } else {
            render(status: 404)
        }
    }

    def fieldguide() {
        Long id = Long.parseLong(request.getParameter("id"))

        String outputDir = grailsApplication.config.fieldguide.store + File.separator
        String currentDay = DateFormatUtils.format(new Date(id), "ddMMyyyy")
        String pdfParam = currentDay + File.separator + "fieldguide" + id + ".pdf"

        //load json
        String string = FileUtils.readFileToString(new File(outputDir + id + ".json"))
        def json = new JsonSlurper().parseText(string)

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

        String text = new String(post.getResponseBody(), "UTF-8");

        //UTF-8 encoding errors removal
        text = text.replaceAll( "([\\ufffd])", "");

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

        Map map = new HashMap();
        map.put("title", json.title ? json.title : "Generated field guide");
        map.put("link", json.link ? json.link : grailsApplication.config.fieldguide.url + "/guide/" + pdfParam );
        map.put("families", taxonGroupedSorted);

        [
            data: map
        ]
    }

    def clearCache() {
        collectionsService.clearCache()
        imageService.clearCache()

        render [:] as JSON
    }

    //initiate generation of an offline field guide
    def offline() {
        if (!params.email) {
            render status: 400, text: 'Missing parameter \'email\''
        } else {
            render queueService.add(new JSONObject(params), request.JSON) as JSON
        }
    }

    //status of an offline field guide
    def status(String id) {
        render queueService.status(id) as JSON
    }

    def stats() {
        if (queueService.isApiKeyValid(params?.apiKey))
            render queueService.stats() as JSON
        else
            render status:403, text: "invalid apiKey"
    }

    def cancel(String id) {
        if (queueService.isApiKeyValid(params?.apiKey)) {
            def response = [id: id, cancelled: queueService.cancel(id)]
            render response as JSON
        } else {
            render status:403, text: "invalid apiKey"
        }
    }
}
