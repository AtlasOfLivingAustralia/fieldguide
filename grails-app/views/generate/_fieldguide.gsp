<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
 "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<%@ page contentType="text/html;charset=UTF-8" %>
<html>
    <head>
        <title>Field guide produced by ALA using aggregated sources</title>

        <link rel="stylesheet" href="${grailsApplication.config.getProperty('fieldguide.url')}/static/css/fieldguide.css" type="text/css"></link>
        <style>
        /* Define a custom class for bigger font size and different color */
        .custom-text {
            font-size: 90px;
            font-height: 50px;
            font-family: Roboto, Dialog;
        }
        </style>
    </head>

    <body style="margin-top: 260px;margin-left: 0px">
        <div class='header' style="margin-top: 0px;margin-left: 0px">
            <img src="${grailsApplication.config.getProperty('fieldguide.url')}/static/images/field-guide-banner-pg1.png" width="100%" height="60px" style="margin-bottom: 20px;margin-top: 0px"/><br/>
            <img src="${grailsApplication.config.getProperty('fieldguide.url')}/static/images/ALA_Logo_Stacked_RGB.png" width="30%" style="float:right;margin-right:20px"/>
            <div style="margin-left:30px;color: #ff0900;" class='custom-text' width="20%" height="40px">Field Guide <h2 style="color:black">Species photos and maps</h2></div>
        </div>

        <div class='footer' style="margin-top: 15px;margin-left: 30px">r
            <div style="float:left;margin-left:20px">www.ala.org.au</div>
            <div style="float:right;margin-right:10px">Page <span id="pagenumber"></span> of <span id="pagecount"></span></div>
        </div>
    <%
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMMM yyyy");
        String formattedDate = sdf.format(new java.util.Date());
    %>
    <div style="float:left; margin-left:150px">
        <h3 style="font-style: italic;font-weight: bold">This PDF was generated on <span>${formattedDate}</span>.
            <a style="margin-top:20px; color:black" href="${data.link}"><u>View the original search query</u></a>
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
                    <div style="margin-top:60px;width:90%;margin-left:50px;margin-bottom:50px">
                        <g:if test="${i == 0}">
                            <%
                                iterationCounter++;
                                if(iterationCounter ==1){
                                    isFristPage=true;
                                }
                                if ((iterationCounter >2) && (iterationCounter % 2 == 1)) {
                                    flag=true;
                                    isFristPage=false;
                            %>

                            <div class='page-header'>
                                <img src="${grailsApplication.config.getProperty('fieldguide.url')}/static/images/field-guide-banner-other-pages.png"
                                     width="100%" height="100%" style="float:left;margin-bottom:30px"/>
                            </div>
                            <div style="page-break-after: always;margin-bottom: 20px;margin-top:180px"></div>
                            <h1 style="page-break-after: always;margin-top: 135px;font-weight: normal">Family: <b>${family.key}</b></h1>
                            <hr style="margin-top: 190px"/>
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
                            <h2 style="margin-top:30px; margin-left:20px;font-weight: normal"> Species: <a style="color: black" href="${grailsApplication.config.getProperty('fieldguide.species.url')}/${taxon.guid}"><b>${commonName.key}</b></a></h2>
                            <h2 style="margin-top:5px;margin-left:20px;font-weight: normal">Scientific name:<b>${taxon.scientificName}</b></h2>
                        </g:if>
                        <g:else>
                            <h2 style="margin-left:20px;font-weight: normal"> Species: <a style="color: black" href="${grailsApplication.config.getProperty('fieldguide.species.url')}/${taxon.guid}"><b>${commonName.key}</b></a></h2>
                            <h2 style="margin-left:20px;font-weight: normal">Scientific name:<b>${taxon.scientificName}</b></h2>
                        </g:else>

                        <g:if test="${taxon.largeImageUrl != null}">
                            <img src="file://${grailsApplication.config.getProperty('fieldguide.store')}/${taxon.thumbnail.replace('?id=','/')}"  width="30%" style="height:300px;margin-top: 25px;margin-bottom: 15px;margin-left: 50px;margin-right: 30px"/>
                        </g:if>
                        <img src="file://${grailsApplication.config.getProperty('fieldguide.store')}/${taxon.densitymap.replace('?id=','/')}" width="35%" style="margin-top: 25px;margin-bottom: 5px;margin-left: 30px"/>
                        <img src="file://${grailsApplication.config.getProperty('fieldguide.store')}/${taxon.densitylegend.replace('?id=','/')}" width="100%"  height="250px" style="width:95px;margin-top: 25px;margin-bottom: 15px;margin-left: 30px"/>
                    </div>
                </g:each>
            </g:each>
        </g:each>

    <pagebreaks/>
    <pagebreaks/>
    <div class='page-header'>
        <img src="${grailsApplication.config.getProperty('fieldguide.url')}/static/images/field-guide-banner-other-pages.png"
             width="100%" height="100%" style="float:left;margin-bottom:30px"/>
    </div>
    <div style="margin-left:10px;margin-bottom: 150px;margin-top: 300px"></div>
        <h1 style="margin-left:50px;page-break-before: always;margin-top: 150px">Attribution</h1>
        <hr/>
        <g:each var="family" in="${data.families}">
            <g:each var="commonName" in="${family.value}" status="i" >
                <g:each var="taxon" in="${commonName.value}" status="j">
                    <div style="margin-top: 35px;margin-left: 50px;margin-bottom: 20px">
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
