package org.devops

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;

def replaceVersion() { 
    //获取gitops代码
    sh '''
        git config --global user.email "${gitlab_username}@example.com"
        git config --global user.name "${gitlab_username}"                       
        git clone http://${gitlab_username}:${gitlab_password}@gitlab.example.com:13140/operation/example-helm.git   
        chown -R 1000:1000 example-helm
    ''' 
    String filename = "example-helm/${group}/values/${deployEnv}/${project}-${deployEnv}-values.yaml"
    File yaml = new File("${filename}") 
    println yaml.text 

    String newVersion = "2.0.0" 
    yaml.text = yaml.text.replaceFirst(/tag: '.*'/, "tag: '${newVersion}'") 
    println yaml.text 
}