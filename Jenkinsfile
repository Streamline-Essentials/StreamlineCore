pipeline {
    agent any
    tools {
        // Specify the Gradle version configured in Jenkins
        gradle 'Gradle'
        // Specify the JDK version configured in Jenkins
        jdk 'Java 21'
    }
    stages {
        stage('Checkout') {
            steps {
                // Checkout code from SCM (e.g., Git)
                checkout scm
            }
        }
        stage('Build') {
            steps {
                // Run Gradle build
                sh 'echo $JAVA_HOME'
                sh 'echo $GRADLE_HOME'
                sh 'java -version'
                sh 'gradle --version'
                sh 'gradle clean build'
            }
        }
        stage('Publish Artifacts') {
            when {
                // Only publish for main branch
                branch 'main'
            }
            steps {
                // Archive build artifacts
                archiveArtifacts artifacts: 'build/libs/*.jar', allowEmptyArchive: true
            }
        }
    }
    post {
        always {
            // Clean workspace after build
            cleanWs()
        }
        success {
            // Notify on success
            echo 'Build and tests completed successfully!'
        }
        failure {
            // Notify on failure
            echo 'Build or tests failed!'
        }
    }
}