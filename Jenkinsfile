pipeline {
    agent { label 'baltic-bank-vm' }
    options {
        disableConcurrentBuilds()
        timeout(time: 60, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '15'))
        skipDefaultCheckout(true)
    }
    environment {
        BANK_DOMAIN = 'balticbank.duckdns.org'
    }
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    def commit = sh(script: 'git rev-parse --short=12 HEAD', returnStdout: true).trim()
                    env.RELEASE_TAG = "${commit}-${env.BUILD_NUMBER}"
                }
            }
        }
        stage('Prepare Docker') {
            steps { sh 'bash devops/deploy/docker-job.sh prepare' }
        }
        stage('Test and build image') {
            steps { sh 'bash devops/deploy/docker-job.sh build' }
        }
        stage('Deploy bank container') {
            steps {
                withCredentials([
                    string(credentialsId: 'baltic-bank-db-url', variable: 'DB_URL'),
                    usernamePassword(credentialsId: 'baltic-bank-db', usernameVariable: 'DB_USERNAME', passwordVariable: 'DB_PASSWORD')
                ]) {
                    sh 'bash devops/deploy/docker-job.sh deploy'
                }
            }
        }
    }
}
