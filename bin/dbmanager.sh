#! /bin/sh
# 
# Hibernate OGM, Domain model persistence for NoSQL datastores
# 
# License: GNU Lesser General Public License (LGPL), version 2.1 or later
# See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.

# Stop and start Docker containers for the various datastore tested
# Emmanuel Bernard

# unset ${!DOCKER_*}

function stop {
    echo "stopping $1"
    docker stop ogm_$1
    docker rm ogm_$1
}

function run {
    case $1 in
    mongodb)
    docker run -d -p 27017:27017 -p 28017:28017 --name ogm_mongodb mongo --storageEngine wiredTiger
    ;;
    couchdb)
    docker run -d -p 5984:5984 --name ogm_couchdb fedora/couchdb
    ;;
    redis)
    docker run --name ogm_redis -d -p 6379:6379 redis
    ;;
    cassandra)
    docker run --name ogm_cassandra -d -p 9042:9042 cassandra:2.2
    ;;
    *)
    echo "Error"
    ;;
    esac
}

function logs {
    docker logs -f ogm_$1
}

function checkdb {
    case $1 in
    mongodb) ;;
    couchdb) ;;
    redis) ;;
    cassandra) ;;
    *)
    echo "Unknown database $1"
    exit 1;
    esac
}

if [[ "$#" -eq "1" ]]; then
    COMMAND="$1"
    case $COMMAND in
    start)
    run mongodb
    run couchdb
    run redis
    run cassandra
    ;;
    stop)
    stop mongodb
    stop couchdb
    stop redis
    stop cassandra
    ;;
    restart)
    stop mongodb
    stop couchdb
    stop redis
    stop cassandra
    run mongodb
    run couchdb
    run redis
    run cassandra
    ;;
    *)
    echo "Unknown command $1"
    ;;
    esac
elif [[ "$#" -eq "2" ]]; then
    DB="$1"
    COMMAND="$2"
    checkdb $DB
    case $COMMAND in
    start)
    run $DB
    ;;
    stop)
    stop $DB
    ;;
    logs)
    logs $DB
    ;;
    *)
    echo "Unknown command $COMMAND"
    ;;
    esac
else 
    echo "dbmanager {start|stop|restart} : execute command on all databases"
    echo "dbmanager {mongodb|cassandra|redis|couchdb} {start|stop|logs} : execute command on all databases"
    exit 1;
fi
exit -1;

