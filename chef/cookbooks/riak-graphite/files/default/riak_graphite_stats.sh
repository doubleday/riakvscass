#!/bin/bash
# *  *    * * *   root    /path/to/riak_graphite_stats.sh

set -e

SOURCE=$(hostname)
GRAPHITE_PORT=2003
GRAPHITE_SERVER="graphite"
PREFIX=$(hostname).riak

while true ; do
  STATUS=$(riak-admin status)

  VARIABLES=("vnode_gets" "vnode_puts" "read_repairs" "node_gets" "node_puts" "cpu_nprocs" "sys_process_count" "pbc_connects" "pbc_active" \
    "node_get_fsm_time_mean" "node_get_fsm_time_median" "node_get_fsm_time_95" "node_get_fsm_time_99" "node_get_fsm_time_100" \
    "node_put_fsm_time_mean" "node_put_fsm_time_median" "node_put_fsm_time_95" "node_put_fsm_time_99" "node_put_fsm_time_100")


  DATE=$(date +%s)

  for STAT in ${VARIABLES[@]}; do
    RE_PREFIX=$'(\n|^)'
    RE_SUFFIX=$' : ([^\n]*)'

    RE="$RE_PREFIX$STAT$RE_SUFFIX"

    if [[ "$STATUS" =~ $RE ]]; then
      VALUE=${BASH_REMATCH[2]}
      echo "$PREFIX.$STAT $VALUE $DATE" | nc ${GRAPHITE_SERVER} ${GRAPHITE_PORT}
    fi
  done
  sleep 10
done