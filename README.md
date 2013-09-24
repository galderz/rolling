Infinispan Rolling Upgrades Showcase
====================================

Shows off Infinispan Rolling Upgrades using two projects. The first project,
called `rolling-source`, represents a source cluster with data on it. The
second project, named `rolling-target`, represents the target cluster to
which data is migrated live. Both clusters are running an embedded Infinispan
library configuration.

To run the application:

1. Run `SourceCluster` (from IDE or command line) from `rolling-source`.
This will start a two-node replicated cluster loading 3 key/value pairs into
it. More data can be stored in the cluster by pressing `<ENTER>` from the
command line, mimicking live data storage. Run with the following system
properties:

    -Djava.net.preferIPv4Stack=true
    -Djgroups.bind_addr=127.0.0.1
    -Dcom.sun.management.jmxremote
    -Dcom.sun.management.jmxremote.port=2626
    -Dcom.sun.management.jmxremote.authenticate=false
    -Dcom.sun.management.jmxremote.ssl=false

2. Run `TargetCluster` (from IDE or command line) from `rolling-target`.
This will start a two-node replicated cluster which brings back the 3
key/value pairs initially stored in the source cluster. More data can be
lazily be brought back by pressing `<ENTER>`. Run with the following system
properties:

    -Djava.net.preferIPv4Stack=true
    -Djgroups.bind_addr=127.0.0.1
    -Dcom.sun.management.jmxremote
    -Dcom.sun.management.jmxremote.port=2626
    -Dcom.sun.management.jmxremote.authenticate=false
    -Dcom.sun.management.jmxremote.ssl=false

3. Typically, you'd start `SourceCluster` and then `TargetCluster`. Once both
are up, click `<ENTER>` several times in `SourceCluster` and click `<ENTER>`,
the same number of times in the `TargetCluster`. Any data that has been stored
as a result of pressing `<ENTER>` in the source cluster should be retrieved
successfully by the target cluster.