package opstree.common

import opstree.common.*

def image_scanning_factory(Map step_params) {
    logger = new logger()
    //if (step_params.gitleaks_check == true) {
    if (step_params.image_scanning_check == 'true') {
        trivy(step_params)
    }
  else {
        logger.logger('msg':'No valid option selected for creds scanning. Please mention correct values.', 'level':'WARN')
  }
    }

def trivy(Map step_params) {
    logger = new logger()
    parser = new parser()
    image_scanning_check_reports = new reports_management()

    logger.logger('msg':'Performing Image Scanning', 'level':'INFO')
    image_scanning_report_publish = "${step_params.image_scanning_report_publish}"
    def fail_job_if_scan_failed = "${step_params.fail_job_if_scan_failed ?: 'false'}"

    dir("${WORKSPACE}") {
        sh "mkdir -p ${WORKSPACE}/trivy"
        sh "mkdir -p ${JENKINS_HOME}/trivy-cache"
        sh "sudo chmod -R 777 ${WORKSPACE}/trivy ${JENKINS_HOME}/trivy-cache"

        try {
            def imageExists = sh(script: "docker images -q ${step_params.image_name}:${step_params.image_tag}", returnStdout: true).trim()

            if (imageExists) {
                logger.logger('msg':'Image found, proceeding with Trivy scan', 'level':'INFO')

                sh """
                docker run --rm \
                    -v /var/run/docker.sock:/var/run/docker.sock \
                    -v ${WORKSPACE}/trivy:/output \
                    -v ${JENKINS_HOME}/trivy-cache:/root/.cache/trivy \
                    -e IMAGE_NAME="${step_params.image_name}" \
                    -e IMAGE_TAG="${step_params.image_tag}" \
                    -e SCAN_SEVERITY="${step_params.scan_severity}" \
                    aquasec/trivy:0.56.0 image ${step_params.image_name}:${step_params.image_tag} --format template --template "@/contrib/html.tpl" --output /output/trivy_report.html
                """
                logger.logger('msg':'Trivy scan completed successfully', 'level':'INFO')
            }

            else {
                logger.logger('msg':"Image ${step_params.image_name} not found", 'level':'ERROR')
            }

            if (image_scanning_report_publish == 'true') {
                logger.logger('msg':'Publishing Trivy Image Sdcanning Report', 'level':'INFO')
                image_scanning_check_reports.publish('report_dir':"${WORKSPACE}/trivy", 'report_file':'trivy_report.html', 'report_name':'Trivy Image Scanning Report')
            }

            else {
                logger.logger('msg':'Trivy Image Scanning Report Publishing Skipped', 'level':'INFO')
            }
        } catch (Exception e) {
            logger.logger('msg':"Trivy scan failed: ${e.message}", 'level':'ERROR')
            if (fail_job_if_scan_failed == 'true') {
                error 'Trivy scan failed. Please check the logs for more details.'
            } else {
                logger.logger('msg':'Trivy scan failed but ignoring as per user config.', 'level':'WARN')
            }
        }
    }
}
