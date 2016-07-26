package au.org.ala.fieldguide

import grails.converters.JSON
import grails.util.Environment
import groovy.json.JsonSlurper
import org.apache.commons.io.FileUtils
import org.apache.commons.lang.time.DateFormatUtils
import org.codehaus.groovy.grails.web.json.JSONArray
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.PostMethod

class GenerateController {

    def imageService
    def collectionsService

    def index() {

        JSON json = request.JSON;

        Long id = System.currentTimeMillis();
        String currentDay = DateFormatUtils.format(new Date(id), "ddMMyyyy")

        String outputDir = grailsApplication.config.fieldguide.store + File.separator;
        String pdfPath = outputDir + currentDay + File.separator + "fieldguide" + id + ".pdf";

        File dir = new File(outputDir + currentDay + File.separator)
        if (!dir.exists()) {
            FileUtils.forceMkdir(dir)
        }

        //write json to dir
        String pthJson = outputDir + id + ".json"
        FileUtils.writeStringToFile(new File(pthJson), json.toString());

        String fieldGuideUrl = grailsApplication.config.fieldguide.url + "/generate/fieldguide?id=" + id

        //generate pdf
        String [] cmd = [ grailsApplication.config.wkhtmltopdf.path,
            /* page margins (mm) */
            "-B","10","-L","0","-T","10","-R","0",
            /* encoding */
            "--encoding","UTF-8",
            /* footer settings */
            "--footer-font-size","9",
            "--footer-line",
            "--footer-left","    www.ala.org.au",
            "--footer-right","Page [page] of [toPage]     ",
            /* source page */
            fieldGuideUrl,
            /* output pdf */
            pdfPath ]

        String cmdString = cmd[0] + " \"" + cmd[1 .. cmd.length-1].join("\" \"") + "\"";
        println cmdString
        log.debug "get fieldGuide html\ncmd: " + cmdString + "\nURL: " + fieldGuideUrl + "\npdf generated: " + pdfPath

        ProcessBuilder builder = new ProcessBuilder(cmd);
        builder.environment().putAll(System.getenv());
        builder.redirectErrorStream(true);
        Process proc = builder.start();
        proc.waitFor();

        //test output file exists
        if (new File(pdfPath).exists()) {

            response.addHeader("fileId", currentDay + File.separator + "fieldguide" + id + ".pdf")

            //was successful, no longer need json
            if (Environment.current == Environment.PRODUCTION) {
                FileUtils.deleteQuietly(new File(pthJson))
            }

            render(status: 201, text: '')
        } else {
            log.error "failed to generate pdf from html\nrequest JSON: " + pthJson + "\nHTML version: " + fieldGuideUrl
            render(status: 404);
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
}
