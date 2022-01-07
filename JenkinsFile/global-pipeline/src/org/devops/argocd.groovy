package org.devops

def ArgocdInitDeploy(){
    container('helm'){
        sh '''
        argocd login --username admin --password ${argocd_password} argocd.example.cn
        argocd proj create ${group} --allow-cluster-resource "*"/"*" --allow-namespaced-resource "*"/"*" --dest "*","*" --src "*" --upsert
        argocd app create ${project} \
            --upsert \
            --project ${group} \
            --repo http://gitlab.example.com:13140/operation/example-helm.git \
            --path ${group}\
            --revision ${deployEnv} \
            --dest-server  https://kubernetes.default.svc \
            --dest-namespace ${group} \
            --sync-policy automated \
            --self-heal \
            --sync-option CreateNamespace=true \
            --values values/${project}-values.yaml
        argocd app wait ${project} --health
        '''
    }
}