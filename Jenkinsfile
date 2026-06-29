// =====================================================================
//  OmiinQA :: Declarative Jenkins pipeline
//  Parameterized, parallel quality + test stages, Allure publishing.
//  Requires Jenkins plugins: Allure, HTML Publisher, Docker (optional).
// =====================================================================
pipeline {
    agent any

    tools {
        jdk 'jdk-21'
        maven 'maven-3.9'
    }

    parameters {
        choice(name: 'SUITE',
               choices: ['testng-smoke.xml', 'testng-regression.xml', 'testng-api.xml', 'testng-ui.xml', 'testng-e2e.xml'],
               description: 'TestNG suite to execute')
        choice(name: 'BROWSER', choices: ['chrome', 'firefox', 'edge', 'remote-chrome'], description: 'Target browser')
        choice(name: 'ENV', choices: ['qa', 'staging', 'dev'], description: 'Target environment')
        booleanParam(name: 'HEADLESS', defaultValue: true, description: 'Headless execution')
    }

    options {
        timestamps()
        ansiColor('xterm')
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timeout(time: 60, unit: 'MINUTES')
    }

    environment {
        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
    }

    stages {
        stage('Checkout') {
            steps { checkout scm }
        }

        stage('Build') {
            steps { sh 'mvn -B clean test-compile' }
        }

        stage('Static Analysis') {
            parallel {
                stage('Checkstyle/PMD/SpotBugs') {
                    steps { sh 'mvn -B -P quality verify -DskipTests' }
                }
                stage('Dependency-Check') {
                    when { expression { params.SUITE == 'testng-regression.xml' } }
                    steps { sh 'mvn -B -P security verify -DskipTests || true' }
                }
            }
        }

        stage('Test') {
            steps {
                sh """
                   mvn -B test \
                       -Dsuite.file=${params.SUITE} \
                       -Dbrowser=${params.BROWSER} \
                       -Dheadless=${params.HEADLESS} \
                       -Denv=${params.ENV}
                """
            }
        }
    }

    post {
        always {
            junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
            allure includeProperties: false, results: [[path: 'target/allure-results']]
            archiveArtifacts artifacts: 'extent-reports/**, logs/**, screenshots/**',
                             allowEmptyArchive: true
        }
        failure {
            echo 'Build failed — see Allure & Extent reports for triage.'
        }
        cleanup { cleanWs() }
    }
}
