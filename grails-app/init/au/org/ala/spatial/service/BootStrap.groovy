package au.org.ala.fieldguide

import groovy.util.logging.Slf4j

@Slf4j
class BootStrap {

    def messageSource

    def init = { servletContext ->
        messageSource.setBasenames(
                "file:///var/opt/atlas/i18n/spatial-service/messages",
                "file:///opt/atlas/i18n/spatial-service/messages",
                "WEB-INF/grails-app/i18n/messages",
                "classpath:messages"
        )
    }

    def destroy = {
    }
}
