mvn clean install -Pprod
docker rm selezioni-iss
docker image rm selezioni-iss/latest
docker build -t selezioni-iss/latest .
docker run --network="host" --name="selezioni-iss" selezioni-iss/latest


