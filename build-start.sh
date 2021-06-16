mvn clean compile package
docker rm selezioni-iss
docker image rm selezioni-iss/latest
docker build -t selezioni-iss/latest .
docker run -p 8080:8080 --name selezioni-iss selezioni-iss/latest

