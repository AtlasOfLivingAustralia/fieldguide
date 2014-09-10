class UrlMappings {

	static mappings = {

        "/"(view:"/index")
        "500"(view:'/error')

        "/guide/$date/$file"(controller: "download", action: "index")

        "/$controller/$action?/$id?"{
            constraints {
                // apply constraints here
            }
        }
	}
}
