#/bin/bash

service_name=$1
resource_group=$2

mvn clean package -DskipTests

az spring-cloud app create --name gateway -s ${service_name} -g ${resource_group}
az spring-cloud app update --is-public true -n gateway -s ${service_name} -g ${resource_group}
az spring-cloud app create --name auth-service -s ${service_name} -g ${resource_group}
az spring-cloud app create --name account-service -s ${service_name} -g ${resource_group}
az spring-cloud app update --is-public true -n  account-service -s ${service_name} -g ${resource_group}

az spring-cloud app deploy -n gateway --jar-path ./gateway/target/gateway.jar -s ${service_name} -g ${resource_group}
az spring-cloud app deploy -n auth-service --jar-path ./auth-service/target/auth-service.jar -s ${service_name} -g ${resource_group}
az spring-cloud app deploy -n account-service --jar-path ./account-service/target/account-service.jar -s ${service_name} -g ${resource_group}

