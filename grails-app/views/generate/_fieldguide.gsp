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

        <div class='footer footerDiv'>
            <div class="alaSite">www.ala.org.au</div>
            <div class="pageNum">Page <span id="pagenumber"></span> of <span id="pagecount"></span></div>
        </div>
    <%
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMMM yyyy");
        String formattedDate = sdf.format(new java.util.Date());
    %>
    <div class="headerDiv">
        <h3 class="headerText">This PDF was generated on <span>${formattedDate}</span>.
            <a class="headerLink" href="${data.link}"><u>View the original search query.</u></a>
        </h3><br/>
    </div>

    <!-- Header for pages after the first page -->

        <br/>
        <%
            int iterationCounter = 0;
            boolean flag=false;
            boolean isFristPage=false;
        %>
        <g:each var="family" in="${data.families}">
           <g:each var="commonName" in="${family.value}" status="i" >
                <g:each var="taxon" in="${commonName.value}" status="j">
                    <div class="taxonDiv">
                        <g:if test="${i == 0}">
                            <%
                                iterationCounter++;
                                if((iterationCounter ==1) || (iterationCounter ==2)){
                                    isFristPage=true;
                            %>
                            <%
                                }

                                if ((iterationCounter >2) && (iterationCounter % 2 == 1) && (!isFristPage)) {
                                    flag=true;
                                    isFristPage=false;
                            %>

                            <div class='page-header'>
                                <img src="${grailsApplication.config.getProperty('fieldguide.url')}/static/images/field-guide-banner-other-pages.png"
                                     width="100%" height="100%" class="secondHeader"/>
                            </div>
                            <div class="pagebreakDiv"></div>
                            <h1 class="pagebreakH1">Family: <b>${family.key}</b></h1>
                            <hr class="hrPagebreak"/>
                            <%
                                }else{
                                    flag=false;
                                    isFristPage=false;
                            %>
                            <h1 style="font-weight: normal">Family: <b>${family.key}</b></h1>
                            <hr/>
                            <%
                                }
                            %>

                        </g:if>
                        <g:if test="${(j == 0) && (flag)}">
                            <h2 class="h2SpeciesPageBreak"> Species: <a style="color: black" href="${grailsApplication.config.getProperty('fieldguide.species.url')}/${taxon.guid}"><b>${commonName.key}</b></a></h2>
                            <h2 class="h2ScientificNamePageBreak">Scientific name:<b>${taxon.scientificName}</b></h2>
                        </g:if>
                        <g:else>
                            <h2 class="h2Species"> Species: <a style="color: black" href="${grailsApplication.config.getProperty('fieldguide.species.url')}/${taxon.guid}"><b>${commonName.key}</b></a></h2>
                            <h2 class="h2ScientificName">Scientific name:<b>${taxon.scientificName}</b></h2>
                        </g:else>

                        <g:if test="${taxon.largeImageUrl != null}">
                            <img src="file://${grailsApplication.config.getProperty('fieldguide.store')}/${taxon.thumbnail.replace('?id=','/')}"  width="35%" class="imgThumbnail"/>
                        </g:if>
                        <img src="file://${grailsApplication.config.getProperty('fieldguide.store')}/${taxon.densitymap.replace('?id=','/')}" width="30%" class="densityMap"/>
                        <img src="file://${grailsApplication.config.getProperty('fieldguide.store')}/${taxon.densitylegend.replace('?id=','/')}" width="10%"  height="250px" class="densityLegend"/>
                    </div>
                </g:each>
            </g:each>
        </g:each>

    <pagebreaks/>
    <pagebreaks/>
    <%
        int attributionCounter = 0;
        boolean isFlag=false;
    %>
    <div class='page-header'>
        <img src="${grailsApplication.config.getProperty('fieldguide.url')}/static/images/field-guide-banner-other-pages.png"
             width="100%" height="100%" class="attHeader"/>
    </div>
    <div class="attDiv"></div>
        <h1 class="h1Attr">Attribution</h1>
        <hr/>
        <g:each var="family" in="${data.families}">
            <g:each var="commonName" in="${family.value}" status="i" >
                <g:each var="taxon" in="${commonName.value}" status="j">
                    <div class="attrAttributeDiv">
                       <%
                            attributionCounter++;
                            if((attributionCounter>=6) && (attributionCounter % 6 ==1)){
                                isFlag=true;
                        %>
                        <br/>
                        <div class="attrAttributePageBreak"></div>
                        <div class='page-header'>
                            <img src="${grailsApplication.config.getProperty('fieldguide.url')}/static/images/field-guide-banner-other-pages.png"
                                 width="100%" height="100%" class="attHeader"/>
                        </div>
                        <br/>
                        <%
                            } else {
                                isFlag = false;
                            }
                        %>
                        <h2>${taxon.scientificName}</h2>
                        <g:if test="${taxon.datasetName}">
                            <h3>Taxonomic information supplied by: <a
                                    href="${grailsApplication.config.getProperty('collections.url') + '/public/show/' + taxon.datasetID}">${taxon.datasetName}</a>
                            </h3>
                        </g:if>
                        <g:if test="${taxon.imageDataResourceURL}">
                            <h2>Image sourced from: <a
                                    href="${taxon.imageDataResourceURL}">${taxon.imageDataResourceName}</a></h2>
                        </g:if>
                        <g:if test="${ taxon.imageCreator }">
                            <h2>Image by: <a
                                    href="${grailsApplication.config.getProperty('collections.url') + '/public/show/' + taxon.imageDataResourceUid}">${taxon.imageCreator}</a>
                            </h2>
                        </g:if>
                        <g:if test="${ taxon.imageRights }">
                            <h2>Image rights: ${taxon.imageRights}</h2>
                        </g:if>
                    </div>
                </g:each>
            </g:each>
        </g:each>
    </body>
</html>