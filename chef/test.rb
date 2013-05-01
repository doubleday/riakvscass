
def get_hosts
  @defaults = {}
  %w[description env_run_lists name override_attributes run_list].each do |meth|
    binding.eval("def #{meth}(params); end")
  end

  def default_attributes(attr)
    @defaults.merge!(attr)
  end
  binding.eval(IO.read('/Users/daniel/Source/wooga/riakvscass/chef/roles/base.rb'))

  @defaults['hosts']
end

puts get_hosts
