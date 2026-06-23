# ci-jenkins-shared-libraries
ci-jenkins-shared-libraries


## Description

This README file provides information about the Jenkinsfile used for the CI pipeline with the 'ot-central-ci' library.

# DEPENDENCIES

# Plugins

### Generic Plugins

    Ansicolor: 1.0.2
    Credentials Binding Plugin: 604.vb_64480b_c56ca_
    Pipeline Utility Steps: 2.15.4
    Work space cleanup plugin: 0.45
    HTML Publisher Plugin: 1.31
    Warnings next generation plugin: 10.2.0

### Java Specific Plugins

    Pipeline Maven Integration: 1298.v43b_82f220a_e9
    pipeline aws steps: 1.43





## Software

### Generic Tool

    Docker: 23.0.5

### Java Specific Tool

    Maven: 3.6.3

## Usage
---

    @Library('ot-central-ci') _
    def cipipeline = new opstree.ci.templates.java_ci.java_ci()
    node {
    cipipeline.call([

        // WORKSPACE MANAGEMENT
        clean_workspace: true,
        ignore_clean_workspace_failure:  false,
        delete_dirs: true,
        clean_when_build_aborted: true,
        clean_when_build_failed: true,
        clean_when_not_built: true,
        clean_when_build_succeed: true,
        clean_when_build_unstable: true,

        // VCS MANAGEMENT
        repo_https_url: "https://gitlab.com/ot-client/central-team/pods/ci/wrapper_code.git",
        repo_ssh_url: "git@gitlab.com:ot-client/central-team/pods/ci/wrapper_code.git",
        branch: "ci_testing",
        repo_url_type: "ssh",
        //ssh_private_key_location: "/var/lib/jenkins/test"
        //jenkins_git_creds_id: "sudheerpathakapitoken"
        jenkins_git_ssh_key_id:"jenkins-ssh"
    ])
    }







---



# Inputs
---
### TABLE FOR WORKSPACE MANAGEMENT

| Name | Description | Type | Default |Required |
|-----------------|-----------------|-----------------|-----------------|-----------------|
| clean_workspace | This option determines whether the workspace directory should be cleaned before the build starts. | Boolean |  | yes |
|ignore_clean_workspace_failure  | It determines whether the pipeline should continue executing even if the workspace cleaning operation fails. | Boolean | false | no |
|delete_dirs  |It controls whether directories should be deleted before the build process starts.    | Boolean  | true  | no |
|clean_when_build_aborted   |It determines whether the workspace should be cleaned when the build is aborted.   | Boolean | true |no |
|clean_when_build_failed|It determines whether the workspace should be cleaned when the build fails in a Jenkins pipeline.|Boolean| true | no |
|clean_when_build_succeed|It determines whether the workspace should be cleaned when the build succeeds.| Boolean | true | no |
|clean_when_build_unstable|It determines whether the workspace should be cleaned when the build is unstable.| Boolean  | true | no |

---




### TABLE FOR VCS MANAGEMENT



| Name | Description | Type | Default |Required |
|-----------------|-----------------|-----------------|-----------------|-----------------|
|repo_https_url|HTTPS URL of the repository |String  |  | yes |
|repo_ssh_url|ssh URL of the repository |String |  | yes |
|branch|Name of branch|String|  |yes|
|repo_url_type|It define the type of URL used |String  |  | yes |
|ssh_private_key_location |It specifies the location or path of the SSH private key file |String  |  | yes |
|jenkins_git_creds_id |It refers to the ID or name of the Jenkins credentials |String  |  | yes |
|jenkins_git_ssh_key_id |It represents the ID of the Jenkins SSH key credentials |String  |  | yes |






### Table for Creds Scanning


| Name | Description | Type | Default |Required |
|-----------------|-----------------|-----------------|-----------------|-----------------|
|gitleaks_check |performs a security scan |Boolean |   |yes|
|fail_job_if_leak_detected |determines whether the job should fail if any leaks are detected during the gitleaks check |Boolean |   |  yes |
|gitleaks_report_format |Specifies the format of the report generated |String |    |yes |
|gitleaks_report_jenkins_publish|It is responsible for publishing the report|Boolean  |    | yes|

### Table for Dependency Scanning


| Name | Description | Type | Default |Required |
|-----------------|-----------------|-----------------|-----------------|-----------------|
|dependency_check |It performs a dependency analysis or checks for vulnerabilities in project dependencies | Boolean |    |yes|
|dependency_scan_tool |It represents the name or identifier of the dependency scanning tool |String |    | yes|
|owasp_project_name |name of the OWASP project |String |   |yes |
|owasp_report_publish |generate a report based on the results of the OWASP security scanning process |String |   |yes |
|owasp_report_format|It specifies the format of the OWASP |String |   |yes |
|fail_job_if_dependency_returned_exception |It specifies whether the job should fail if a dependency job returns an exception |String |   |yes |

### Table for Security Vulnerablity Scanning

| Name | Description | Type | Default |Required |
|-----------------|-----------------|-----------------|-----------------|-----------------|
|snyk_security_check|automatically scan your code and dependencies for known vulnerabilities and security issues|Boolean|  |yes |
|language_codebase_for_snyk_check|Refers to the Java codebase being scanned for vulnerabilities using Snyk|String|  |yes |
|snyk_report_format|It refers to the specific format in which vulnerability reports are generated |String|  | yes |
|fail_job_if_snyk_security_returned_exception|It sets the behavior when Snyk encounters an exception|Boolean|  | yes  |
|snyk_api_creds_id|It grants access to the Snyk API |String |  | yes |

### Table for Unit Testing

| Name | Description | Type | Default |Required |
|-----------------|-----------------|-----------------|-----------------|-----------------|
|unit_testing_check|It ensures that unit tests are automatically executed whenever code changes are pushed or merged|Boolean|  | yes  |
| fail_job_if_unit_issue_detected|Automated job failure triggered if any unit-related errors are detected during execution|Boolean |  |yes|
| build_tool|Namme of build tool| String |   | yes|
|unit_test_reports_path| file path pattern used to locate XML test reports|String |  |yes|
|findbugs_test_report_path| It specifies the path for the FindBugs test report files |String |  |yes |
 |withmaven_globaltool_jdk|It represents configuring the Jenkins pipeline to use the global Maven tool installation and a specific JDK version|String|  |yes |
 | withmaven_globaltool_maven|Specify the global Maven tool to be used within a specific stage or block in the Jenkins Pipeline |String|   | yes|

### Table for Static Code Analysis

| Name | Description | Type | Default |Required |
|-----------------|-----------------|-----------------|-----------------|-----------------|
|codebase_to_scan_directory|It refers to the directory or path that you want to scan for code files|String|  |yes   |
|static_code_analysis_check|Enables static code analysis checks for the codebase|Boolean |  | yes |
path_to_sonar_properties |It refers to the file path of the SonarQube configuration file |String |  |yes |
|fail_job_if_analysis_returned_exception|Specifies that the job should fail if the static code analysis returned an exception|Boolean |   |yes |
|jenkins_sonarqube_token_creds_id |It specifies the Jenkins credential ID for the SonarQube token|String |  |yes |

### Table for Build


| Name | Description | Type | Default |Required |
|-----------------|-----------------|-----------------|-----------------|-----------------|
|perform_build |It executes the build process to compile and package the codebase| Boolean |   | yes


### Table for Publish
| Name | Description | Type | Default |Required |
|-----------------|-----------------|-----------------|-----------------|-----------------|
|artifact_publish_check | It enables publishing of artifacts |Boolean |  |  yes |
|artifact_destination_type |It specifies the type of destination for the artifact | String |  |yes |
|artifact_s3_bucket_aws_region |It specifies the AWS region for the S3 bucket used for storing artifacts |String |  | yes |
|jenkins_aws_credentials_id| It specifies the Jenkins AWS credentials ID to be used for AWS-related operations |String   | | yes |
|artifact_s3_bucket_name|It specifies the name of the S3 bucket used for storing artifacts |String|  |yes |
|artifact_source_path |It specifies the path to the artifact source file | String |  | yes |
|artifact_s3_keypath_destination |It specifies the S3 key path destination for the artifact | String | |yes |
|env |Name of the environment |String |    | yes |
|app_name |Name of application |String |   | yes |


### Notification (Slack)

| Name                   | Description                                   | Type    | Required |
|------------------------|-----------------------------------------------|---------|----------|
| notification_enabled   | Enable notifications                          | Boolean | yes      |
| webhook_url_creds_id   | Jenkins credential ID for the Slack webhook   | String  | yes      |
| notification_channel   | Notification platform (e.g., Slack)           | String  | yes      |
| slack_channel          | Slack channel to post messages                | String  | yes      |



### Deployment (AWS Elastic Beanstalk)

| Name                      | Description                                       | Type    | Required |
|---------------------------|---------------------------------------------------|---------|----------|
| deployment_env            | Deployment target (e.g., Elastic Beanstalk)       | String  | yes      |
| eb_aws_creds_id           | Jenkins AWS credentials                           | String  | yes      |
| eb_awsRegion              | AWS region                                        | String  | yes      |
| eb_applicationName        | Beanstalk application name                        | String  | yes      |
| eb_environmentName        | Beanstalk environment name                        | String  | yes      |
| eb_bucketName             | S3 bucket name for deployment                     | String  | yes      |
| eb_keyPrefix              | S3 path prefix                                    | String  | yes      |
| eb_rootObject             | Root object for deployment                        | String  | yes      |
| eb_zeroDowntime           | Perform zero-downtime deployment                  | Boolean | no       |
| eb_sleepTime              | Sleep time between health checks                  | Int     | no       |
| eb_checkHealth            | Check the Beanstalk environment health            | Boolean | no       |
| eb_maxAttempts            | Maximum retries for the deployment check          | Int     | no       |
| eb_skipEnvironmentUpdates | Skip environment updates before deployment        | Boolean | no       |

---
