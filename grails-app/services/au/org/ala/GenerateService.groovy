package au.org.ala

import grails.util.Environment
import org.apache.commons.io.FileUtils
import org.apache.commons.lang.time.DateFormatUtils
import org.codehaus.groovy.grails.web.json.JSONObject

class GenerateService {

    def grailsApplication

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

        //write json to dir
        String pthJson = outputDir + id + ".json"
        FileUtils.writeStringToFile(new File(pthJson), json.toString())

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
}
