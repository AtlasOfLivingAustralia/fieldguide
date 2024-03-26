<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
 "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<%@ page contentType="text/html;charset=UTF-8" %>
<html>
    <head>
        <title>Field guide produced by ALA using aggregated sources</title>
        <link rel="stylesheet" href="${grailsApplication.config.getProperty('fieldguide.url')}/static/css/fieldguide.css" type="text/css"></link>
    </head>

    <body class="bodyClass">
        <div class='header mainHeader'>
            <img src="${grailsApplication.config.getProperty('fieldguide.url')}/static/images/field-guide-header-pg1.png" width="100%" height="100%" class="headerImg"/>
        </div>
    <%
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMMM yyyy");
        String formattedDate = sdf.format(new java.util.Date());
    %>
        <h4 class="headerText">This PDF was generated on <span>${formattedDate}</span>.
            <a class="headerLink" href="${data.link}"><u>View the original search query.</u></a>
        </h4><br/>

    <div class="footer footerDiv">
        <div class="alaSite"><a href="https://www.ala.org.au">Atlas of Living Australia</a> – Field Guide</div>
        <div class="pageNum">Page <span id="pagenumber"></span> of <span id="pagecount"></span></div>
    </div>

    <!-- Header for pages after the first page -->
    <br/>
    <div class='page-header'>
     <img src="${grailsApplication.config.getProperty('fieldguide.url')}/static/images/field-guide-banner-other-pages.png"
         width="100%" height="100%" class="secondHeader"/>
    </div>
        <g:each var="family" in="${data.families}">
           <g:each var="commonName" in="${family.value}" status="i" >
                <g:each var="taxon" in="${commonName.value}" status="j">
                    <div class="taxonDiv">
                        <g:if test="${i == 0}">
                            <h2 class="familyNormal">Family: <b class="upper">${family.key}</b></h2>
                            <hr class="hrClass"/>
                        </g:if>
                        <h2 class="h2ScientificName">Scientific name: <a class="classBlack" href="${grailsApplication.config.getProperty('fieldguide.species.url')}/${taxon.guid}"><b><i>${taxon.scientificName}</i></b></a></h2>
                        <g:if test="${commonName.value}">
                            <h2 class="h2Species"><b>${commonName.key}</b></h2>
                        </g:if>
                        <g:if test="${taxon.largeImageUrl != null}">
                            <img src="file://${grailsApplication.config.getProperty('fieldguide.store')}/${taxon.thumbnail.replace('?id=','/')}" width="${taxon.width}px" height="${taxon.height}px" class="imgThumbnail"/>
                        </g:if>
                        <img src="file://${grailsApplication.config.getProperty('fieldguide.store')}/${taxon.densitymap.replace('?id=','/')}" width="30%" class="densityMap"/>
                        <img src="file://${grailsApplication.config.getProperty('fieldguide.store')}/${taxon.densitylegend.replace('?id=','/')}" class="densityLegend"/>
                    </div>
                </g:each>
            </g:each>
        </g:each>



    <div class='page-header'>
        <img src="${grailsApplication.config.getProperty('fieldguide.url')}/static/images/field-guide-banner-other-pages.png"
             width="100%" height="100%" class="attHeader"/>
    </div>
    <div class="attDiv"></div>
        <h1 class="h1Attr">Attribution</h1>
        <hr style="color: gray;border-width: 0.5px; width:90%"/>
        <g:each var="family" in="${data.families}">
            <g:each var="commonName" in="${family.value}" status="i" >
                <g:each var="taxon" in="${commonName.value}" status="j">
                    <div class="attrAttributeDiv">
                        <h2><i>${taxon.scientificName}</i></h2>
                        <g:if test="${taxon.datasetName}">
                            <h3>Taxonomic information supplied by: <a
                                    href="${grailsApplication.config.getProperty('collections.url') + '/public/show/' + taxon.datasetID}">${taxon.datasetName}</a>
                            </h3>
                        </g:if>
                        <g:if test="${taxon.imageDataResourceURL}">
                            <h3>Image sourced from: <a
                                    href="${taxon.imageDataResourceURL}">${taxon.imageDataResourceName}</a></h3>
                        </g:if>
                        <g:if test="${ taxon.imageCreator }">
                            <h3>Image by: <a
                                    href="${grailsApplication.config.getProperty('collections.url') + '/public/show/' + taxon.imageDataResourceUid}">${taxon.imageCreator}</a>,
                                     <a href="${taxon.acronymUrl}">${taxon.acronym}</a>
                            </h3>
                        </g:if>
                        <g:if test="${ taxon.imageRights }">
                            <h3>Image rights: ${taxon.imageRights}</h3>
                        </g:if>
                    </div>
                </g:each>
            </g:each>
        </g:each>
    </body>
</html>
