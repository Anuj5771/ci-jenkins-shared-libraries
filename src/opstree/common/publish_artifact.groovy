package opstree.common

def publish_factory(Map step_params) {
    logger = new logger()

    if (step_params.artifact_publish_check == true) {
        publish_artifact(step_params)
    } else {
        logger.logger('msg': 'No valid option selected for Publishing Artifact. Please mention correct values.', 'level': 'WARN')
    }
}

def publish_artifact(Map step_params) {
    logger = new logger()
    parser = new parser()

    logger.logger('msg':'Performing Publish Artifact Step', 'level':'INFO')

    try {
        if (step_params.artifact_destination_type == 'S3') {
            artifact_s3_bucket_aws_region = "${step_params.artifact_s3_bucket_aws_region}"
            jenkins_aws_credentials_id = "${step_params.jenkins_aws_credentials_id}"
            artifact_s3_bucket_name = "${step_params.artifact_s3_bucket_name}"
            artifact_source_path = "${step_params.artifact_source_path}"
            artifact_s3_keypath_destination = "${step_params.artifact_s3_keypath_destination}"
            env = "${step_params.env}"
            app_name = "${step_params.app_name}"

            artifact_extension = "${artifact_source_path.tokenize('/.').last()}"

            withAWS(credentials: jenkins_aws_credentials_id, region: artifact_s3_bucket_aws_region) {
                s3Upload(file:"${artifact_source_path}", bucket:"${artifact_s3_bucket_name}", path:"${artifact_s3_keypath_destination}/${env}_${app_name}_${BUILD_NUMBER}.${artifact_extension}")
                s3Upload(file:"${artifact_source_path}", bucket:"${artifact_s3_bucket_name}", path:"${artifact_s3_keypath_destination}/${env}_${app_name}_latest.${artifact_extension}")
                logger.logger('msg':'Uploaded Artifact successfully in S3 bucket', 'level':'INFO')
            }
        }
        else if (step_params.artifact_destination_type == 'ecr') {
            jenkins_aws_credentials_id = "${step_params.jenkins_aws_credentials_id}"
            docker_image_name = "${step_params.docker_image_name}"
            ecr_repo_name = "${step_params.ecr_repo_name}"
            ecr_region = "${step_params.ecr_region}"
            repo_url = "${step_params.repo_url}"
            repo_dir = parser.fetch_git_repo_name('repo_url':"${repo_url}")
            account_id = "${step_params.account_id}"

            def docker_image_tag = sh(
                            script: """git config --global --add safe.directory ${WORKSPACE}/${repo_dir} && \
                                       cd ${WORKSPACE}/${repo_dir} && git rev-parse --short HEAD""",
                            returnStdout: true
                        ).trim()

            withAWS() {
                def imageExists = false
                // Check if the image already exists in ECR
                def ecrImages = sh(
                    script: "aws ecr describe-images --repository-name ${ecr_repo_name} --region ${ecr_region} --query 'imageDetails[].imageTags' --output text",
                    returnStdout: true
                ).trim()

                if (ecrImages.contains(docker_image_tag)) {
                    imageExists = true
                    echo "Image with tag '${docker_image_tag}' already exists in the repository '${ecr_repo_name}'."
                }

                if (!imageExists) {
                    sh """
                    echo \${AWS_ACCESS_KEY_ID}
                    docker tag $docker_image_name:$docker_image_tag ${account_id}.dkr.ecr.${ecr_region}.amazonaws.com/$ecr_repo_name:$docker_image_tag
                    docker run --rm \\
                        -e AWS_REGION=$ecr_region \\
                        -e AWS_ACCESS_KEY_ID=\${AWS_ACCESS_KEY_ID} \\
                        -e AWS_SECRET_ACCESS_KEY=\${AWS_SECRET_ACCESS_KEY} \\
                        -v /var/run/docker.sock:/var/run/docker.sock \\
                        amazon/aws-cli \\
                        ecr get-login-password --region $ecr_region | docker login --username AWS --password-stdin ${account_id}.dkr.ecr.${ecr_region}.amazonaws.com && docker push ${account_id}.dkr.ecr.${ecr_region}.amazonaws.com/$ecr_repo_name:$docker_image_tag
                            """
                    logger.logger('msg':'Uploaded Image successfully in ECR Repo', 'level':'INFO')
                    logger.logger('msg':'Removing Docker images from local', 'level':'INFO')
                    sh """
                    docker rmi -f $docker_image_name:$docker_image_tag ${account_id}.dkr.ecr.${ecr_region}.amazonaws.com/$ecr_repo_name:$docker_image_tag
                """
                    logger.logger('msg':'Removed Docker images from local', 'level':'INFO')
                }
            }
        }
        else if (step_params.artifact_destination_type == 'harbor') {
            harbor_url = "${step_params.harbor_url}"
            harbor_project = "${step_params.harbor_project}"
            harbor_credentials_id = "${step_params.harbor_credentials_id}"
            docker_image_name = "${step_params.docker_image_name}"
            repo_url = "${step_params.repo_url}"
            repo_dir = parser.fetch_git_repo_name('repo_url':"${repo_url}")

            def docker_image_tag = sh(
                            script: """git config --global --add safe.directory ${WORKSPACE}/${repo_dir} && \
                                       cd ${WORKSPACE}/${repo_dir} && git rev-parse --short HEAD""",
                            returnStdout: true
                        ).trim()

            withCredentials([usernamePassword(credentialsId: harbor_credentials_id,
                                           usernameVariable: 'HARBOR_USER',
                                           passwordVariable: 'HARBOR_PASSWORD')]) {
                // Login to Harbor
                sh """
                  echo "\$HARBOR_PASSWORD" | docker login -u "\$HARBOR_USER" --password-stdin ${harbor_url}
                   """

                // Tag and push the image
                sh """
                    docker tag $docker_image_name:$docker_image_tag ${harbor_url}/${harbor_project}/$docker_image_name:$docker_image_tag
                    docker push ${harbor_url}/${harbor_project}/$docker_image_name:$docker_image_tag
                """

                logger.logger('msg':'Uploaded Image successfully to Harbor registry', 'level':'INFO')

                // Clean up local images
                logger.logger('msg':'Removing Docker images from local', 'level':'INFO')
                sh """
                    docker rmi -f $docker_image_name:$docker_image_tag ${harbor_url}/${harbor_project}/$docker_image_name:$docker_image_tag
                    docker logout ${harbor_url}
                """
                logger.logger('msg':'Removed Docker images from local', 'level':'INFO')
                                           }
        }
        else {
            logger.logger('msg':'Choose appropriate publish destination (S3, ECR, or Harbor)!', 'level':'ERROR')
            error("Invalid artifact destination type: ${step_params.artifact_destination_type}")
        }
    } catch (Exception e) {
        logger.logger('msg':"Publish Failed Error Details: ${e}", 'level':'ERROR')
        error("Publish artifact failed: ${e.getMessage()}")
    }
}
