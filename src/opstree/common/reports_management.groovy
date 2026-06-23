package opstree.common

def publish(Map params) {
    publishHTML([allowMissing: true, alwaysLinkToLastBuild: false, keepAll: false, reportDir: "${params.report_dir}", reportFiles: "${params.report_file}", reportName: "${params.report_name}", reportTitles: '', useWrapperFileDirectly: true])
    if (params.dc_publisher == true) {
        dependencyCheckPublisher pattern: '**/dependency-check-report.xml'
    }
}

def publish_static_code_analysis_issues(Map params)  {
    if (params.unit_test_reports_path != null && params.unit_test_reports_path != 'null' && params.unit_test_reports_path != '') {
        junit allowEmptyResults: true, testResults: "${params.unit_test_reports_path}"
    }
    if (params.findbugs_test_report_path != null && params.findbugs_test_report_path != 'null' && params.findbugs_test_report_path != '') {
        recordIssues(tools: [findBugs(pattern: "${params.findbugs_test_report_path}")])
    }
}

def publish_code_coverage(Map params)  {
    recordCoverage(tools: [[parser: 'COBERTURA', pattern: "${params.coverage_report_path}"]])
}
