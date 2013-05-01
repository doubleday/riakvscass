name 'standalone'
description 'standalone server'

default_attributes(
)

run_list %w{
  role[base]
  recipe[graphite]
}
