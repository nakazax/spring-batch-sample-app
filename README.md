# spring-batch-sample-app
Sample app for Spring Boot & Spring Batch.

## Command notes
### jobConvertPersonCsv2Tsv

```
java -jar target/spring-batch-sample-app-1.0.0-SNAPSHOT.jar --spring.batch.job.names=jobConvertPersonCsv2Tsv --spring.batch.job.enabled=true csv=src/main/resources/sample-data.csv tsv=src/main/resources/sample-data.tsv
```

### jobImportPerson

```
java -jar target/spring-batch-sample-app-1.0.0-SNAPSHOT.jar --spring.batch.job.names=jobImportPerson --spring.batch.job.enabled=true --spring.batch.initialize-schema=always csv=src/main/resources/sample-data.csv
```
