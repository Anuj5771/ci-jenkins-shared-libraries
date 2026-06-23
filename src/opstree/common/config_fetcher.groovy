package opstree.common

import groovy.json.JsonSlurper

def fetch_app_config(Map params) {

    def appName = params.application_name
    def env     = params.env
    def configUrl = "https://cplus-app-configs.qwikcilver.com/${appName}-api/${env}"

    def response = sh(
        script: "curl -s ${configUrl}",
        returnStdout: true
    ).trim()

    def json = new JsonSlurper().parseText(response)

    if (!json?.propertySources) {
        error("Config server returned invalid response for ${appName}-${env}")
    }

    // Merge all propertySources into one map
    def mergedConfig = [:]
    json.propertySources.each { ps ->
        mergedConfig.putAll(ps.source)
    }

    return mergedConfig
}
