<!DOCTYPE html>
<html>
    <head>
        <link rel="shortcut icon" type="image/x-icon" href="favicon.ico">
    </head>
    <body>
        <h1>Field Guide Generator</h1>

        <p>
            HTTP POST an array of GUIDs to /generate to generate a field guide.  Include in request header, Content-Type: application/json
        </p>

        <p>
            <textarea rows="8" cols="120">
            {
            "title" : "Field Guide of Mammals for ACT",
            "guids" :  [
            "urn:lsid:biodiversity.org.au:afd.taxon:8969a0e0-3d46-4c6e-a233-1ba9e82a2886",
            "urn:lsid:biodiversity.org.au:afd.taxon:7640dbd0-5e55-46ae-9171-7eafadc2b21e"
            ]
            }
            </textarea>
        </p>

        <p>This should reply with the following HTTP headers:</p>

        <p>
            <textarea rows="8" cols="120">
            HTTP/1.1 201 Created
            Fileid: 30082011/fieldguide1314682018564.pdf
            Content-Type: text/html; charset=utf-8
            </textarea>
        </p>

        <p>A URL to download can then be constructed.
        e.g /guide/30082011/fieldguide1314682018564.pdf
        </p>
    </body>
</html>
