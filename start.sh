mvn clean spring-boot:run -Pprod -Dspring.profiles.active=iss,prod -Dserver.servlet.context-path=/ -Duser.admin.password=admin -Drepository.base.url=http://localhost:9080/alfresco/ -Dcache.debug=false -Dspid.enable=true -Dspid.issuer.entityid=http://localhost:8088 -Dspid.destination=http://localhost:8080/spid/send-response
