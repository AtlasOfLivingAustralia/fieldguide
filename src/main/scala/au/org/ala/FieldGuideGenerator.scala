package au.org.ala

import java.io.{File, FileOutputStream}
import com.lowagie.text.pdf.PdfWriter
import java.io.FileOutputStream
import java.net.URL
import java.awt.Color
import com.lowagie.text.{Phrase, Anchor,Paragraph, Font, Table, Cell, Chunk,Section}
import java.util.Date
import org.apache.commons.io.FileUtils
import org.apache.commons.lang.time.DateFormatUtils
import scala.util.parsing.json.{JSONArray, JSON}
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.PostMethod

object FieldGuideGenerator {

  def main(args:Array[String]){
    val testset = List(
        "urn:lsid:biodiversity.org.au:afd.taxon:8969a0e0-3d46-4c6e-a233-1ba9e82a2886",
        "urn:lsid:biodiversity.org.au:afd.taxon:7640dbd0-5e55-46ae-9171-7eafadc2b21e"
    )
    generateForList("Test Generation", testset)
  }

  def getCurrentDay = DateFormatUtils.format(new Date(), "ddMMyyyy")

  def generateForList(queryName:String, guids:List[String]) : String = {

    //do a HTTP request to
    val guidsAsString = JSONArray(guids).toString()
    val http = new HttpClient()
    val post = new PostMethod("http://diasbtest1-cbr.vm.csiro.au:8080/bie-webapp/species/fieldGuides")
    post.setRequestBody(guidsAsString)
    http.executeMethod(post)
    val taxonProfiles = JSON.parseFull(post.getResponseBodyAsString)

    val id = System.currentTimeMillis()

    val currentDay = getCurrentDay
    val filePath = getCurrentDay + File.separator + "fieldguide" + id + ".pdf"

    val dir = new File("/data/fieldguides/" + currentDay + File.separator)
    if (!dir.exists()){
      FileUtils.forceMkdir(dir)
    }

    val file = new File("/data/fieldguides/"+filePath)
    val fout = new FileOutputStream(file)
    val document = new com.lowagie.text.Document()
    PdfWriter.getInstance(document,fout)
    document.open
    document.addTitle("Field guide produced by ALA using aggregated sources")
    document.addSubject("Field guide produced by ALA")
    document.addAuthor("Aggregated sources")

    document.add(new Phrase(queryName, new Font(Font.HELVETICA, 18, Font.BOLD, new Color(0, 0, 0)) ))

    //image, scientific name, common names, distribution map?
    val table = new Table(2);
    table.setBorderWidth(0.0f);
    table.setBorder(0)
    table.setBorderColor(new Color(0, 0, 0));
    table.setCellsFitPage(false);
    table.setBorderColorLeft(new Color(255, 255, 255));
    table.setBorderColorRight(new Color(255, 255, 255));
    table.setBorderColor(new Color(255, 255, 255));
    table.setTableFitsPage(false)
    table.setPadding(10.0f)
    table.setAlignment(0)
    table.setUseVariableBorders(true)

    if (!taxonProfiles.isEmpty){
      taxonProfiles.get.asInstanceOf[List[Map[String,String]]].foreach(taxonProfile => {

        //retrieve common names
        //retrieve images 1 or more images
        if (!taxonProfile.get("thumbnail").isEmpty && taxonProfile.getOrElse("thumbnail", "") != null){
          //retrieve distribution map
          println(taxonProfile)

          val repoLocation = taxonProfile.getOrElse("thumbnail","")
          val imageUrl = repoLocation.replace("raw", "thumbnail")
          val image = com.lowagie.text.Image.getInstance(new URL(imageUrl))
          try {
            table.addCell(new Cell(image))
          } catch {
            case e:Exception => e.printStackTrace(); table.addCell("No Image available")
          }
        } else {
          table.addCell("No Image available")
        }

        val namesCell = new Cell

        //common name
        val commonName = taxonProfile.getOrElse("commonName", "").asInstanceOf[String]
        if(commonName != ""){
          //table.addCell(commonName)
          val commonNameChunk  = new Paragraph(commonName +"\n", new Font(Font.HELVETICA, 12, Font.BOLD, new Color(0, 0, 0)) )
          namesCell.add(commonNameChunk)
        }

        //add scientific name
        val scientificName = taxonProfile.getOrElse("scientificName", "").asInstanceOf[String]
        val scientificNameChunk = new Paragraph(scientificName +"\n", new Font(Font.HELVETICA, 12, Font.ITALIC, new Color(0, 0, 0)) )
        namesCell.add(scientificNameChunk)

        //add family name
        val familyName = taxonProfile.getOrElse("family", "").asInstanceOf[String]
        val familyChunk = new Chunk(familyName+"\n", new Font(Font.HELVETICA, 12, Font.NORMAL, new Color(0, 0, 0)) )
        namesCell.add(familyChunk)

        //add anchor
        val guid = taxonProfile.getOrElse("guid", "").asInstanceOf[String]
        val anchor = new Anchor(new Phrase("ALA species page", new Font(Font.HELVETICA, 12, Font.UNDERLINE, new Color(0, 0, 255)) ))
        anchor.setName("LINK")
        anchor.setReference("http://bie.ala.org.au/species/"+guid)
        anchor.setFont(new Font(Font.HELVETICA, 12, Font.UNDERLINE, new Color(0, 0, 255)) );
        namesCell.add(anchor)

        //add the name stuff
        table.addCell(namesCell)

//        try {
//          val image = com.lowagie.text.Image.getInstance(new URL("http://spatial.ala.org.au/output/sampling/urn_lsid_biodiversity.org.au_afd.taxon_7790064f-4ef7-4742-8112-6b0528d5f3fb.png"))
//          table.addCell(new Cell(image))
//        } catch {
//          case e:Exception => e.printStackTrace(); table.addCell("No Image available")
//        }

        //http://spatial.ala.org.au/output/sampling/urn_lsid_biodiversity.org.au_afd.taxon_7790064f-4ef7-4742-8112-6b0528d5f3fb.png


        //table.addCell(anchorCell)
      })

      document.add(table)
      document.close
      filePath
    } else {
      "field guide not"
    }
  }
}