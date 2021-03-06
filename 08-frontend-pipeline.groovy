podTemplate(
    name: 'questcode',
    namespace: 'devops', 
    label: 'questcode', 
    containers: [
            containerTemplate(alwaysPullImage: false, args: 'cat', command: '/bin/sh -c', envVars: [], image: 'docker', livenessProbe: containerLivenessProbe(execArgs: '', failureThreshold: 0, initialDelaySeconds: 0, periodSeconds: 0, successThreshold: 0, timeoutSeconds: 0), name: 'docker-container', ports: [], privileged: false, resourceLimitCpu: '', resourceLimitMemory: '', resourceRequestCpu: '', resourceRequestMemory: '', shell: null, ttyEnabled: true, workingDir: '/home/jenkins/agent'),
            containerTemplate(args: 'cat', command: '/bin/sh -c', image: 'lachlanevenson/k8s-helm:v2.11.0', name: 'helm-container', ttyEnabled: true)

            
    ],
    volumes: [hostPathVolume(hostPath: '/var/run/docker.sock', mountPath: '/var/run/docker.sock')],
) {
    node('questcode') {
        def REPOS
        def IMAGE_VERSION ="0.1.4"
        def IMAGE_POSFIX = ""
        def KUBE_NAMESPACE
        def IMAGE_NAME = "questcode-frontend"
        def ENVIRONMENT = "staging"
        def GIT_REPOS_URL = "https://github.com/jefersonaraujo/missaodevops.git"
        def GIT_BRANCH 
        def HELM_CHART_NAME = "questcode/frontend"
        def HELM_DEPLOY_NAME
        def CHARTMUSEUM_URL = "http://helm-chartmuseum:8080"
        def INGRESS_HOST = "questcode.org"

        stage('Checkout') {
            echo "Inicializando Clone do Repositorio"
            REPOS = git credentialsId: 'Github', url: GIT_REPOS_URL     
            //IMAGE_VERSION = sh returnStdout: true, script: 'sh ./frontend/read-package-version.sh'
            //IMAGE_VERSION = IMAGE_VERSION.trim() + IMAGE_POSFIX          
        }
        stage('Package') {
            container('docker-container') {                
                echo "Inicializando empacotamento com Docker"
                withCredentials([usernamePassword(credentialsId: 'dockerhub', passwordVariable: 'DOCKER_HUB_PASSWORD', usernameVariable: 'DOCKER_HUB_USER')]) {
                      sh "docker login -u ${DOCKER_HUB_USER} -p ${DOCKER_HUB_PASSWORD}"
                      sh "docker build -t ${DOCKER_HUB_USER}/${IMAGE_NAME}:${IMAGE_VERSION} ./frontend --build-arg NPM_ENV='${ENVIRONMENT}'"
                      sh "docker push ${DOCKER_HUB_USER}/${IMAGE_NAME}:${IMAGE_VERSION}"       
                }
              
               
            }
        }
        stage('Deploy') {
            container('helm-container') {
                echo "Inicializando Deploy com Helm"                
                sh """
                    helm init --client-only
                    helm repo add questcode ${CHARTMUSEUM_URL}
                    helm repo update
                    helm upgrade staging-frontend questcode/frontend --set image.tag=${IMAGE_VERSION}
                """              

            }
   
        }
    }
} 


