
rvm_environment "ruby-1.9.3-p194@s3cleanup" do
  user "ubuntu"
end

%w[ dogapi aws-sdk ].each do |gem|
  rvm_gem gem do
    ruby_string "ruby-1.9.3-p194@s3cleanup"
    user        "ubuntu"
  end
end

directory "/home/ubuntu/scripts/s3" do
  owner     "ubuntu"
  group     "ubuntu"
  mode      0755
  action    :create
end

bucket_creds = Chef::EncryptedDataBagItem.load("s3", "wooga-g9-backup")

template "/home/ubuntu/scripts/s3/cleanup.rb" do
  source "cleanup.rb.erb"
  mode   0755
  owner  "ubuntu"
  group  "ubuntu"
  variables({
    :bucket_name => "wooga-g9-backup",
    :access_key  => bucket_creds["access_key"],
    :secret_key  => bucket_creds["secret_key"]
   })
end

cron "s3cleanup" do
  hour    "1"
  minute  "0"
  command "/home/ubuntu/.rvm/bin/ruby-1.9.3-p194@s3cleanup /home/ubuntu/scripts/s3/cleanup.rb >>/var/log/wooga/s3cleanup.log 2>&1"
  user    "ubuntu"
end

