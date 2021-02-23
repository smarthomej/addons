pipeline {
  agent {
    docker {
      image '3.6.3-jdk-11'
    }

  }
  stages {
    stage('Build') {
      steps {
        sh 'mvn -B clean install'
      }
    }

  }
}