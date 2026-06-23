package opstree.common

import opstree.common.build_packer_ami

import opstree.common.*

def packer_ami_build(Map step_params) {
    logger = new logger()
    parser = new parser()

    logger.logger('msg':'Performing Packer AMI Build', 'level':'INFO')

    repo_url = "${step_params.repo_url}"
    packer_ami_template_file = "${step_params.packer_ami_template_file}"
    packer_var_file = "${step_params.packer_var_file}"
    packer_ami_region = "${step_params.packer_ami_region}"
    source_code_path = "${step_params.source_code_path}"
    jenkins_aws_credentials_id = "${step_params.jenkins_aws_credentials_id}"

    def repo_dir = parser.fetch_git_repo_name('repo_url':"${repo_url}")
    repo_dir = repo_dir + source_code_path
    sh "echo ${repo_dir}"

    dir("${WORKSPACE}/${repo_dir}") {
            withAWS(credentials: jenkins_aws_credentials_id, region: packer_ami_region) {
            dir("${WORKSPACE}/${repo_dir}") {
                logger.logger(msg: " Starting Packer AMI build using: ${packer_ami_template_file}", level: 'INFO')

                sh """
                mkdir -p /var/lib/jenkins/packer_plugins
                docker run --rm \\
                    -e AWS_REGION=${packer_ami_region} \\
                    -e AWS_ACCESS_KEY_ID=\${AWS_ACCESS_KEY_ID} \\
                    -e AWS_SECRET_ACCESS_KEY=\${AWS_SECRET_ACCESS_KEY} \\
                    -e PACKER_PLUGIN_PATH=/workspace/.packer.d/plugins \\
                    -v /var/run/docker.sock:/var/run/docker.sock \\
                    -v $WORKSPACE/${repo_dir}:/workspace -w /workspace \\
                    -v /var/lib/jenkins/packer_plugins:/workspace/.packer.d/plugins \\
                    hashicorp/packer:1.13 \\
                    init .
            """

                sh """
                docker run --rm \\
                    -e AWS_REGION=${packer_ami_region} \\
                    -e AWS_ACCESS_KEY_ID=\${AWS_ACCESS_KEY_ID} \\
                    -e AWS_SECRET_ACCESS_KEY=\${AWS_SECRET_ACCESS_KEY} \\
                    -e PACKER_PLUGIN_PATH=/workspace/.packer.d/plugins \\
                    -v /var/run/docker.sock:/var/run/docker.sock \\
                    -v $WORKSPACE/${repo_dir}:/workspace -w /workspace \\
                    -v /var/lib/jenkins/packer_plugins:/workspace/.packer.d/plugins \\
                    hashicorp/packer:1.13 \\
                    build -var-file=${packer_var_file} ${packer_ami_template_file}
            """
                logger.logger(msg: 'Packer AMI build completed successfully', level: 'INFO')
            }
            }
    }
}

