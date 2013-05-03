name 'base'

run_list %w{
  recipe[system]
  recipe[system::collectl]
}

default_attributes(
    'hosts' => {
      'graphite'  => %w(10.231.7.187    54.228.146.235),
      'load'      => %w(10.210.165.96   54.246.23.6),
      'riak01'    => %w(10.62.41.40     54.216.21.123),
      'riak02'    => %w(10.208.73.159   46.51.154.81),
      'riak03'    => %w(10.208.18.182   54.216.1.54),
      'riak04'    => %w(10.210.165.106  79.125.65.195),
      'riak05'    => %w(10.210.165.68   54.247.9.244)
    }
)