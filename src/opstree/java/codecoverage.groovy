package opstree.java

import opstree.java.codecoverage

import opstree.common.*

def code_coverage_factory(Map step_params) {
    logger = new logger()
    if (step_params.perform_code_coverage == 'true') {
        code_coverage(step_params)
    } else {
        logger.logger('msg':'No valid option selected for code coverage. Please mention correct values.', 'level':'WARN')
    }
}

def code_coverage(Map step_params) {
    logger = new logger()
    parser = new parser()

    logger.logger('msg':'Performing Code coverage Step', 'level':'INFO')

    repo_url = "${step_params.repo_url}"
    repo_url_type = "${step_params.repo_url_type}"

    code_coverage_test = "${step_params.code_coverage_test}"
    build_tool = "${step_params.build_tool}"
    withmaven_globaltool_jdk = "${step_params.withmaven_globaltool_jdk}"
    withmaven_globaltool_maven = "${step_params.withmaven_globaltool_maven}"
    source_code_path = "${step_params.source_code_path}"

    repo_dir = parser.fetch_git_repo_name('repo_url':"${repo_url}")
    repo_dir = repo_dir + source_code_path

    try {
        dir("${WORKSPACE}/${repo_dir}") {
            if (build_tool == 'maven') {
                withMaven(globalMavenSettingsConfig: '', jdk: "${withmaven_globaltool_jdk}", maven: "${withmaven_globaltool_maven}", mavenSettingsConfig: '') {
                    // Construct Docker image tag based on Maven version and Java version
                    docker_image = 'maven:3.8.6-jdk-11'

                    // Run Maven clean package and test inside Docker
                    sh """
                        docker run --rm \
                            -v ${WORKSPACE}/${repo_dir}:/app \
                            -v ${WORKSPACE}/${repo_dir}/target:/app/target \
                            -w /app \
                            ${docker_image} \
                            sh -c "mvn test jacoco:report"
                    """
                    logger.logger('msg':'Code coverage successful', 'level':'INFO')
                }
            } else {
                logger.logger('msg':'Choose appropriate Coverage tool !!! TEST Failed Error Details: Invalid Code overage tool specified.', 'level':'ERROR')
                error('Invalid Code overage tool specified.')
            }
        }
    } catch (Exception e) {
        logger.logger('msg':"Code Coverage Failed Error Details: ${e.message}", 'level':'ERROR')
        error('Build process failed due to an exception.')
    }
}
