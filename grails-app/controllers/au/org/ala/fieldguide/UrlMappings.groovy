package au.org.ala.fieldguide

class UrlMappings {

    static mappings = {

        "/"(view: "/index")
        "500"(view: '/error')

        "/generate"(controller: "generate", action: "offline")
        "/status/$id"(controller: "generate", action: "status")
        "/download/$id"(controller: "download", action: "offline")

        "/$controller/$action?/$id?" {
            constraints {
                // apply constraints here
            }
        }
    }
}
