<!DOCTYPE html>
<html>
    <head>
        <link rel="shortcut icon" type="image/x-icon" href="favicon.ico">
    </head>
    <body>
        <h1>Field Guide Generator</h1>

        <p>
            HTTP POST an array of GUIDs to ${grailsApplication.config.getProperty('fieldguide.service.url')}/generate to generate a field guide.  Include in request header, Content-Type: application/json
        </p>

        <p>
            <textarea rows="8" cols="120">
            {
            "title" : "Field Guide of Mammals for ACT",
            "guids" :  [
            "https://biodiversity.org.au/afd/taxa/7702ac8d-6a0d-4555-8e03-4c4dd47d6aa5",
            "https://biodiversity.org.au/afd/taxa/5d2a30f5-deea-467d-9275-880116222f0b"
            ]
            }
            </textarea>
        </p>

        <p>This should reply with the following JSON containing a statusUrl:</p>

        <p>
            <textarea rows="8" cols="120">
            {"status":"running","statusUrl":"${grailsApplication.config.getProperty('fieldguide.service.url')}/status/23092022-fieldguide1663911782972.pdf"}
            </textarea>
        </p>

        <p>This should reply with the following JSON contain a downloadUrl:</p>

        <p>
            <textarea rows="8" cols="120">
            {"status":"finished","downloadUrl":"${grailsApplication.config.getProperty('fieldguide.service.url')}/download/23092022-fieldguide1663911782972.pdf"}
            </textarea>
        </p>
    </body>
</html>
