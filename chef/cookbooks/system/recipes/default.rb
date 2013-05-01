# common packages
%w{
  htop
  sysstat
  ifstat
  dstat
  traceroute
  nmap
  zip
  iftop
}.each {|p| package p}

# ntp ---------------------------
file "/etc/timezone" do
  content node[:timezone]
end

execute "configure timezone" do
  command "dpkg-reconfigure -f noninteractive tzdata"
  not_if { `date` =~ /UTC/ }
end

package "ntpdate"

cron "ntpdate" do
  minute "10"
  command "/usr/sbin/ntpdate pool.ntp.org >/dev/null 2>&1"
end

# motd ---------------------------
bash "disable motd" do
  code "sed -i 's/^\\(session.*optional.*pam_motd\\.so.*\\)$/#\\1/g' /etc/pam.d/*"
  only_if "grep -e '^session.*optional.*pam_motd\.so' /etc/pam.d/*"
end

# sshd ---------------------------
service "ssh"

cookbook_file "/etc/ssh/sshd_config" do
  source "sshd_config"
  mode 0644
  notifies :restart, resources(:service => 'ssh')
end

# iptables ---------------------------
if node.attribute?(:iptables)
  bash "load iptables" do
    code "/sbin/iptables-restore /etc/iptables.conf"
    action :nothing
  end

  cookbook_file "/etc/iptables.conf" do
    source "iptables_#{node[:iptables][:rule]}"
    mode 0644
    notifies :run, resources(:bash  => "load iptables")
  end

  cookbook_file "/etc/network/if-pre-up.d/iptables_load" do
    source "iptables_load"
    mode 0755
  end
end

template "/etc/hosts" do
  source "hosts.erb"
  owner "root"
  group "root"
  mode 0644
end

# sysctl ---------------------------
service "procps"

def compile_attr(prefix, v)
  case v
  when Array
    return "#{prefix}=#{v.join(" ")}"
  when String, Fixnum, Float, Symbol
    "#{prefix}=#{v}"
  when Hash, Chef::Node::Attribute
    prefix += "." unless prefix.empty?
    return v.map {|key, value| compile_attr("#{prefix}#{key}", value)}.flatten
  else
    raise Chef::Exceptions::UnsupportedAction, "Sysctl cookbook can't handle values of type: #{v.class}"
  end
end

if node.attribute?(:sysctl)
  attr_txt = compile_attr("", node[:sysctl]).sort.join("\n") + "\n"

  file "/etc/sysctl.d/99-chef-attributes.conf" do
    content attr_txt
    mode "0644"
    notifies :start, resources(:service => 'procps')
  end
end

# disable cpu throttling ---------------------------
bash "run rc.local" do
  code "/bin/bash /etc/rc.local"
  action :nothing
end

bash "disable ondemand deamon" do
  code "update-rc.d -f ondemand remove"
  only_if {File.exists?("/etc/rc2.d/S99ondemand")}
end

cookbook_file "/etc/rc.local" do
  source "rc.local"
  mode 0755
  notifies :run, resources(:bash  => "run rc.local")
end

# standard log and log rotate
directory "/var/log/wooga" do
  mode 0777
end

template "logrotate.conf" do
  path   "/etc/logrotate.d/wooga"
  source "logrotate.conf.erb"
  mode   0644
  variables(
    :log_dir => '/var/log/wooga'
  )
end

# common script helper
directory "/home/ubuntu/scripts" do
  mode 0755
  user "ubuntu"
end

template "common.rb" do
  path   "/home/ubuntu/scripts/common.rb"
  source "common.rb.erb"
  mode   0644
  user   "ubuntu"
end  

