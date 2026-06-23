#  Jenkins Shared Library - PACKER AMI

This Jenkins Shared Library simplifies CI/CD pipeline development by providing reusable components for:

-  Git operations
-  Packer AMI builds
-  Workspace management
-  Slack/Teams/gmail notifications
-  CD pipeline triggering

---

##  Key Components

| Module                   | Path                                          | Purpose                             |
|--------------------------|-----------------------------------------------|-------------------------------------|
| `packer_ami_build`       | `src/opstree/ci/templates/packer/`            | End-to-end Packer AMI pipeline      |
| `git_management`         | `src/opstree/common/git_management.groovy`    | Git checkout logic                  |
| `build_packer_ami`       | `src/opstree/common/build_packer_ami.groovy`  | Packer AMI build executor           |
| `workspace_management`   | `src/opstree/common/workspace_management.groovy` | Workspace cleanup utility        |
| `notify`                 | `src/opstree/common/notify.groovy`            | Slack / Teams /gmail notification handler  |

---

##  Example Usage (`Jenkinsfile`)

```groovy
@Library('cicd_accelerator') _
def packerpipeline = new opstree.ci.templates.packer.packer_ami_build()

node {
  packerpipeline.call([
    // Workspace cleanup
    clean_workspace: true,
    ignore_clean_workspace_failure: false,
    delete_dirs: false,
    clean_when_build_aborted: true, 
    clean_when_build_failed: true,
    clean_when_not_built: true,
    clean_when_build_succeed: true,
    clean_when_build_unstable: true,

    // Git configuration
    repo_https_url: "https://github.com/ot-central-team/ci-jenkins-wrapper-code.git",
    repo_ssh_url: "git@github.com:ot-central-team/ci-jenkins-wrapper-code.git",
    repo_branch: "packer",
    repo_url_type: "http",
    jenkins_git_creds_id: "neelesh-git-creds",
    source_code_path: "/samples/packer_scripts",

    // Packer configuration
    jenkins_aws_credentials_id: "neelesh-aws-creds",
    packer_ami_template_file: "templates/base-ubuntu.pkr.hcl",
    packer_var_file: "variables/dev.pkrvars.hcl",
    packer_ami_region: "us-east-1",

    // Notification 
    notification_enabled: true,
    notification_channel: "teams",   // or 'slack'
    webhook_url_creds_id: "teams_webhook"
  ])
}
