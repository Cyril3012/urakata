# urakata
urakata ( devops )

# database url 
http://localhost:8080/h2-console/

local database files : urakataDb.lock , urakataDb.mv , urakataDb.trace
#----------------------------------------
#Database connections 
#----------------------------------------
spring.datasource.url=jdbc:h2:file:D:/opt/H2Database/urakataDb;AUTO_SERVER=TRUE;IFEXISTS=FALSE
#spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_ON_EXIT=FALSE;
spring.datasource.driverClassName=org.h2.Driver
