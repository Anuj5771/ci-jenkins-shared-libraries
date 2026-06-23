package opstree.common

import opstree.common.*

def dockle(Map step_params) {
    logger = new logger()
    image_scanning_check_reports = new reports_management()

    logger.logger('msg':'Performing Dockle Image Hardening Scan', 'level':'INFO')
    image_scanning_report_publish = "${step_params.image_scanning_report_publish}"

    dir("${WORKSPACE}") {
        sh "mkdir -p ${WORKSPACE}/dockle"
        sh "sudo chmod -R 777 ${WORKSPACE}/dockle"

        try {
            def imageExists = sh(script: "docker images -q ${step_params.image_name}:${step_params.image_tag}", returnStdout: true).trim()

            if (imageExists) {
                logger.logger('msg':'Image found, proceeding with Dockle scan', 'level':'INFO')

                sh """
                docker run --rm \
                    -v /var/run/docker.sock:/var/run/docker.sock \
                    goodwithtech/dockle:v0.4.15 ${step_params.image_name}:${step_params.image_tag} \
                    > ${WORKSPACE}/dockle/dockle_report.txt
                """

                logger.logger('msg':'Dockle scan completed successfully', 'level':'INFO')
            } else {
                logger.logger('msg':"Image ${step_params.image_name}:${step_params.image_tag} not found", 'level':'ERROR')
            }

            if (image_scanning_report_publish == 'true') {
                logger.logger('msg':'Publishing Dockle Report', 'level':'INFO')
                image_scanning_check_reports.publish(
                    'report_dir':"${WORKSPACE}/dockle",
                    'report_file':'dockle_report.txt',
                    'report_name':'Dockle Image Hardening Report'
                )
            } else {
                logger.logger('msg':'Dockle report publishing skipped', 'level':'INFO')
            }
        } catch (Exception e) {
            logger.logger('msg':"Dockle scan failed: ${e.message}", 'level':'ERROR')
            error 'Dockle scan failed. Please check the logs for more details.'
        }
    }
}
