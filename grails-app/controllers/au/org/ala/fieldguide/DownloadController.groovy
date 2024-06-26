package au.org.ala.fieldguide

import au.org.ala.plugins.openapi.Path
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import com.amazonaws.services.s3.model.ResponseHeaderOverrides
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
    def queueService

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
                    ),
                    @ApiResponse(
                            description = "Redirect to PDF",
                            responseCode = "302"
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

            // fetch the requested filename
            String filename = queueService.storedFilename(id)
            if (!filename?.endsWith(".pdf")) {
                filename += ".pdf"
            }

            // support switching of an instance to S3 without losing access to all previously generated local files
            if (!myFile.exists() && grailsApplication.config.getProperty('storage.provider') == 'S3') {
                if (grailsApplication.config.getProperty('storage.provider') == 'S3') {
                    response.status = 302

                    String locationUrl
                    if (filename) {
                        ResponseHeaderOverrides responseHeaders = new ResponseHeaderOverrides().withContentDisposition("attachment; filename=${filename}")
                        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(amazonS3Service.serviceConfig.bucket, id).withResponseHeaders(responseHeaders)
                        locationUrl = amazonS3Service.client.generatePresignedUrl(request).toString()
                    } else {
                        locationUrl = amazonS3Service.generatePresignedUrl(id, DateUtils.addHours(new Date(), grailsApplication.config.getProperty('s3.temporaryurl.duration', Integer.class)))
                    }

                    response.addHeader('Location', locationUrl)
                }
            } else {
                response.setHeader('content-disposition', "attachment; filename=${filename}")
                render(file: myFile.newInputStream(), contentType: "application/pdf", contentDis)
            }
        } else {
            render status: 400, text: 'invalid id'
        }
    }
}
