<div style="text-align: center;">

[![License](https://img.shields.io/badge/license-MIT-blue)](https://github.com/Edgxtech/prise/blob/master/LICENSE)
</div>

# Consignment Management System (CMS) - Demo App

Kotlin Spring based Application as used in the Medium write up: [Data Flow across Trust Boundaries using Decentralised Ledgers](https://medium.com/@tdedgx/data-flow-across-trust-boundaries-using-decentralised-ledgers-515203676ce5). As a *demonstration*, provides Consignment Tracking Functionality over a Blockchain Distributed Ledger. 

Uses;
-   [Cardano Node](https://github.com/IntersectMBO/cardano-node) with [Ogmios](https://github.com/CardanoSolutions/ogmios) interface for blockchain interaction
-   Cardano Foundation [Reeve - Ledger on Blockchain Platform](https://github.com/cardano-foundation/cf-reeve-platform) for sync of data from off and on-chain (extended version)
-   Bloxbean [Yaci](https://github.com/bloxbean/yaci) JVM based Cardano mini-protocols library, used as part of Reeve
-   Bloxbean [YaciStore](https://github.com/bloxbean/yaci-store) general purpose indexer, used as part of Reeve
-   [Blockfrost](https://blockfrost.io) Cardano data API, only for txOutput resolution and chain sync checking

## Quickstart

**Configure your environment**

```bash
cp org1.env.template org1.env
ln -s org1.env .env
```

Modify .env, in particular, these are required. You may modify other variables if needed
```properties
LOB_OWNER_ACCOUNT_MNEMONIC=<use any existing funded testnet wallet or setup wallet as described below>
BLOCKFROST_API_KEY=<get from https://blockfrost.io>
DATABASE_USERNAME=<yours>
DATABASE_PASSWORD=<yours>
KEYCLOAK_ADMIN_USERNAME=<yours>
KEYCLOAK_ADMIN_PASSWORD=<yours>
```

**(If needed) Setup a PREPROD testnet wallet**

Generate a Mnemonic which represents your private key and should be stored in *LOB_OWNER_ACCOUNT_MNEMONIC*.
```bash
python scripts/generate_mnemonic.py --org-name org1
```

Import into a cardano wallet e.g. [Lace](https://www.lace.io/), [Eternl](https://eternl.io), [Vespr](https://vespr.xyz/).

Visit the testnet funding faucet [https://docs.cardano.org/cardano-testnets/tools/faucet](https://docs.cardano.org/cardano-testnets/tools/faucet), select preprod network, paste in a receive address from your cardano testnet wallet, request funds and wait a couple mins

**Run the system**

```bash
docker compose up -d
```

```bash
scripts/check_running.sh
```

Wait until a state similar to the following
```
=== Docker Compose Stats Snapshot ===fig   w Enable Watch
CONTAINER ID   NAME                          CPU %     MEM USAGE / LIMIT     MEM %     NET I/O           
846d44b9b310   cms-demo-app-app-1            0.40%     585.6MiB / 9.704GiB   5.89%     348kB / 262kB     
98074b703a69   cms-demo-app-keycloak-1       0.37%     574.3MiB / 9.704GiB   5.78%     344kB / 161kB     
d4006b7291bb   cms-demo-app-indexer-1        0.40%     768MiB / 9.704GiB     7.73%     14.6MB / 18.8MB   
f906b22af89b   cms-demo-app-ogmios-1         0.00%     310.5MiB / 9.704GiB   3.13%     4.98kB / 4.54kB   
7198660c1fa4   cms-demo-app-postgres-1       0.00%     144.7MiB / 9.704GiB   1.46%     18.2MB / 4.36MB   
66d386bc9384   cms-demo-app-cardano-node-1   1.72%     2.994GiB / 9.704GiB   30.86%    17.5MB / 11.2MB   

=== Health Checks ===
Checking Postgres...
localhost:5433 - accepting connections
✓ Postgres is ready
Checking Cardano Node via Ogmios (http://localhost:1337/health) ...
✓ Cardano Node is synchronised
Checking Indexer (http://localhost:9090/api/v1/tip) ...
✓ Indexer is synced
Checking Keycloak (http://localhost:9000/health/ready) ...
✓ Keycloak is ready
Checking CMS Demo App (http://localhost:8088/api/hello) ...
✓ CMS Demo App is healthy
All checks passed successfully!
```

Now you will have CRUD endpoints available for Consignments on http://localhost:8088/api/consignments


## Syncing Cardano Node

Getting the DLT Node synced up requires a bit of lead time. 

You will see progress via the `check_running.sh` script as follows

```
=== Health Checks ===
Checking Postgres...
localhost:5433 - accepting connections
✓ Postgres is ready
Checking Cardano Node via Ogmios (http://localhost:1337/health) ...
✗ Cardano Node is not fully synchronised (networkSynchronization: 0.01802)
```

You can also follow progress:

**In the logs**

during synch, the cardano-node service will show logs like this
```bash
docker compose logs cardano-node -f
cardano-node-1  | [8804551b:cardano.node.ChainDB:Info:5] [2025-07-30 02:07:10.90 UTC] Opened db with immutable tip at genesis (origin) and tip genesis (origin)
...
cardano-node-1  | [8804551b:cardano.node.PeerSelection:Info:65] [2025-07-30 02:07:10.95 UTC] TraceUseBootstrapPeersChanged (UseBootstrapPeers [RelayAccessDomain "preprod-node.play.dev.cardano.org" 3001])
...
cardano-node-1  | [8804551b:cardano.node.ChainDB:Notice:33] [2025-07-30 02:07:12.42 UTC] Chain extended, new tip: 1d031daf47281f69cd95ab929c269fd26b1434a56a5bbbd65b7afe85ef96b233 at slot 2
```

And once synced the slot number with be closer to a number correlated to the current time.

**Or, through the Ogmios interface**

Check at the following endpoint directly. Key properties to confirm are network and sync.
```bash
curl localhost:1337/health
{ .. "networkSynchronization":1.00000 .. "network":"preprod" ..}
```

### Speeding up the Sync Progress

To speed up this process and get to the demo quicker, you can:

* Skip it altogether and point the indexer to a public relay node rather than running your own. There is an example url in the ` .env.template` file. This can be a bit flaky with frequent connection resets by the Yaci libraries attempting to communicate, however can work

* Restore the node ledger from a snapshot (see below)

### Restore Cardano Node Ledger State From Snapshot

To speed-up the process, you can try download and extract a Cardano ledger snapshot to the local database directory, there are other snapshot providers you can try, here is one by [csnapshots.io](https://csnapshots.io)
```bash
export TARGET_DIR=/path/to/cms-demo-app/background/cnode
rm -rf $TARGET_DIR/db/* && mkdir -p $TARGET_DIR/db
curl -o - https://downloads.csnapshots.io/testnet/$(curl -s https://downloads.csnapshots.io/testnet/testnet-db-snapshot.json | jq -r '.[].file_name' ) | zstd -d -c | tar -x -C $TARGET_DIR/
```

Replace /path/to with your development folder location.

This should reduce initial startup time from hours to minutes.

## Run simple demo

**Check Demo Environment**

The 'setup-accounts' docker service should auto run and configure keycloak with a realm, client, service account and related authentication token that can be used by demonstration scripts. It also configures the CMS app to have the organisation available in the database for use during demo's

view scripts/.env - it should contain
```properties
ORG1_ID=<id>
ORG1_NAME=org1
CMS_BASE_URL_ORG1="http://localhost:8088/api"
CMS_AUTH_TOKEN_ORG1=<your jwt token>
```

Optionally, Login to keycloak https://localhost:8080 and check realm, client, service accounts related to cms-demo

If needed re-run the accounts setup
```bash
docker compose setup-accounts up
```

**Test creation of a single consignment** 

Create consignment from sender to receiver with arbitrary goods and locations - this results in blockchain publishing.

```python
python scripts/test_single_consignment.py
```

Check database
```bash
curl -X GET http://localhost:8088/api/consignments -H "Authorization: Bearer $CMS_AUTH_TOKEN_ORG1"
> [{"id":"... ]
```

Check database
```bash
scripts/exec_in_psql.sh
```
```sql
select * from blockchain_publisher_consignment;
> consignment_id | id_control | ...
etc...
```

**Run second organisation as listener**

As an extension, run a second CMS to demonstrate second party reading of published consignments.
This reuses the same cardano node and postgres instance but for postgres, a different database name.
It spins up a demo org2 specific keycloak, CMS indexer and CMS app and adds ORG2 specific variables to scripts/.env for demo usage.

```bash
docker compose -p org2 --env-file=org2.env -f docker-compose-org2.yml up -d
```

Check database on ORG2 CMS App Port
```bash
curl -X GET http://localhost:8089/api/consignments -H "Authorization: Bearer $CMS_AUTH_TOKEN_ORG2"
> [{"id":"... ]
```


## Building

It is necessary to build the [Reeve]( https://github.com/cardano-foundation/cf-reeve-platform) platform AND the background follower app into your local maven repo. The installed libraries can then be included in the CMS projects' build context.

**Building Reeve**

```bash
git clone https://github.com/cardano-foundation/cf-reeve-platform
cd cf-reeve-platform
./gradlew clean build publishMavenJavaPublicationToLocalM2Repository -x test
```

The CMS indexer requires installing lob-follower-app dependencies to local maven repo separately. First, modify `_backend-services/cf-reeve-ledger-follower-app/build.gradle.kts`, merge in all the following
```bash
plugins {
    id("maven-publish")
}
version = "1.0.1-SNAPSHOT"
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    sourceCompatibility = org.gradle.api.JavaVersion.VERSION_21
}
publishing {
    publications {
    create<MavenPublication>("mavenJava") {
        from(components["java"])
        groupId = project.group.toString()
        artifactId = project.name
        version = project.version.toString()
    }
}
repositories {
    maven {
        name = "localM2"
        url = uri("${System.getProperty("user.home")}/.m2/repository")
        }
    }
}
```

Then build & install to local Maven repo
```bash
cd _backend-services/cf-reeve-ledger-follower-app/
./gradlew clean build publishMavenJavaPublicationToLocalM2Repository -x test
```

**Building CMS**

Now build the CMS app as a Docker image
```bash
git clone https://github.com/Edgxtech/cms-demo-app
cd cms-demo-app
# build the jar first
./gradlew clean build -x test
docker build -t edgxtech/cms-demo-app .
```

And the CMS Indexer. This indexer is an extended version of the cf-reeve-platform follower-app, with one key change being the indexing of Consignment specific events.
```bash
cd background/cms-demo-indexer
# build the jar first
./gradlew clean build -x test
docker build -t edgxtech/cms-demo-indexer .
```

Optional, to save effort configuring Keycloak or if running the demo's, an account setup script is included. It just needs a suitable Python environment to run in the docker context, of which you can build from the CMS project root like so.
```bash
cd config/python-yaml-env
docker build -t edgxtech/python-yaml-env -f python-yaml-env.Dockerfile .
```

You should now have all the images needed to bring up the system; check the `docker-compose.yml` to confirm.

## Checks

Here's a few ways you can check logs and operation.
```bash
docker compose logs -f
```
```bash
docker compose logs app -f
```
```bash
scripts/check_running.sh
```
```bash
scripts/exec_in_psql.sh
```

In the DB, check the necessary schemas are created, e.g. should have **keycloak** & **lob_follower_service**
```sql
SELECT schema_name FROM information_schema.schemata;
 > public, keycloak, lob_follower_service
 ```

In the DB, check tables in public schema; should be many ~60 with owner being the postgres user as specified in the projects .env
```sql
 \d
 > e.g. 
  public | blockchain_publisher_consignment                         | table    | cmsuser
   ...
```


## Components
| Component | Description                                                                                                                                                         | 
|----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| indexer  | Extended Reeve background follower-app which, in addition to indexing normal blockchain data (blocks, transactions, etc..) listens for and indexes new consignments |  
| app      | Kotlin Spring Boot webserver to handle consignment management and to implement the Reeve platform for blockchain sync                                               |   


## Java Version

    Tested with Java 21


## What can go Wrong

| Issue                                                                                                                                                                                                                                                               | Resolution                                                                                    | 
|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------|
| *Cannot connect to the Docker daemon at unix:///path/to/docker.sock. Is the docker daemon running?*                                                                                                                                                                 | Install and Start Docker Engine. On Desktop Install and Run Docker Desktop                    |  
| *Sometimes Keycloak gets in a bad state resulting in; GET organisation response status: 500, body: {"timestamp":1754107988982,"status":500,"error":"Internal Server Error","path":"/api/organisations/<org id>"}*                                                   | Restart keycloak `docker compose down keycloak && dc up keycloak -d`                          |
| *Building cf-lob-platform can throw Execution failed for task ':blockchain_common:compileJava'. error: invalid source release: 21                                                                                                                                   | add JVM version toolchain `java { toolchain { languageVersion = JavaLanguageVersion.of(21) }` |
| *Execution failed for task ':accounting_reporting_core:spotlessJava'. Issue processing file: /path/to/cf-reeve-platform/accounting_reporting_core/src/test/java/org/cardanofoundation/lob/app/accounting_reporting_core/resource/views/ReportResponseViewTest.java* | disable `spotless { java { //removeUnusedImports() } }`                                       |


## Contributions
Contributions welcome

## Support
This project is made possible by Delegators to the [AUSST](https://ausstaker.com.au) Cardano Stakepool and
supporters of [Edgx](https://edgx.tech) R&D



