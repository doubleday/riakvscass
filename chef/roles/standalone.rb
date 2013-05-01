name 'standalone'
description 'standalone server'

default_attributes(
  'servers'  => {
    'riak'  => ['127.0.0.1'],
    # ordering is important
    'redis' => [{:host => '127.0.0.1', :db => 0},{:host => '127.0.0.1', :db => 1}],
    'app'   => ['127.0.0.1']
  },
  'application' => {
    'name'          => 'g9_backend',
    'path'          => '/home/ubuntu/applications',
    'server'        => 2,
    'tracking_host' => 'g9-test.t.wooga.com'
  },
  'nginx'       => {
    'server_name' => 'g9-staging.wooga.com'
  }
)
run_list %w{
  role[base]
  recipe[s3]
  recipe[nginx] 
  recipe[redis]
  recipe[riak]
  recipe[ruby-rvm]
  recipe[application]
  recipe[standalone]
}
