cookbook_file "/home/ubuntu/.ssh/config" do
  source "ssh_config" 
  mode 00644
  owner "ubuntu"
  group "ubuntu"
end

%w[ beanstalk edgecast ].each do |rsa|
  key = Chef::EncryptedDataBagItem.load("ssh", rsa)["key"]
  file "/home/ubuntu/.ssh/#{rsa}_rsa" do
    mode 00600
    owner "ubuntu"
    group "ubuntu"
    content key
  end
end

