name 'riak'
description 'recipes for riak db server'

run_list %w{
  role[base]
  recipe[riak]
  recipe[riak-graphite]
}

default_attributes(
  'riak' => {
    'package' => {
      'version' => {
        'major' => '1',
        'minor' => '3',
        'incremental' => '1~precise1'
      }
    },
    'config' => {
      'bitcask' => {
        'data_root' => '/mnt/riak/bitcask'
      }
    }
  },

  # sysctl
  'sysctl' => {
    'net' => {
      'core' => {
        'wmem_default' => 8388608,
        'rmem_default' => 8388608,
        'wmem_max' => 8388608,
        'rmem_max' => 8388608,
        'netdev_max_backlog' => 10000,
        'somaxconn' => 4000
      },
      'ipv4' => {
        'tcp_max_syn_backlog' => 40000,
        'tcp_fin_timeout' => 15,
        'tcp_tw_reuse' => 1
      }
    }
  }
)
