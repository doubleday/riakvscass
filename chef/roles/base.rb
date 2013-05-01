name 'base'

run_list %w{
  recipe[system]
  recipe[system::collectl]
}

default_attributes(
    'hosts' => {
      'graphite'  => %w(10.36.148.190 54.216.14.105),
      'riak01'    => %w(10.36.180.179 54.228.42.162),
      'riak03'    => %w(10.36.214.15 54.228.122.115)
    }
)