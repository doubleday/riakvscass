service "riak_graphite_stats" do
  supports :status => true, :start => true, :stop => true, :restart => true
  action :nothing
end

cookbook_file "/usr/sbin/riak_graphite_stats.sh" do
  source "riak_graphite_stats.sh"
  mode 0755
end

cookbook_file "/etc/init.d/riak_graphite_stats" do
  source "riak_graphite_stats_init"
  mode 0755
  notifies :enable, "service[riak_graphite_stats]"
  notifies :start, "service[riak_graphite_stats]"
end

