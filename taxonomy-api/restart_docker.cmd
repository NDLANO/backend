docker stop taxonomy-postgres
docker rm taxonomy-postgres
docker run -p 127.0.0.1:5432:5432 --name taxonomy-postgres -d postgres:alpine