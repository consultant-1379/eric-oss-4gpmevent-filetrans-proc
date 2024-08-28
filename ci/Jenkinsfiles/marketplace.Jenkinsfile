def bob = "./bob/bob"
def local_ruleset = "ci/local_ruleset.yaml"
def ci_ruleset = "ci/common_ruleset2.0.yaml"

if(!env.RELEASE){
stage('Custom Upload Marketplace Documentation') {
                  withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS'),
    string(credentialsId: "MARKETPLACE_4GPM_TOKEN", variable: 'MARKETPLACE_4GPM_TOKEN')]) {
                      echo "Marketplace upload-dev"
                      sh "${bob} -r ${local_ruleset} marketplace-upload-dev || true"
                  }
          }
}
if(env.RELEASE){
stage('Custom Upload Marketplace Documentation') {
                  withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS'),
    string(credentialsId: "MARKETPLACE_4GPM_TOKEN", variable: 'MARKETPLACE_4GPM_TOKEN')]) {
                      echo "Marketplace upload"
                      sh "${bob} -r ${local_ruleset} marketplace-upload-release || true"
                  }
          }
}
if (env.RELEASE) {
stage('Custom Publish') {
    withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS'),
    file(credentialsId: 'docker-config-json', variable: 'DOCKER_CONFIG_JSON')
    ]) {
        ci_pipeline_scripts.checkDockerConfig()
        sh "${bob} -r ${ci_ruleset} upload-mvn-jars"
        ci_pipeline_scripts.retryMechanism("${bob} -r ${local_ruleset} publish", 3)
        sh "${bob} -r ${local_ruleset} publish-md-oas"
    }
}
}