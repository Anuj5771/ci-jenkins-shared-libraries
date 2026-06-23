package opstree.common

import opstree.common.argocd_deployment

import opstree.common.*

def deployment_factory(Map step_params) {
    logger = new logger()
    if (step_params.perform_argocd_deployment == 'true') {
        eks_deployment(step_params)
    }
  else {
        logger.logger('msg':"${step_params.perform_argocd_deployment} No valid option selected for Deplying application via Argocd. Please mention correct values.", 'level':'WARN')
  }
}

def eks_deployment(Map step_params) {
    logger = new logger()
    parser = new parser()

    logger.logger('msg':'Performing Docker Build Step', 'level':'INFO')

    app_name = "${step_params.app_name}"
    repo_url = "${step_params.repo_url}"
    deployment_strategy = "${step_params.deployment_strategy}"
    argocd_credential_id = "${step_params.argocd_credential_id}"
    argocd_server_env_name = "${step_params.argocd_server_env_name}"
    argocd_server_url = env[argocd_server_env_name]
    eks_api_endpoint_env_name = "${step_params.eks_api_endpoint_env_name}"
    prune_post_deployment = "${step_params.prune_post_deployment}"
    eks_api_endpoint = env[eks_api_endpoint_env_name]
    helm_chart_path = "${step_params.helm_chart_path}"
    values_file_path = "${step_params.values_file_path}"
    source_code_path = "${step_params.source_code_path}"
    prune_post_deployment = "${step_params.prune_post_deployment}"
    repo_dir = parser.fetch_git_repo_name('repo_url':"${repo_url}")
    repo_dir = repo_dir + source_code_path
    repo_branch = "${step_params.repo_branch}"

    dir("${WORKSPACE}/${repo_dir}") {
        withCredentials([string(credentialsId: argocd_credential_id, variable: 'PASSWORD')]) {
            logger.logger('msg':'Login into ArgoCD', 'level':'INFO')
            sh "argocd login ${argocd_server_url} --username admin --password $PASSWORD --insecure"

            logger.logger('msg':'Creating or updating ArgoCD application...', 'level':'INFO')
            if (params.image_tag) {
                sh """
                #!/bin/bash
                argocd app create $app_name \
                    --repo $repo_url \
                    --path $helm_chart_path \
                    --values ${values_file_path} \
                    --dest-server ${eks_api_endpoint} \
                    --revision ${repo_branch} \
                    --sync-policy automated \
                    --upsert \
                    --helm-set image.tag=${params.image_tag}

                sleep 10
                if [ "$prune_post_deployment" = true ]; then
                    argocd app sync $app_name --prune
                else
                    argocd app sync $app_name
                fi
            """
            }
            else
            {
                sh """
                #!/bin/bash
                argocd app create $app_name \
                    --repo $repo_url \
                    --path $helm_chart_path \
                    --values ${values_file_path} \
                    --dest-server ${eks_api_endpoint} \
                    --revision ${repo_branch} \
                    --sync-policy automated \
                    --upsert

                sleep 10

                if [ "$prune_post_deployment" = "true" ]; then
                   argocd app sync $app_name --prune
                else
                   argocd app sync $app_name
                fi
            """
            }
            logger.logger('msg':'Created or updated ArgoCD application...', 'level':'INFO')
            logger.logger('msg':'Checking Application Health check. Please wait for sometime...', 'level':'INFO')
            sh """
                    #!/bin/bash
                    MAX_RETRIES=5
                    RETRY_INTERVAL=30 # in seconds
                    RETRIES=0

                    while [ \$RETRIES -lt \$MAX_RETRIES ]; do
                        APP_STATUS=\$(argocd app get ${app_name} --output json | jq -r '.status.health.status')

                        echo "Application health status: \$APP_STATUS"

                        if [ "\$APP_STATUS" = "Healthy" ]; then
                            echo "Application is Healthy."
                            exit 0
                        else
                            echo "Application is not Healthy yet. Retrying in \${RETRY_INTERVAL} seconds..."
                            sleep \${RETRY_INTERVAL}
                            RETRIES=\$((RETRIES + 1))
                        fi
                    done

                    echo "Application did not become healthy within the expected time."
                    exit 1
                    """

            logger.logger('msg':'Application Deployed successfully', 'level':'INFO')
        }
    }
}
