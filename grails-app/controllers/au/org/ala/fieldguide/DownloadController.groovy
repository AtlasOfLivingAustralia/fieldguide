package au.org.ala.fieldguide

class DownloadController {
    def file;
    def date;

    def index() {

        String outputDir = grailsApplication.config.fieldguide.store + File.separator;
        String pdfPath = outputDir + params.date + File.separator + params.file;
        File myFile = new File(pdfPath)

        render ( file: myFile.newInputStream(), contentType: "application/pdf")
    }
}
