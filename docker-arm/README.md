# How to run GG9 cluster in Docker on Arm architecture

1. Download the binary distribution and extract
```sh
unzip gridgain-9.0.x.zip
mv gridgain-db-9.0.x dist/db
mv gridgain-cli-9.0.x dist/cli
```
1. Add license file to k8/gridgain-license.conf
2. Build the Docker image
```sh
cd docker
docker build -t gridgain/gridgain9:9.0.x .
```
3.
4. Initialise the cluster using cli
```bash
gridgain9
  _________        _____ __________________        _____
  __  ____/___________(_)______  /__  ____/______ ____(_)_______
  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
                      GridGain CLI ver. 9.0.0

You appear to have not connected to any node yet. Do you want to connect to the default node http://localhost:10300? [Y/n] Y
Connected to http://localhost:10300
[node1]> cluster init --metastorage-group=gridgain-cluster-0,gridgain-cluster-1,gridgain-cluster-2 --name=TEST --config-files=/gg9/etc/gridgain-license.conf

```
