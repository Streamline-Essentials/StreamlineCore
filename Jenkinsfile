pipeline {
    agent any

    tools {
        gradle 'Gradle' // Gradle tool configured in Jenkins
        jfrog 'jfrog-cli' // JFrog CLI tool, ensure it's configured in Jenkins
    }

    stages {
        stage('Checkout') {
            steps {
                // Checkout code from SCM (Git assumed)
                checkout scm
            }
        }

        stage('Build') {
            steps {
                // Chmod all files in the workspace to ensure they are executable
                sh 'find . -type f -exec chmod +x {} \\;'
                // Use Gradle wrapper for consistency
                sh './gradlew build'
            }
        }

        stage('Publish Artifacts') {
            steps {
                // Publish artifacts to Jenkins
                archiveArtifacts artifacts: '**/build/libs/*.jar', allowEmptyArchive: true
            }
        }

        stage('Artifactory Publish') {
            steps {
                jf 'rt build-publish'
            }
        }
    }

    post {
        always {
            // Clean workspace, but preserve artifacts/logs on failure for debugging
            cleanWs cleanWhenFailure: false
        }
        success {
            echo 'Build and tests completed successfully!'
        }
        failure {
            echo 'Build or tests failed! Check logs and artifacts for details.'
        }
    }
}