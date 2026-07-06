package opstree.common

import opstree.common.*

def creds_scanning_factory(Map step_params) {
    if (step_params.gitleaks_check == true || step_params.gitleaks_check == 'true') {
        gitleaks(step_params)
    } else {
        def logger = new logger()
        logger.logger('msg':'No valid option selected for creds scanning. Please mention correct values.', 'level':'WARN')
    }
}

def gitleaks(Map step_params) {
    def logger  = new logger()
    def parser  = new parser()
    def reports = new reports_management()

    logger.logger('msg':'Performing Credentials Scanning (Gitleaks)', 'level':'INFO')

    /* ---------------- INPUTS ---------------- */
    String repoDir = parser.fetch_git_repo_name(repo_url: step_params.repo_url)

    boolean publishHtml =
        step_params.gitleaks_report_jenkins_publish == true ||
        step_params.gitleaks_report_jenkins_publish == 'true'

    boolean failOnLeak =
        step_params.fail_job_if_leak_detected == true ||
        step_params.fail_job_if_leak_detected == 'true'

    /* ---------------- SCAN MODE ---------------- */
    String scanMode = step_params.gitleaks_scan_mode ?: 'latest'
    String logOpts

    switch (scanMode) {
        case 'full':
            logOpts = '--all'
            break
        case 'last5':
            logOpts = '-5'
            break
        case 'last10':
            logOpts = '-10'
            break
        default:
            logOpts = '-1'
    }

    logger.logger('msg':"Gitleaks scan mode: ${scanMode}", 'level':'INFO')
    logger.logger('msg':"Git log options: ${logOpts}", 'level':'INFO')

    dir("${WORKSPACE}/gitleaks") {

        sh "mkdir -p ."
        sh "chmod -R 777 ."

        /* ---------------- ENSURE FULL GIT HISTORY ---------------- */
        sh """
          cd ${WORKSPACE}/${repoDir}
          if [ -f .git/shallow ]; then
            echo "Repository is shallow. Fetching full history..."
            git fetch --unshallow || git fetch --all
          else
            echo "Repository already has full history."
          fi
        """

        /* ---------------- LOAD RENDER FILES ---------------- */
        writeFile file: 'report.html',
                  text: libraryResource('gitleaks/render/report.html')

        writeFile file: 'report.css',
                  text: libraryResource('gitleaks/render/report.css')

        writeFile file: 'inject.sh',
                  text: libraryResource('gitleaks/render/inject.sh')

        sh 'chmod +x inject.sh'

        int exitCode = failOnLeak ? 1 : 0

        /* ---------------- RUN GITLEAKS ---------------- */
        sh """
          docker run --rm \
            -v ${WORKSPACE}/${repoDir}:/repo \
            -v ${WORKSPACE}/gitleaks:/out \
            zricethezav/gitleaks:v8.30.0 detect \
            --source /repo \
            --log-opts="${logOpts}" \
            --report-format json \
            --report-path /out/gitleaks.json \
            --exit-code ${exitCode}
        """

        /* ---------------- GENERATE FINAL HTML ---------------- */
        sh './inject.sh'

        /* ---------------- PUBLISH REPORT ---------------- */
        if (publishHtml) {
            reports.publish(
                report_dir : "${WORKSPACE}/gitleaks",
                report_file: 'gitleaks_report.html',
                report_name: 'Gitleaks Security Report'
            )
        }
    }
}
