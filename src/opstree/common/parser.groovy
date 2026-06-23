package opstree.common

def fetch_git_repo_name(Map params) {
    // Split the URL by '/', take the last segment (the repo name with or without .git)
    def repoPart = params.repo_url.tokenize('/').last()

    // Check if the repo name ends with .git and remove it; otherwise, return the full repo name
    return repoPart.endsWith('.git') ? repoPart[0..-5] : repoPart
}
