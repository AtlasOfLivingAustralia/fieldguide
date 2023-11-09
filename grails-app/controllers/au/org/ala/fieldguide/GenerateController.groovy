package au.org.ala.fieldguide

import au.org.ala.plugins.openapi.Path
import au.org.ala.web.UserDetails
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import grails.converters.JSON
import groovy.json.JsonSlurper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.parameters.RequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import org.apache.commons.io.FileUtils
import org.apache.commons.lang.time.DateFormatUtils
import org.grails.web.json.JSONObject

import javax.ws.rs.Produces

import static io.swagger.v3.oas.annotations.enums.ParameterIn.PATH
import static io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY

class GenerateController {

    def queueService
    def authService

    def fieldguide() {
        Long id = Long.parseLong(request.getParameter("id"))

        String outputDir = grailsApplication.config.getProperty('fieldguide.store') + File.separator
        String currentDay = DateFormatUtils.format(new Date(id), "ddMMyyyy")
        String pdfParam = currentDay + File.separator + "fieldguide" + id + ".pdf"

        //load json
        String string = FileUtils.readFileToString(new File(outputDir + id + ".json"))
        def json = new JsonSlurper().parseText(string)

        Map map = new HashMap()
        map.put("title", json.title ? json.title : "Generated field guide")
        map.put("link", json.link ? json.link : grailsApplication.config.getProperty('fieldguide.url') + "/guide/" + pdfParam)
        map.put("families", json.sortedTaxonInfo)

        def stream = params.stream ? params.stream : true

        renderPdf(template: "/generate/fieldguide", model: [data: map],
                filename: "fieldguide" + id + ".pdf", controller: "generate", stream: stream)

    }

    @Operation(
            method = "POST",
            tags = "fieldguide",
            operationId = "generate",
            summary = "Initiate the generation of a fieldguide",
            description = "Initiate the generation of a fieldguide",
            parameters = [
                    @Parameter(
                            name = "email",
                            in = QUERY,
                            description = "User email address",
                            schema = @Schema(implementation = String),
                            required = true
                    )
            ],
            requestBody = @RequestBody(
                    description = "Fieldguide parameters",
                    required = true,
                    content = [
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = FieldguideRequest)
                            )
                    ]
            ),
            responses = [
                    @ApiResponse(
                            description = "Status of the fieldguide (`queued`, `running`, `finished`) and statusUrl or downloadUrl",
                            responseCode = "200",
                            content = [
                                    @Content(
                                            mediaType = "application/json",
                                            schema = @Schema(implementation = FieldguideResponse)
                                    )
                            ]
                    ),
                    @ApiResponse(
                            description = "no email provided",
                            responseCode = "400"
                    )
            ],
            security = []
    )
    @Path("generate")
    @Produces("application/json")
    //initiate generation of an offline field guide
    def offline() {
        if (grailsApplication.config.getProperty('validateEmail', boolean) &&
                (grailsApplication.config.getProperty('security.cas.enabled', boolean) || grailsApplication.config.getProperty('security.oidc.enabled', boolean))) {
            // use logged in user's email
            String validEmail = authService.email

            // validate against registered user emails
            if (!validEmail) {
                UserDetails userDetails = authService.getUserForEmailAddress(params.email, true)
                if (userDetails && !userDetails.locked) {
                    validEmail = userDetails.email
                }
            }

            params.email = validEmail
        }

        if (!params.email) {
            render status: 400, text: 'Invalid parameter \'email\''
        } else {
            render queueService.add(new JSONObject(params), request.JSON) as JSON
        }
    }

    // Only used by openapi annotations
    @JsonIgnoreProperties(["metaClass"])
    class FieldguideResponse {
        String status
        URL statusUrl
        URL downloadUrl
    }

    // Only used by openapi annotations
    @JsonIgnoreProperties(["metaClass"])
    class FieldguideRequest {
        String title
        List<String> guids
    }

    @Operation(
            method = "GET",
            tags = "fieldguide",
            operationId = "status",
            summary = "Show the status of a fieldguide",
            description = "Show the status of a  fieldguide",
            parameters = [
                    @Parameter(
                            name = "id",
                            in = PATH,
                            description = "Id of the fieldguide",
                            schema = @Schema(implementation = String),
                            required = true
                    )
            ],
            responses = [
                    @ApiResponse(
                            description = "Status of the fieldguide (`queued`, `running`, `finished`) and statusUrl or downloadUrl",
                            responseCode = "200",
                            content = [
                                    @Content(
                                            mediaType = "application/json",
                                            schema = @Schema(implementation = FieldguideResponse)
                                    )
                            ]
                    )
            ],
            security = []
    )
    @Path("status/{id}")
    @Produces("application/json")
    //status of an offline field guide
    def status(String id) {
        render queueService.status(id) as JSON
    }

    def cache(String id) {
        def file = new File("${grailsApplication.config.getProperty('fieldguide.store')}/cache/${URLEncoder.encode(id, 'UTF-8').replace('/', '')}")
        if (file.exists()) {
            render file: file.newInputStream(), contentType: 'image/jpeg'
        } else {
            render status: 404
        }
    }
}
