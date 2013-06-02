name 'riak'
description 'recipes for riak db server'

run_list %w{
  role[base]
  recipe[java]
  recipe[cassandra::install_from_package]
}

default_attributes(
  "java" => {
    "install_flavor" => "oracle",
    "oracle" => {
      "accept_oracle_download_terms" => true
    }
  }
)
