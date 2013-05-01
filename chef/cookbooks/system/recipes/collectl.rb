cookbook_file "/tmp/setup_collectl.sh" do
  source "setup_collectl.sh"
  owner 'root'
  mode 0755
end

bash "install collects" do
  code "cd /tmp && ./setup_collectl.sh"
  not_if { File.exist?('/etc/collectl.conf') }
end

template "/etc/collectl.conf" do
  source "collectl.erb"
  owner 'root'
  group "root"
  mode 0644
end

service "collectl" do
  supports :status => true, :start => true, :stop => true, :restart => true
  action [ :enable, :restart ]
end
