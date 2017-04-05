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

        Map map = new HashMap();
        map.put("title", json.title ? json.title : "Generated field guide");
        map.put("link", json.link ? json.link : grailsApplication.config.fieldguide.url + "/guide/" + pdfParam );
        map.put("families", json.sortedTaxonInfo);

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

    def cache(String id) {
        def file = new File("${grailsApplication.config.fieldguide.store}/cache/${URLEncoder.encode(id, 'UTF-8').replace('/','')}")
        if (file.exists()) {
            render file: file.newInputStream(), contentType: 'image/jpeg'
        } else {
            render status: 404
        }
    }
}
