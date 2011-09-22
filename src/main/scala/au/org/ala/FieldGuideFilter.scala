package au.org.ala

import org.scalatra._
import java.net.URL
import scalate.ScalateSupport
import java.io.File
import scala.util.parsing.json.JSON

class FieldGuideFilter extends ScalatraFilter with ScalateSupport {
  
  get("/") {
    <html>
      <body>
        <h1>Field Guide Generator</h1>
        <p>
          HTTP POST an array of GUIDs to /getFieldGuide to generate a field guide
        </p>
        <p><textarea rows="8" cols="120"><![CDATA[
          {
           "title" : "Field Guide of Mammals for ACT",
           "guids" :  [
              "urn:lsid:biodiversity.org.au:afd.taxon:8969a0e0-3d46-4c6e-a233-1ba9e82a2886",
              "urn:lsid:biodiversity.org.au:afd.taxon:7640dbd0-5e55-46ae-9171-7eafadc2b21e"
            ]
          }
          ]]>
          </textarea>
        </p>
        <p> This should reply with the following HTTP headers: </p>
        <p><textarea rows="8" cols="120"><![CDATA[
            HTTP/1.1 201 Created
            Fileid: 30082011/fieldguide1314682018564.pdf
            Content-Type: text/html; charset=utf-8
          ]]>
          </textarea>
        </p>
        <p> A URL to download can then be constructed.
          e.g http://..../generatedGuide/30Aug2011/fieldguide1314682018564.pdf
        </p>
      </body>
    </html>
  }

  post("/generate") {
    val json = JSON.parseFull(request.body)
    json match {
      case Some(list) => {
        val jsonMap = list.asInstanceOf[Map[String, Object]]
        val title = jsonMap.getOrElse("title", "Generated field guide").asInstanceOf[String]
        val link = jsonMap.getOrElse("link", "").asInstanceOf[String]
        val guidList = jsonMap.getOrElse("guids", List()).asInstanceOf[List[String]]
        if (guidList.isEmpty){
          println("No guids supplied")
          response.sendError(400)
        } else {
          val fileName = FieldGuideGenerator.generateForList(title, link, guidList, servletContext)
          response.setHeader("fileId", fileName)
          response.setStatus(201)
        }
      }
      case None => {
        println("Unable to parse input")
        response.sendError(400)
      }
    }
  }

  get("/guide/:directory/:file"){
    response.setContentType("application/pdf")
    new File("/data/fieldguides/"+params("directory") + File.separator + params("file"))
  }

  notFound {
    // If no route matches, then try to render a Scaml template
    val templateBase = requestPath match {
      case s if s.endsWith("/") => s + "index"
      case s => s
    }
    val templatePath = "/WEB-INF/scalate/templates/" + templateBase + ".scaml"
    servletContext.getResource(templatePath) match {
      case url: URL => 
        contentType = "text/html"
        templateEngine.layout(templatePath)
      case _ => 
        filterChain.doFilter(request, response)
    } 
  }
}
