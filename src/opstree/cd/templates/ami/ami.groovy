// //////////////////////////////////////////////// with all the features ///////////////////////////////////////////////////////////////////

package opstree.cd.templates.ami

import opstree.common.*

/**
 * Helper method to get parameter values, allowing for Jenkins build parameter override.
 *
 * @param enableOverride Boolean indicating if Jenkins build parameters should override.
 * @param step_params Map of parameters passed to the shared library step.
 * @param paramName The name of the parameter to retrieve.
 * @return The resolved parameter value.
 */
def get_params_value(Boolean enableOverride, Map step_params, String paramName) {
    def value = enableOverride && params.containsKey(paramName) ? params[paramName] : step_params[paramName]
    // Special handling for boolean strings from Jenkins parameters
    if (value instanceof String) {
        if (value.equalsIgnoreCase('true')) return true
        if (value.equalsIgnoreCase('false')) return false
    }
    return value
}

/**
 * The main entry point for this shared library step.
 * Performs AMI update on a Launch Template and Auto Scaling Group,
 * with optional rolling update, ASG scaling, health check, and process management,
 * including error handling and notifications.
 *
 * @param step_params A map containing all necessary parameters for the AMI deployment.
 * Expected keys (all are strings unless specified):
 * - enable_jenkins_build_param_override (Boolean, optional): If true, allows Jenkins build params to override.
 * - aws_credential_id (String): Jenkins Credentials ID for AWS Access Key and Secret Key.
 * - region (String): AWS Region for deployment.
 * - launch_template_id (String): The ID of the AWS Launch Template.
 * - new_ami_id (String): The ID of the new AMI to use.
 * - asg_name (String): The name of the Auto Scaling Group.
 *
 * - update_asg_capacity (Boolean, optional): Whether to update ASG min/max/desired capacity.
 * - min_size (Integer, optional): Minimum size of the Auto Scaling group.
 * - max_size (Integer, optional): Maximum size of the Auto Scaling group.
 * - desired_capacity (Integer, optional): Desired capacity of the Auto Scaling group.
 * - default_cooldown (Integer, optional): Cooldown period for simple scaling policies.
 *
 * - update_asg_health_check (Boolean, optional): Whether to update ASG health check settings.
 * - health_check_type (String, optional): Type of health check ('EC2' or 'ELB').
 * - health_check_grace_period (Integer, optional): Health check grace period in seconds.
 *
 * - enable_instance_refresh (Boolean, optional): Enable/disable the instance refresh step.
 * - instance_refresh_strategy (String, optional): Strategy for instance refresh ('Rolling' or 'Checkpoints').
 * - instance_refresh_min_healthy_percentage (Integer, optional): Min healthy percentage for refresh.
 * - instance_refresh_instance_warmup (Integer, optional): Instance warmup time for refresh.
 * - instance_refresh_auto_rollback (Boolean, optional): Auto rollback on refresh failure.
 * - instance_refresh_checkpoints (String, optional): Comma-separated list of checkpoints (e.g., '25,50,75').
 * - instance_refresh_skip_matching (Boolean, optional): Whether to skip instances that already match.
 *
 * - suspend_asg_processes (Boolean, optional): Whether to suspend ASG processes.
 * - processes_to_suspend (List<String>, optional): List of ASG processes to suspend.
 * - resume_asg_processes (Boolean, optional): Whether to resume ASG processes after the main operation.
 * - processes_to_resume (List<String>, optional): List of ASG processes to resume.
 *
 * - enable_instance_protection (Boolean, optional): Enable/disable new instance protection from ASG termination.
 *
 * - clean_workspace (Boolean): Whether to clean workspace after build.
 * - ignore_clean_workspace_failure (Boolean, optional): Ignore errors during workspace cleanup.
 * - delete_dirs (String, optional): Comma-separated list of directories to delete during cleanup.
 * - clean_when_build_aborted (Boolean, optional): Clean workspace on aborted build.
 * - clean_when_build_failed (Boolean, optional): Clean workspace on failed build.
 * - clean_when_not_built (Boolean, optional): Clean workspace if build not run.
 * - clean_when_build_succeed (Boolean, optional): Clean workspace on successful build.
 * - clean_when_build_unstable (Boolean, optional): Clean workspace on unstable build.
 *
 * - notification_enabled (Boolean, optional): Enable/disable notifications.
 * - webhook_url_creds_id (String, conditional): Jenkins Credentials ID for notification webhook URL.
 * - notification_channel (String, conditional): Type of notification channel (e.g., 'slack').
 * - slack_channel (String, conditional): Specific Slack channel name.
 */
def call(Map step_params) {
    ansiColor('xterm') {
        def enableOverride = get_params_value(false, step_params, 'enable_jenkins_build_param_override')?.toBoolean() ?: false
        echo "enableOverride = ${enableOverride}"

        def workspace = new opstree.common.workspace_management()
        def notify = new opstree.common.notify()

        // --- Retrieve Common Parameters ---
        def aws_credential_id = get_params_value(enableOverride, step_params, 'aws_credential_id')
        def region = get_params_value(enableOverride, step_params, 'region')
        def launch_template_id = get_params_value(enableOverride, step_params, 'launch_template_id')
        def new_ami_id = get_params_value(enableOverride, step_params, 'new_ami_id')
        def asg_name = get_params_value(enableOverride, step_params, 'asg_name')

        // --- ASG Scaling Parameters ---
        def update_asg_capacity = get_params_value(enableOverride, step_params, 'update_asg_capacity')?.toBoolean() ?: false
        def min_size = get_params_value(enableOverride, step_params, 'min_size')
        def max_size = get_params_value(enableOverride, step_params, 'max_size')
        def desired_capacity = get_params_value(enableOverride, step_params, 'desired_capacity')
        def default_cooldown = get_params_value(enableOverride, step_params, 'default_cooldown')

        // --- ASG Health Check Parameters ---
        def update_asg_health_check = get_params_value(enableOverride, step_params, 'update_asg_health_check')?.toBoolean() ?: false
        def health_check_type = get_params_value(enableOverride, step_params, 'health_check_type')
        def health_check_grace_period = get_params_value(enableOverride, step_params, 'health_check_grace_period')

        // --- Instance Refresh Parameters ---
        def enable_instance_refresh = get_params_value(enableOverride, step_params, 'enable_instance_refresh')?.toBoolean() ?: false
        def instance_refresh_strategy = get_params_value(enableOverride, step_params, 'instance_refresh_strategy') ?: 'Rolling'
        def instance_refresh_min_healthy_percentage = get_params_value(enableOverride, step_params, 'instance_refresh_min_healthy_percentage')
        def instance_refresh_instance_warmup = get_params_value(enableOverride, step_params, 'instance_refresh_instance_warmup')
        def instance_refresh_auto_rollback = get_params_value(enableOverride, step_params, 'instance_refresh_auto_rollback')?.toBoolean() ?: false
        def instance_refresh_checkpoints = get_params_value(enableOverride, step_params, 'instance_refresh_checkpoints')
        def instance_refresh_skip_matching = get_params_value(enableOverride, step_params, 'instance_refresh_skip_matching')?.toBoolean() ?: false

        // --- ASG Process Management Parameters ---
        def suspend_asg_processes = get_params_value(enableOverride, step_params, 'suspend_asg_processes')?.toBoolean() ?: false
        def processes_to_suspend = get_params_value(enableOverride, step_params, 'processes_to_suspend') as List<String> ?: []
        def resume_asg_processes = get_params_value(enableOverride, step_params, 'resume_asg_processes')?.toBoolean() ?: false
        def processes_to_resume = get_params_value(enableOverride, step_params, 'processes_to_resume') as List<String> ?: []

        // --- Instance Protection Parameter ---
        def enable_instance_protection = get_params_value(enableOverride, step_params, 'enable_instance_protection')?.toBoolean() ?: false

        // Use withEnv for secure credential passing to sh steps
        withCredentials([
            usernamePassword(
                credentialsId: aws_credential_id,
                usernameVariable: 'AWS_ACCESS_KEY',
                passwordVariable: 'AWS_SECRET_KEY'
            )
        ]) {
            try {
                // Suspend ASG processes if configured
                if (suspend_asg_processes && !processes_to_suspend.isEmpty()) {
                    stage("Suspend ASG Processes") {
                        echo "Suspending ASG processes: ${processes_to_suspend.join(', ')}"
                        // Use withEnv for security
                        withEnv(["AWS_ACCESS_KEY_ID=${env.AWS_ACCESS_KEY}", "AWS_SECRET_ACCESS_KEY=${env.AWS_SECRET_KEY}"]) {
                            def suspend_command = "docker run --rm -e AWS_ACCESS_KEY_ID -e AWS_SECRET_ACCESS_KEY amazon/aws-cli autoscaling suspend-processes --auto-scaling-group-name ${asg_name} --region ${region}"
                            if (!processes_to_suspend.isEmpty()) {
                                suspend_command += " --scaling-processes ${processes_to_suspend.join(' ')}"
                            }
                            sh suspend_command
                        }
                    }
                }

                // Update ASG with new launch template version
                stage("Update Launch Template") {
                    // Use withEnv for security
                    withEnv(["AWS_ACCESS_KEY_ID=${env.AWS_ACCESS_KEY}", "AWS_SECRET_ACCESS_KEY=${env.AWS_SECRET_KEY}"]) {
                        sh """
                        docker run --rm -e AWS_ACCESS_KEY_ID -e AWS_SECRET_ACCESS_KEY \\
                          amazon/aws-cli ec2 create-launch-template-version \\
                          --launch-template-id ${launch_template_id} \\
                          --source-version 1 \\
                          --launch-template-data '{"ImageId":"${new_ami_id}"}' \\
                          --region ${region}
                        """
                    }
                }

                // Update ASG configuration (capacity, health check, instance protection)
                stage("Update ASG Configuration") {
                    // Use withEnv for security
                    withEnv(["AWS_ACCESS_KEY_ID=${env.AWS_ACCESS_KEY}", "AWS_SECRET_ACCESS_KEY=${env.AWS_SECRET_KEY}"]) {
                        def update_asg_command = "docker run --rm -e AWS_ACCESS_KEY_ID -e AWS_SECRET_ACCESS_KEY amazon/aws-cli autoscaling update-auto-scaling-group --auto-scaling-group-name ${asg_name} --region ${region} --launch-template \"LaunchTemplateId=${launch_template_id},Version=\\\$Latest\""

                        if (update_asg_capacity) {
                            if (min_size != null) update_asg_command += " --min-size ${min_size}"
                            if (max_size != null) update_asg_command += " --max-size ${max_size}"
                            if (desired_capacity != null) update_asg_command += " --desired-capacity ${desired_capacity}"
                            if (default_cooldown != null) update_asg_command += " --default-cooldown ${default_cooldown}"
                        }

                        if (update_asg_health_check) {
                            if (health_check_type) update_asg_command += " --health-check-type ${health_check_type}"
                            if (health_check_grace_period != null) update_asg_command += " --health-check-grace-period ${health_check_grace_period}"
                        }

                        if (enable_instance_protection) {
                            update_asg_command += " --new-instances-protected-from-scale-in"
                        }

                        sh update_asg_command
                    }
                }

                // Conditionally trigger instance refresh with preferences
                if (enable_instance_refresh) {
                    stage("Trigger Instance Refresh") {
                        // Use withEnv for security
                        withEnv(["AWS_ACCESS_KEY_ID=${env.AWS_ACCESS_KEY}", "AWS_SECRET_ACCESS_KEY=${env.AWS_SECRET_KEY}"]) {
                            def refresh_command = """
                            docker run --rm -e AWS_ACCESS_KEY_ID -e AWS_SECRET_ACCESS_KEY \\
                              amazon/aws-cli autoscaling start-instance-refresh \\
                              --auto-scaling-group-name ${asg_name} \\
                              --strategy ${instance_refresh_strategy} \\
                            """

                            def preferences_json_parts = []
                            if (instance_refresh_min_healthy_percentage != null) {
                                preferences_json_parts << "MinHealthyPercentage=${instance_refresh_min_healthy_percentage}"
                            }
                            if (instance_refresh_instance_warmup != null) {
                                preferences_json_parts << "InstanceWarmup=${instance_refresh_instance_warmup}"
                            }
                            if (instance_refresh_skip_matching) {
                                preferences_json_parts << "SkipMatching=true"
                            }
                            if (instance_refresh_checkpoints != null && instance_refresh_strategy == 'Checkpoints') {
                                preferences_json_parts << "Checkpoints=[${instance_refresh_checkpoints}]"
                            }

                            // Crucial Fix: Add DesiredConfiguration when AutoRollback is true
                            if (instance_refresh_auto_rollback) {
                                preferences_json_parts << "AutoRollback=true"
                                // Specify DesiredConfiguration which matches the ASG's current desired LT
                                refresh_command += "--desired-configuration '{\"LaunchTemplate\":{\"LaunchTemplateId\":\"${launch_template_id}\",\"Version\":\"\$Latest\"}}' \\"
                            }

                            if (preferences_json_parts) {
                                refresh_command += "--preferences \"${preferences_json_parts.join(',')}\" \\"
                            }

                            refresh_command += "--region ${region}"

                            sh refresh_command
                        }
                    }

                    // Monitor the instance refresh status
                    stage("Monitor Instance Refresh") {
                        timeout(time: 60, unit: 'MINUTES') { // Increased timeout for potentially longer refreshes
                            // Use withEnv for security
                            withEnv(["AWS_ACCESS_KEY_ID=${env.AWS_ACCESS_KEY}", "AWS_SECRET_ACCESS_KEY=${env.AWS_SECRET_KEY}"]) {
                                waitUntil {
                                    def statusOutput = sh(script: """
                                    docker run --rm -e AWS_ACCESS_KEY_ID -e AWS_SECRET_ACCESS_KEY \\
                                      amazon/aws-cli autoscaling describe-instance-refreshes \\
                                      --auto-scaling-group-name ${asg_name} \\
                                      --query 'InstanceRefreshes[0].Status' \\
                                      --output text \\
                                      --region ${region}
                                    """, returnStdout: true).trim()

                                    echo "Instance Refresh Status: ${statusOutput}"

                                    if (statusOutput == 'Successful' || statusOutput == 'RollbackSuccessful') {
                                        echo "Instance refresh completed successfully or rolled back successfully."
                                        return true
                                    } else if (statusOutput == 'Failed' || statusOutput == 'Cancelled' || statusOutput == 'RollbackFailed') {
                                        error "Instance refresh ${statusOutput} for ASG ${asg_name}."
                                    }
                                    return false
                                }
                            }
                        }
                    }
                }

            } catch (Exception e) {
                currentBuild.result = 'FAILURE'
                echo "Caught exception: ${e.message}"
                // Attempt to resume processes even on failure, if configured
                if (resume_asg_processes && !processes_to_resume.isEmpty()) {
                    try {
                        stage("Resume ASG Processes on Failure") {
                            echo "Attempting to resume ASG processes on failure: ${processes_to_resume.join(', ')}"
                            // Use withEnv for security
                            withEnv(["AWS_ACCESS_KEY_ID=${env.AWS_ACCESS_KEY}", "AWS_SECRET_ACCESS_KEY=${env.AWS_SECRET_KEY}"]) {
                                def resume_command = "docker run --rm -e AWS_ACCESS_KEY_ID -e AWS_SECRET_ACCESS_KEY amazon/aws-cli autoscaling resume-processes --auto-scaling-group-name ${asg_name} --region ${region}"
                                if (!processes_to_resume.isEmpty()) {
                                    resume_command += " --scaling-processes ${processes_to_resume.join(' ')}"
                                }
                                sh resume_command
                            }
                        }
                    } catch (innerException) {
                        echo "Failed to resume ASG processes on failure: ${innerException.message}"
                    }
                }
                if (step_params.notification_enabled?.toBoolean()) {
                    notify.notification_factory(
                        build_status: 'FAILURE',
                        webhook_url_creds_id: get_params_value(enableOverride, step_params, 'webhook_url_creds_id'),
                        notification_channel: get_params_value(enableOverride, step_params, 'notification_channel'),
                        notification_enabled: get_params_value(enableOverride, step_params, 'notification_enabled'),
                        slack_channel: get_params_value(enableOverride, step_params, 'slack_channel')
                    )
                }
                throw e // Re-throw the original exception to fail the pipeline
            } finally {
                // Always attempt to resume processes in finally, unless already done in catch
                // (To avoid double resume if catch block already handled it)
                if (resume_asg_processes && !processes_to_resume.isEmpty() && currentBuild.result != 'FAILURE') { // Only if not already attempted in catch
                    try {
                        stage("Resume ASG Processes in Finally") {
                            echo "Resuming ASG processes in finally block: ${processes_to_resume.join(', ')}"
                            // Use withEnv for security
                            withEnv(["AWS_ACCESS_KEY_ID=${env.AWS_ACCESS_KEY}", "AWS_SECRET_ACCESS_KEY=${env.AWS_SECRET_KEY}"]) {
                                def resume_command = "docker run --rm -e AWS_ACCESS_KEY_ID -e AWS_SECRET_ACCESS_KEY amazon/aws-cli autoscaling resume-processes --auto-scaling-group-name ${asg_name} --region ${region}"
                                if (!processes_to_resume.isEmpty()) {
                                    resume_command += " --scaling-processes ${processes_to_resume.join(' ')}"
                                }
                                sh resume_command
                            }
                        }
                    } catch (innerException) {
                        echo "Failed to resume ASG processes in finally block: ${innerException.message}"
                    }
                }

                if (step_params.notification_enabled?.toBoolean()) {
                    if (currentBuild.currentResult == 'SUCCESS') {
                        notify.notification_factory(
                            build_status: 'SUCCESS',
                            webhook_url_creds_id: get_params_value(enableOverride, step_params, 'webhook_url_creds_id'),
                            notification_channel: get_params_value(enableOverride, step_params, 'notification_channel'),
                            notification_enabled: get_params_value(enableOverride, step_params, 'notification_enabled'),
                            slack_channel: get_params_value(enableOverride, step_params, 'slack_channel')
                        )
                    }
                }

                if (step_params.clean_workspace?.toBoolean()) {
                    workspace.workspace_management(
                        clean_workspace: get_params_value(enableOverride, step_params, 'clean_workspace'),
                        ignore_clean_workspace_failure: get_params_value(enableOverride, step_params, 'ignore_clean_workspace_failure'),
                        delete_dirs: get_params_value(enableOverride, step_params, 'delete_dirs'),
                        clean_when_build_aborted: get_params_value(enableOverride, step_params, 'clean_when_build_aborted'),
                        clean_when_build_failed: get_params_value(enableOverride, step_params, 'clean_when_build_failed'),
                        clean_when_not_built: get_params_value(enableOverride, step_params, 'clean_when_not_built'),
                        clean_when_build_succeed: get_params_value(enableOverride, step_params, 'clean_when_build_succeed'),
                        clean_when_build_unstable: get_params_value(enableOverride, step_params, 'clean_when_build_unstable')
                    )
                }
            }
        }
    }
}


//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


