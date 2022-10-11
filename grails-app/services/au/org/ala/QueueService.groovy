package au.org.ala

import grails.converters.JSON
import org.apache.commons.io.FileUtils
import org.apache.commons.lang.time.DateFormatUtils

import javax.annotation.PostConstruct
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.LinkedBlockingQueue

class QueueService {

    static transactional = true

    def grailsApplication
    def generateService
    def mailService

    final LinkedHashMap<String, Map> queue = new LinkedHashMap<>()
    final LinkedBlockingQueue<String> order = new LinkedBlockingQueue<>()

    Thread [] consumers

    Set<String> apiKeys = new ConcurrentSkipListSet<>()

    @PostConstruct
    init() {
        File dir = new File("/data/fieldguide/queue/")
        if (!dir.exists()) dir.mkdirs()
        for (File queued : dir.listFiles()) {
            Map request = loadFromDisk(queued.getName().replace(".json", ""))
            if (request != null) {
                //create a new request
                add(request, null)
                queued.delete()
            }
        }
        consumers = new Thread[grailsApplication.config.threadPoolSize ?: 4]
        for (int i=0;i<consumers.length;i++) {
            consumers[i] = new ConsumerThread()
            consumers[i].start()
        }
    }

    def emailSuccess(Map request) {
        TimeZone.setDefault(TimeZone.getTimeZone(grailsApplication.config.timezone ?: "Australia/Canberra"))

        String contents = (grailsApplication.config.email.text.success ?: "Your download is available on the URL:" +
                "<br><br>[url]<br><br>When using this field guide please use the following citation:" +
                "<br><br><cite>Atlas of Living Australia field guide generated from [query] accessed on [date]." +
                "</cite><br><br>More information can be found at " +
                "<a href='http://www.ala.org.au/about-the-atlas/terms-of-use/citing-the-atlas/'>citing the ALA</a>.<br><br>")
                .replace("[url]", request.downloadUrl).replace("[date]", new Date().toString()).replace("[query]", request?.data?.link ?: "")

        String title = (grailsApplication.config.email.subject.success ?: "ALA Field Guide Download Complete - [filename]")
                .replace("[filename]", request.fileRef)

        if (grailsApplication.config.email.enabled) {
            mailService.sendMail {
                from grailsApplication.config.email.from ?: "support@ala.org.au"
                subject title
                to request.email
                html contents
            }
        } else {
            log.debug("to: " + request.email)
            log.debug("from: " + (grailsApplication.config.email.from ?: "support@ala.org.au"))
            log.debug("subject:" + title)
            log.debug("html:" + contents)
        }
    }

    def loadFromDisk(String id) {
        def found = null
        try {
            //try queued files
            File file = new File("/data/fieldguide/queue/${id}.json")
            if (!file.exists()) {
                //try finished files
                file = new File("${grailsApplication.config.fieldguide.store}/${id}")
                if (file.exists()) {
                    found = [status: "finished", downloadUrl: grailsApplication.config.fieldguide.url + "/download/offline/" + id]
                }
            } else {
                found = JSON.parse(FileUtils.readFileToString(file))
            }
        } catch (Exception e) {
           log.error("failed to load: " + id, e)
        }

        found
    }

    def saveToDisk(Map params) {
        long id = System.currentTimeMillis()
        String fileRef = DateFormatUtils.format(new Date(id), "ddMMyyyy") + "-" + "fieldguide" + id + ".pdf"

        File queued = new File("/data/fieldguide/queue/${fileRef}.json")
        queued.getParentFile().mkdirs()

        FileUtils.writeStringToFile(queued, (params).toString())

        fileRef
    }

    def deleteFromDisk(String fileRef) {
        File queued = new File("/data/fieldguide/queue/${fileRef}.json")
        if (queued.exists()) {
            FileUtils.deleteQuietly(queued)
        }
    }

    def add(params, data) {
        if (data) params.data = data

        params.status = 'inQueue'
        params.fileRef = saveToDisk(params)

        queue.put(params.fileRef.toString(), params)
        order.add(params.fileRef.toString())

        status(params.fileRef.toString())
    }

    def status(String id) {
        def status = [status: "invalid id"]
        if (id != null && id.matches("^[\\w-]+.pdf\$")) {
            Map queued = queue.get(id)
            if (!queued) {
                queued = loadFromDisk(id)
                if (queued != null && queued.containsKey('downloadUrl')) {
                    status = [status: queued.status, downloadUrl: queued.downloadUrl]
                } else {
                    status = [status: 'failed']
                }
            } else {
                if (queued?.downloadUrl) {
                    status = [status: queued.status, downloadUrl: queued.downloadUrl]
                } else {
                    status = [status: queued.status, statusUrl: grailsApplication.config.fieldguide.url + '/generate/status/' + queued.fileRef ]
                }
            }
        }

        status
    }

    class ConsumerThread extends Thread {
        def current = null

        void run() {
            try {
                while (true) {
                    Map request = queue.get(order.take())

                    //cancelled tasks will be null
                    if (request) {
                        current = request
                        try {
                            request.put('status', 'running')

                            def fileRef = generateService.generate(request.get("data"), request.get("fileRef"))

                            deleteFromDisk(request.fileRef)

                            if (fileRef != null) {
                                request.remove('statusUrl')
                                request.put('status', 'finished')
                                request.put('downloadUrl', grailsApplication.config.fieldguide.url + '/download/' + fileRef)

                                emailSuccess(request)
                            }
                        } catch (InterruptedException e) {
                            throw e
                        } catch (Exception e) {
                            //Log the error, do not retry
                            //order.put(request.get("fileRef"))

                            log.error("Failed to generate fieldguide for: " + request.toString() + " > " + e.getMessage(), e)
                        }
                        current = null
                    }
                }
            } catch (InterruptedException e) {
                //do not log shutdown requests
            }
        }
    }
}
