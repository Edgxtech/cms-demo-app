#!/bin/bash

docker exec -it cms-demo-app-postgres-1 psql -U cmsuser -h localhost -p 5433 -d cmsdemo
