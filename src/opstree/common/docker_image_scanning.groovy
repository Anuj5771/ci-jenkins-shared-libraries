package opstree.common

import opstree.common.*

def image_scanning_factory(Map step_params) {
    def logger = new logger()
    if (step_params.image_scanning_check == 'true') {
        trivy(step_params)
    } else {
        logger.logger('msg':'No valid option selected for image scanning. Please mention correct values.', 'level':'WARN')
    }
}

def trivy(Map step_params) {
    def logger              = new logger()
    def parser              = new parser()
    def image_scanning_reports = new reports_management()

    logger.logger('msg':'Performing Image Scanning', 'level':'INFO')

    def image_scanning_report_publish = "${step_params.image_scanning_report_publish}"
    def fail_job_if_scan_failed       = "${step_params.fail_job_if_scan_failed ?: 'false'}"
    def trivyCacheDir                 = "${JENKINS_HOME}/trivy-cache"
    def javaDbPath                    = "${trivyCacheDir}/fanal/javadb"

    dir("${WORKSPACE}") {
        sh "mkdir -p ${WORKSPACE}/trivy"
        sh "mkdir -p ${trivyCacheDir}"
        sh "sudo chmod -R 777 ${WORKSPACE}/trivy ${trivyCacheDir}"

        // ── Java DB freshness check ──────────────────────────────────────────
        // trivy-java-db is 883 MiB and takes 2+ min to download.
        // Skip the update if the local copy is less than 24 hours old to avoid
        // intermittent ghcr.io connectivity failures on every build.
        def skipJavaDbFlag = sh(script: """
            JAVA_DB_META="${trivyCacheDir}/fanal/javadb/metadata.json"
            if [ -f "\$JAVA_DB_META" ]; then
                DB_AGE_HOURS=\$(( ( \$(date +%s) - \$(stat -c %Y "\$JAVA_DB_META" 2>/dev/null || echo 0) ) / 3600 ))
                if [ "\$DB_AGE_HOURS" -lt "24" ] 2>/dev/null; then
                    echo "--skip-java-db-update"
                else
                    echo ""
                fi
            else
                echo ""
            fi
        """, returnStdout: true).trim()

        if (skipJavaDbFlag == '--skip-java-db-update') {
            logger.logger('msg':'Trivy Java DB is fresh (< 24h) - skipping Java DB download', 'level':'INFO')
        } else {
            logger.logger('msg':'Trivy Java DB is stale or missing - downloading Java DB', 'level':'INFO')
        }

        try {
            def imageExists = sh(
                script: "docker images -q ${step_params.image_name}:${step_params.image_tag}",
                returnStdout: true
            ).trim()

            if (imageExists) {
                logger.logger('msg':'Image found, proceeding with Trivy scan', 'level':'INFO')

                // --scanners vuln   → disable secret scanning (saves ~30s and avoids
                //                     the "scanning is slow" warning in Trivy logs)
                // --skip-java-db-update → use cached Java DB when it is still fresh
                sh """
                    docker run --rm \\
                        -v /var/run/docker.sock:/var/run/docker.sock \\
                        -v ${WORKSPACE}/trivy:/output \\
                        -v ${trivyCacheDir}:/root/.cache/trivy \\
                        -e IMAGE_NAME="${step_params.image_name}" \\
                        -e IMAGE_TAG="${step_params.image_tag}" \\
                        -e SCAN_SEVERITY="${step_params.scan_severity}" \\
                        aquasec/trivy:0.56.0 image \\
                            --scanners vuln \\
                            --severity "${step_params.scan_severity}" \\
                            ${skipJavaDbFlag} \\
                            --format template \\
                            --template "@/contrib/html.tpl" \\
                            --output /output/trivy_report.html \\
                            ${step_params.image_name}:${step_params.image_tag}
                """
                logger.logger('msg':'Trivy scan completed successfully', 'level':'INFO')
            } else {
                logger.logger('msg':"Image ${step_params.image_name}:${step_params.image_tag} not found locally. Build may not have run.", 'level':'ERROR')
            }

            if (image_scanning_report_publish == 'true') {
                logger.logger('msg':'Publishing Trivy Image Scanning Report', 'level':'INFO')
                image_scanning_reports.publish(
                    'report_dir' : "${WORKSPACE}/trivy",
                    'report_file': 'trivy_report.html',
                    'report_name': 'Trivy Image Scanning Report'
                )
            } else {
                logger.logger('msg':'Trivy Image Scanning Report Publishing Skipped', 'level':'INFO')
            }

        } catch (Exception e) {
            logger.logger('msg':"Trivy scan failed: ${e.message}", 'level':'ERROR')
            if (fail_job_if_scan_failed == 'true') {
                error "Trivy scan failed: ${e.message}"
            } else {
                logger.logger('msg':'Trivy scan failed but ignoring as per user config.', 'level':'WARN')
            }
        }
    }
}
