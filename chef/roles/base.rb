name 'base'

run_list %w{
  recipe[system]
  recipe[system::collectl]
}

default_attributes(
    'hosts' => {
      'graphite'  => %w(10.35.132.102  54.228.171.11),
      'load1'  => %w(10.34.184.51  54.216.81.9),
      'load2'  => %w(10.35.144.207  46.137.29.86),
      'load3'  => %w(10.35.142.36  54.247.23.162),
      'load4'  => %w(10.34.128.230  54.228.153.134),
      'db1'  => %w(10.35.142.42  79.125.41.62),
      'db2'  => %w(10.34.129.71  54.228.54.79),
      'db3'  => %w(10.34.136.142  176.34.86.240),
      'db4'  => %w(10.35.144.9  79.125.50.30),
      'db5'  => %w(10.35.144.226  54.247.134.151)
    }
)