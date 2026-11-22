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
        ACME_EMAIL = 'goldydefault@gmail.com'
    }
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.REVISION = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
                }
            }
        }
        stage('Prepare host') {
            steps { sh 'bash devops/deploy/provision.sh "$BANK_DOMAIN" "$ACME_EMAIL"' }
        }
        stage('Test and build JAR') {
            steps { sh 'bash devops/deploy/build.sh' }
        }
        stage('Deploy and verify HTTPS') {
            steps {
                withCredentials([
                    string(credentialsId: 'baltic-bank-db-url', variable: 'DB_URL'),
                    usernamePassword(credentialsId: 'baltic-bank-db', usernameVariable: 'DB_USERNAME', passwordVariable: 'DB_PASSWORD')
                ]) {
                    sh '''#!/usr/bin/env bash
set -euo pipefail
python3 devops/deploy/database_env.py | bash devops/deploy/release.sh "$REVISION" "$BANK_DOMAIN"
'''
                }
            }
        }
    }
}
