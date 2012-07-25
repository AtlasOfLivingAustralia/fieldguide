package au.org.ala

import java.net.URL
import java.awt.Color
import java.util.Date
import org.apache.commons.io.FileUtils
import org.apache.commons.lang.time.DateFormatUtils
import scala.util.parsing.json.{JSONArray, JSON}
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.PostMethod
import javax.servlet.ServletContext
import org.apache.commons.io.IOUtils
import com.lowagie.text.pdf.{PdfStream, PdfWriter}

//import org.scalatest.mock.MockitoSugar
//import org.mockito.Mockito._
import java.io.{FileInputStream, File, FileOutputStream}
import com.lowagie.text.{Phrase, Anchor,Paragraph, Font, Table, Cell, Chunk,Section,Element}

object FieldGuideGeneratorTest {

  def main(args: Array[String]) {
    val guids = scala.io.Source.fromFile("/data/test.txt").getLines().toList
    FieldGuideGenerator.generateForList("My Birds",  "http://biocache.ala.org.au/occurrences/taxa/urn:lsid:biodiversity.org.au:afd.taxon:9bd1eeed-c14f-4372-91bd-d0d5f2d2e909", guids)
  }
}

object FieldGuideGenerator {

  val bieUrl = "http://bie.ala.org.au"
  val outputDir = "/data/fieldguides/"
  val LINK_FONT = new Font(Font.HELVETICA, 12, Font.UNDERLINE, new Color(0, 0, 255))
  val GREY_LINK_FONT = new Font(Font.HELVETICA, 12, Font.ITALIC, new Color(128,128,128))
  val SMALL_HDR = new Font(Font.HELVETICA, 18, Font.BOLD, new Color(0, 0, 0))
  val SCI_NAME = new Font(Font.HELVETICA, 12, Font.ITALIC, new Color(0, 0, 0))
  val NORMAL_TEXT = new Font(Font.HELVETICA, 12, Font.NORMAL, new Color(0, 0, 0))

  def getCurrentDay = DateFormatUtils.format(new Date(), "ddMMyyyy")

  def createTable = {
    val table = new Table(2);
    //table.setWidths(Array(300,500))
    table.setBorderWidth(0.0f);
    table.setBorder(1)
    table.setPadding(5.0f)
    table.setAlignment(0)
    table
  }

  def generateForList(queryName: String, link:String, guids: List[String]): String = {
     generateForList(queryName, link, guids, null.asInstanceOf[ServletContext])
  }

  def generateForList(queryName: String, link:String, guids: List[String], ctx:ServletContext): String = {

    //do a HTTP request to
    val guidsAsString = JSONArray(guids).toString()
    val http = new HttpClient()
    val post = new PostMethod(bieUrl + "/ws/species/fieldGuides")
    post.setRequestBody(guidsAsString)
    http.executeMethod(post)
    val taxonProfiles = JSON.parseFull(post.getResponseBodyAsString)

    val id = System.currentTimeMillis()

    val currentDay = getCurrentDay
    val filePath = getCurrentDay + File.separator + "fieldguide" + id + ".pdf"

    val dir = new File(outputDir + currentDay + File.separator)
    if (!dir.exists()) {
      FileUtils.forceMkdir(dir)
    }

    val file = new File(outputDir + filePath)
    val fout = new FileOutputStream(file)
    com.lowagie.text.Document.compress = true
    val document = new com.lowagie.text.Document()
    val pdfWriter = PdfWriter.getInstance(document, fout)
    pdfWriter.setFullCompression()
    pdfWriter.setStrictImageSequence(true)
    pdfWriter.setLinearPageMode()
    pdfWriter.setCompressionLevel(PdfStream.BEST_COMPRESSION)

    document.addTitle("Field guide produced by ALA using aggregated sources")
    document.addSubject("Field guide produced by ALA - " + queryName)
    document.addCreator("Atlas of Living Australia")
    document.addKeywords(link)
    document.addAuthor("Aggregated sources")
    document.setMargins(29.0f,29.0f,29.0f,29.0f)
    document.open

    //add the header image
  //  val inputStream = new FileInputStream(new File("/Users/davemartin/dev/ala-fieldguide/src/main/webapp/WEB-INF/images/fieldguide-header.jpg"));
    val inputStream = ctx.getResourceAsStream("/WEB-INF/images/fieldguide-header.jpg");
    val imageFile = IOUtils.toByteArray(inputStream)
    val headerImage = com.lowagie.text.Image.getInstance(imageFile)
    headerImage.scaleToFit(600.0f, 144f)
    headerImage.setBorder(0)
    headerImage.setIndentationLeft(0.0f)
    headerImage.setAbsolutePosition(0.0f, 680.0f)
    headerImage.setLeft(0.0f)
    document.add(headerImage)

    val p = new Paragraph("", new Font(Font.HELVETICA, 14, Font.ITALIC, new Color(128, 128, 128)))
    val anchor = new Anchor(new Phrase(queryName + " - click here to view original query", GREY_LINK_FONT))
    anchor.setName("LINK")
    anchor.setReference(link)
    p.add(anchor)

    p.setSpacingBefore(128.0f)
    p.setSpacingAfter(0.0f)
    p.setFirstLineIndent(0.0f)
    p.setIndentationLeft(0.0f)

    document.add(p)

    if (!taxonProfiles.isEmpty) {

      var counter = 0
      var countForPage = 0
      var pageNo = 1
      var heightTotal = 0.0f

      val unsortedProfiles = taxonProfiles.get.asInstanceOf[List[Map[String, String]]]

      //split the profiles by family
      val groupedProfiles = unsortedProfiles.groupBy(map => {
        map.getOrElse("family", "") match {
          case null => ""
          case x => x
        }
      })

      var familyNamesSorted = groupedProfiles.keySet.toIndexedSeq.sorted

      //for each family, start with a heading
      familyNamesSorted.foreach( familyName => {

          val profiles = groupedProfiles.get(familyName).get

          if(countForPage >= 2 || heightTotal > 1200.0f){
            document.newPage
            countForPage = 0
            pageNo += 1
            heightTotal = 0.0f
          }

          //http://bie.ala.org.au/species/info/Falconidae.json
          //lookup the family common name
          val p = new Paragraph(familyName, SMALL_HDR)
          p.setIndentationLeft(5.0f)
          p.setSpacingBefore(20.0f)
//          counter == 0 match {
//            case true => p.setSpacingBefore(150.0f)
//            case false => p.setSpacingBefore(20.0f)
//          } //add spacing for PDF header
          document.add(p)

          //sort by common name
          val sortedProfiles = profiles.sortBy(m => {
            val cn = m.getOrElse("commonName", "")
            if (cn != null) cn
            else ""
          })

          sortedProfiles.foreach(taxonProfile => {

            val (cells, height) = createCellsForTaxon(taxonProfile)

            println("pageNo: " + pageNo +", total height: " + heightTotal+", next image: " + height +", count for page: " + countForPage)

            if (countForPage == 1 && pageNo == 1){
              document.newPage
              countForPage = 0
              pageNo += 1
              heightTotal = 0.0f
//            } else if(countForPage == 3 || (heightTotal + height) > 1200.0f){
//              document.newPage
//              countForPage = 0
//              pageNo += 1
//              heightTotal = 0.0f
            }


            if (countForPage == 2){
              document.newPage
              countForPage = 0
              pageNo += 1
              heightTotal = 0.0f
//            } else if(countForPage == 3 || (heightTotal + height) > 1200.0f){
//              document.newPage
//              countForPage = 0
//              pageNo += 1
//              heightTotal = 0.0f
            }

            //add the cells
            val table = createTable
            table.setAlignment(0)
            //table.setBorderWidthBottom(1.0f)
            table.setTableFitsPage(true)
            cells.foreach(cell => table.addCell(cell))
            document.add(table)

            heightTotal += height
            counter += 1
            countForPage += 1
          })
        }
      )

      document.newPage

      //Attribution
      val p = new Paragraph("Attribution", new Font(Font.HELVETICA, 24, Font.BOLD, new Color(0, 0, 0)))
      p.setSpacingAfter(10.0f)
      //p.setIndentationLeft(10.0f)
      document.add(p)


      groupedProfiles.foreach( { case (familyName, profiles) => {
          //sort by common name
          //sort by common name
          val sortedProfiles = profiles.sortBy(m => {
            val cn = m.getOrElse("commonName", "")
            if (cn != null) cn
            else ""
          })

          sortedProfiles.foreach(taxonProfile => {
            val p = createAttributionParagraphForTaxon (taxonProfile)
            p.setSpacingAfter(20.0f)
            document.add(p)
          })
        }
      })

      //add the header
//      val footerImage = com.lowagie.text.Image.getInstance("/Users/davejmartin2/dev/ala-fieldguide/src/main/resources/fieldguide-cc.jpg")
//      document.add(footerImage)
      document.close
      filePath
    } else {
      "field guide not generated due to a complete programming failure"
    }
  }

  def createCellsForTaxon(taxonProfile: Map[String, String]): (List[Cell], Float) = {

    //retrieve images 1 or more images
    val imageCell = new Cell()
   // imageCell.setWidth(500.0f)
    imageCell.setBorder(0)
    //imageCell.setTop(0.0f)
    imageCell.setVerticalAlignment(Element.ALIGN_TOP)
    var imageHeight = 150.0f
    var noImageavailable = false

    if (!taxonProfile.get("thumbnail").isEmpty && taxonProfile.getOrElse("thumbnail", "") != null) {
      //retrieve distribution map
      try {
        val imageUrl = taxonProfile.getOrElse("imageURL", "")
        //val repoLocation = taxonProfile.getOrElse("imageURL","")
        val imageUrl2 = imageUrl.replace("raw", "smallRaw")
        println(imageUrl2)
        val image = com.lowagie.text.Image.getInstance(new URL(imageUrl))

        //imageCell.setVerticalAlignment(0)
        imageCell.add(image)
        imageHeight = image.getHeight()
      } catch {
        case e: Exception => {
          e.printStackTrace()
          //imageCell.add(new Chunk("No Image available"))
          noImageavailable = true
        }
      }
    } else {
      //imageCell.setBackgroundColor(new Color())
      noImageavailable = true
      //imageCell.add(new Chunk("No Image available"))
    }

    val namesCell = new Cell
    //val namesCell = imageCell
    //namesCell.setWidth(300.0f)
    namesCell.setVerticalAlignment(0)
    namesCell.setBorder(0)
    namesCell.setVerticalAlignment(Element.ALIGN_TOP)

    //common name
    val commonName = taxonProfile.getOrElse("commonName", "").asInstanceOf[String]
    if (commonName != null && commonName != "") {
      //table.addCell(commonName)
      val commonNameChunk = new Paragraph(commonName.trim + "\n", new Font(Font.HELVETICA, 12, Font.BOLD, new Color(0, 0, 0)))
      commonNameChunk.setIndentationLeft(0.0f)
      commonNameChunk.setFirstLineIndent(0.0f)

      namesCell.add(commonNameChunk)
    }

    //add scientific name
    val scientificName = taxonProfile.getOrElse("scientificName", "").asInstanceOf[String]
    val scientificNameChunk = new Paragraph(scientificName + "\n", SCI_NAME)
    namesCell.add(scientificNameChunk)
//
//    //add family name
//    val familyName = taxonProfile.getOrElse("family", "").asInstanceOf[String]
//    val familyChunk = new Chunk("Family: " + familyName + "\n", NORMAL_TEXT)
//    namesCell.add(familyChunk)

    if (noImageavailable){
      namesCell.add(new Chunk("No image currently available\n"))
    }

    //add image attribution
    if (taxonProfile.getOrElse("imageCreator", null) != null){
      namesCell.add(new Phrase("Image by: " + taxonProfile.getOrElse("imageCreator", "") + "\n" , NORMAL_TEXT))
    }

    //add anchor
    val guid = taxonProfile.getOrElse("guid", "").asInstanceOf[String]
    val anchor = new Anchor(new Phrase("ALA species page", LINK_FONT))
    anchor.setName("LINK")
    anchor.setReference("http://bie.ala.org.au/species/" + guid)
    anchor.setFont(LINK_FONT);
    namesCell.add(anchor)

//    val rowHeight = imageCell.getHeight > namesCell.getHeight match {
//      case true => imageCell.getHeight
//      case false => namesCell.getHeight
//    }

    val rowHeight = 100.0f

    if (noImageavailable){
      (List(namesCell), rowHeight)
    } else {
      (List(namesCell,imageCell), rowHeight)
    }
    //(List(imageCell), rowHeight)
  }

  def createAttributionParagraphForTaxon(taxonProfile: Map[String, String]): Paragraph = {

    //heading
    val scientificName = taxonProfile.getOrElse("scientificName", "").asInstanceOf[String]
    val attribution = new Paragraph("", NORMAL_TEXT)
    val scientificNameChunk = new Chunk(scientificName + "\n",SCI_NAME)
    attribution.add(scientificNameChunk)

    //add taxon attribution
    val taxonInfosourceUrl = taxonProfile.getOrElse("taxonInfosourceURL", "").asInstanceOf[String]
    attribution.add(new Phrase("Taxonomic information supplied by: ", NORMAL_TEXT))
    val taxonAnchor = new Anchor(new Phrase(taxonProfile.getOrElse("taxonInfosourceName", "") +"\n", LINK_FONT))
    taxonAnchor.setName("LINK")
    taxonAnchor.setReference(taxonInfosourceUrl)
    taxonAnchor.setFont(LINK_FONT);
    attribution.add(taxonAnchor)

    //add image attribution
    if (taxonProfile.getOrElse("imageInfosourceName", "") != null){
      val imageSourceUrl = taxonProfile.getOrElse("imageInfosourceURL", "").asInstanceOf[String]
      attribution.add(new Phrase("Image sourced from: ", NORMAL_TEXT))
      val imageSourceAnchor = new Anchor(new Phrase(taxonProfile.getOrElse("imageInfosourceName", "") +"\n", LINK_FONT))
      imageSourceAnchor.setName("LINK")
      imageSourceAnchor.setReference(imageSourceUrl)
      imageSourceAnchor.setFont(LINK_FONT);
      attribution.add(imageSourceAnchor)
    }

    //add image attribution
    if (taxonProfile.getOrElse("imageCreator", null) != null){
      val imageUrl = taxonProfile.getOrElse("imageisPartOf", "").asInstanceOf[String]
      attribution.add(new Phrase("Image by: ", NORMAL_TEXT))
      val imageAnchor = new Anchor(new Phrase(taxonProfile.getOrElse("imageCreator", "") +"\n", LINK_FONT))
      imageAnchor.setName("LINK")
      imageAnchor.setReference(imageUrl)
      imageAnchor.setFont(LINK_FONT);
      attribution.add(imageAnchor)
    }

    if (taxonProfile.getOrElse("imageLicence", null) != null){
      val imageLicence = new Chunk("Image licence: "+ taxonProfile.getOrElse("imageLicence", null) + "\n", NORMAL_TEXT)
      attribution.add(imageLicence)
    }

    val imageRightsValue = {
      taxonProfile.getOrElse("imageRights", null) match {
        case null => "All rights reserved"
        case _ =>  taxonProfile.getOrElse("imageRights", null)
      }
    }

    val imageRights = new Chunk("Image rights: " + imageRightsValue + "\n", NORMAL_TEXT)
    attribution.add(imageRights)

    attribution
  }
}