def bob = "./bob/bob"
def local_ruleset = "ci/local_ruleset.yaml"
def ci_ruleset = "ci/common_ruleset2.0.yaml"
stage('Custom Generate Docs') {
                  withCredentials([usernamePassword(credentialsId: 'SELI_ARTIFACTORY', usernameVariable: 'SELI_ARTIFACTORY_REPO_USER', passwordVariable: 'SELI_ARTIFACTORY_REPO_PASS'),
    file(credentialsId: 'docker-config-json', variable: 'DOCKER_CONFIG_JSON')]) {
                      sh "${bob} -r ${local_ruleset} generate-docs || true"
                  }
          }