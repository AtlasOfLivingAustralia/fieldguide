package au.org.ala.fieldguide

class DownloadController {
    def file
    def date

    //online generated field guide download
    def index() {

        String outputDir = grailsApplication.config.fieldguide.store + File.separator
        String pdfPath = outputDir + params.date + File.separator + params.file
        File myFile = new File(pdfPath)

        render ( file: myFile.newInputStream(), contentType: "application/pdf")
    }


    //offline generated field guide download
    def offline(String id) {
        if (id != null && id.matches("^[\\w-]+.pdf\$")) {
            File myFile = new File(grailsApplication.config.fieldguide.store + File.separator + id)

            render(file: myFile.newInputStream(), contentType: "application/pdf")
        } else {
            render status:400, text: 'invalid id'
        }
    }
}
