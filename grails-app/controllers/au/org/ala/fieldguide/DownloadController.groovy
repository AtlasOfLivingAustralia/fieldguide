package au.org.ala.fieldguide

import au.org.ala.plugins.openapi.Path
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.apache.commons.lang.time.DateUtils

import javax.ws.rs.Produces

import static io.swagger.v3.oas.annotations.enums.ParameterIn.PATH

class DownloadController {

    def amazonS3Service

    @Operation(
            method = "GET",
            tags = "fieldguide",
            operationId = "download",
            summary = "Download a fieldguide",
            description = "Download a fieldguide",
            parameters = [
                    @Parameter(
                            name = "downloadId",
                            in = PATH,
                            description = "downloadId of the fieldguide",
                            schema = @Schema(implementation = String),
                            required = true
                    )
            ],
            responses = [
                    @ApiResponse(
                            description = "Fieldguide as a PDF",
                            responseCode = "200",
                            content = [
                                    @Content(mediaType = "application/pdf",
                                            schema = @Schema(
                                                    type = "string",
                                                    format = "binary"
                                            )
                                    )

                            ]
                    )
            ],
            security = []
    )
    @Path("download/{downloadId}")
    @Produces("application/pdf")
    //offline generated field guide download
    def offline(String id) {
        if (id != null && id.matches("^[\\w-]+.pdf\$")) {
            File myFile = new File(grailsApplication.config.getProperty('fieldguide.store') + File.separator + id)

            // support switching of an instance to S3 without losing access to all previously generated local files
            if (!myFile.exists() && grailsApplication.config.getProperty('storage.provider') == 'S3') {
                if (grailsApplication.config.getProperty('storage.provider') == 'S3') {
                    response.status = 301
                    response.addHeader('Location', amazonS3Service.generatePresignedUrl(id, DateUtils.addHours(new Date(), grailsApplication.config.getProperty('s3.temporaryurl.duration', Integer.class))))
                }
            } else {
                render(file: myFile.newInputStream(), contentType: "application/pdf")
            }
        } else {
            render status: 400, text: 'invalid id'
        }
    }
}
